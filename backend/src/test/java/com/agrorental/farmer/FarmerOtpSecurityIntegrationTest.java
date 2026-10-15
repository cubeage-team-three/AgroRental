package com.agrorental.farmer;

import com.agrorental.auth.dto.LoginRequest;
import com.agrorental.auth.service.AuthService;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.farmer.controller.FarmerOtpController;
import com.agrorental.farmer.dto.OtpResponse;
import com.agrorental.farmer.dto.SendOtpRequest;
import com.agrorental.farmer.dto.VerifyOtpRequest;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.entity.FarmerOtp;
import com.agrorental.farmer.repository.FarmerOtpRepository;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.farmer.service.FarmerOtpService;
import com.agrorental.operator.otp.OtpDeliveryService;
import com.agrorental.operator.otp.OtpPurpose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P0-2 regression tests for the farmer OTP flow against a real (H2) transactional context, so
 * transaction rollback behaviour, persistence of attempts and atomic updates are exercised for real.
 */
@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:farmer_otp_security_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "agrorental.seed.mock-data.enabled=false",
    "app.jwt.secret=test-secret-key-32-chars-minimum-length-for-hmac-sha256",
    "app.otp.include-dev-mock=false",
    "app.otp.max-attempts=3",
    "app.otp.resend-cooldown-seconds=30",
    "app.otp.max-sends-per-hour=5",
    "server.port=0"
})
@DisplayName("P0-2: Farmer OTP Security Integration Tests")
class FarmerOtpSecurityIntegrationTest {

    /** Records the raw OTP handed to the delivery channel, standing in for the farmer's phone. */
    static class CapturingOtpDeliveryService implements OtpDeliveryService {
        private volatile String lastOtp;

        @Override
        public void deliverOtp(String mobileNumber, String rawOtp, OtpPurpose purpose) {
            this.lastOtp = rawOtp;
        }

        String lastOtp() {
            return lastOtp;
        }
    }

    @TestConfiguration
    static class DeliveryConfig {
        @Bean
        @Primary
        CapturingOtpDeliveryService capturingOtpDeliveryService() {
            return new CapturingOtpDeliveryService();
        }
    }

    private static final String MOBILE = "9811100001";

    @Autowired private FarmerOtpService farmerOtpService;
    @Autowired private FarmerOtpController farmerOtpController;
    @Autowired private AuthService authService;
    @Autowired private FarmerOtpRepository farmerOtpRepository;
    @Autowired private FarmerRepository farmerRepository;
    @Autowired private CapturingOtpDeliveryService delivery;
    @Autowired private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        farmerOtpRepository.deleteAll();
        farmerRepository.deleteAll();
    }

    private Farmer createFarmer(String mobile, String status) {
        Farmer farmer = Farmer.builder()
                .fullName("OTP Test Farmer")
                .mobileNumber(mobile)
                .email(mobile + "@otp-test.in")
                .accountStatus(status)
                .build();
        farmer.setActive(true);
        return farmerRepository.save(farmer);
    }

    private String sendOtp(String mobile) {
        farmerOtpService.sendOtp(SendOtpRequest.builder().mobileNumber(mobile).build());
        return delivery.lastOtp();
    }

    private OtpResponse verify(String mobile, String otp) {
        return farmerOtpService.verifyOtp(VerifyOtpRequest.builder().mobileNumber(mobile).otp(otp).build());
    }

    private static String wrongOtp(String correct) {
        return correct.equals("111111") ? "222222" : "111111";
    }

    private FarmerOtp latestOtp(String mobile) {
        return farmerOtpRepository.findTopByMobileNumberOrderByCreatedAtDesc(mobile).orElseThrow();
    }

    @Test
    @DisplayName("send-otp HTTP response never contains the OTP when dev mock disclosure is disabled")
    void sendOtpResponseDoesNotDiscloseOtp() throws Exception {
        createFarmer(MOBILE, "ACTIVE");
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(farmerOtpController).build();

        mockMvc.perform(post("/api/farmers/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mobileNumber\":\"" + MOBILE + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mobileNumber").value(MOBILE))
                .andExpect(jsonPath("$.data.devMockOtp").doesNotExist());

        assertThat(delivery.lastOtp()).matches("\\d{6}");
    }

    @Test
    @DisplayName("OTP is stored only as a keyed hash, never as the raw code")
    void otpIsStoredHashedNotPlaintext() {
        createFarmer(MOBILE, "ACTIVE");
        String otp = sendOtp(MOBILE);

        String stored = latestOtp(MOBILE).getOtpHash();
        assertThat(stored).isNotEqualTo(otp);
        assertThat(stored).doesNotContain(otp);
        assertThat(stored).hasSize(10);
        String fromDb = jdbcTemplate.queryForObject(
                "SELECT otp FROM farmer_otp WHERE mobile_number = ?", String.class, MOBILE);
        assertThat(fromDb).isEqualTo(stored).doesNotContain(otp);
    }

    @Test
    @DisplayName("Raw OTP is never written to application logs during send and verify")
    void otpIsNeverLogged(CapturedOutput output) {
        createFarmer(MOBILE, "PENDING_OTP");
        String otp = sendOtp(MOBILE);
        assertThatThrownBy(() -> verify(MOBILE, wrongOtp(otp))).isInstanceOf(BadRequestException.class);
        verify(MOBILE, otp);

        // Match the OTP as a standalone number so digits inside timestamps cannot cause false failures
        Pattern standaloneOtp = Pattern.compile("(?<!\\d)" + otp + "(?!\\d)");
        assertThat(standaloneOtp.matcher(output.getAll()).find())
                .as("raw OTP must not appear in logs")
                .isFalse();
    }

    @Test
    @DisplayName("Successful verification activates a pending farmer and consumes the OTP but issues NO session token")
    void successfulVerificationActivatesPendingFarmer() {
        Farmer farmer = createFarmer(MOBILE, "PENDING_OTP");
        String otp = sendOtp(MOBILE);

        OtpResponse response = verify(MOBILE, otp);

        assertThat(response.getVerified()).isTrue();
        // OTP is no longer an authentication method: no token, no session identity
        assertThat(response.getToken()).isNull();
        assertThat(response.getFarmerId()).isNull();
        assertThat(response.getRole()).isNull();
        assertThat(response.getDevMockOtp()).isNull();
        assertThat(farmerRepository.findById(farmer.getId()).orElseThrow().getAccountStatus()).isEqualTo("ACTIVE");
        FarmerOtp record = latestOtp(MOBILE);
        assertThat(record.getVerified()).isTrue();
        assertThat(record.getAttemptCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Failed attempts persist despite the exception and lock the OTP after the limit, even for the correct code")
    void failedAttemptsPersistAndLockOut() {
        createFarmer(MOBILE, "PENDING_OTP");
        String otp = sendOtp(MOBILE);
        String wrong = wrongOtp(otp);

        assertThatThrownBy(() -> verify(MOBILE, wrong)).hasMessageContaining("2 attempt(s) remaining");
        assertThat(latestOtp(MOBILE).getAttemptCount()).isEqualTo(1);
        assertThatThrownBy(() -> verify(MOBILE, wrong)).hasMessageContaining("1 attempt(s) remaining");
        assertThat(latestOtp(MOBILE).getAttemptCount()).isEqualTo(2);
        assertThatThrownBy(() -> verify(MOBILE, wrong)).hasMessageContaining("Maximum attempts exceeded");
        assertThat(latestOtp(MOBILE).getAttemptCount()).isEqualTo(3);

        assertThatThrownBy(() -> verify(MOBILE, otp))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Maximum OTP attempts exceeded");
        FarmerOtp record = latestOtp(MOBILE);
        assertThat(record.getAttemptCount()).isEqualTo(3);
        assertThat(record.getVerified()).isFalse();
        assertThat(farmerRepository.findByMobileNumber(MOBILE).orElseThrow().getAccountStatus()).isEqualTo("PENDING_OTP");
    }

    @Test
    @DisplayName("OTP login is disabled: loginType=OTP is refused before any OTP is checked, so attempts are not consumed")
    void otpLoginIsRejectedWithoutTouchingTheOtp() {
        createFarmer(MOBILE, "ACTIVE");
        String otp = sendOtp(MOBILE);
        for (String code : new String[]{wrongOtp(otp), otp}) {
            LoginRequest login = LoginRequest.builder()
                    .mobileOrEmail(MOBILE).otp(code).role("FARMER").loginType("OTP").build();
            assertThatThrownBy(() -> authService.login(login))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage(AuthService.OTP_LOGIN_DISABLED_MESSAGE);
        }
        FarmerOtp record = latestOtp(MOBILE);
        assertThat(record.getAttemptCount()).isZero();
        assertThat(record.getVerified()).isFalse();
    }

    @Test
    @DisplayName("Even the correct OTP never yields a login token, through /api/auth/login (loginType OTP, any role) or verify-otp")
    void correctOtpNeverYieldsASession() {
        createFarmer(MOBILE, "ACTIVE");
        String otp = sendOtp(MOBILE);
        for (String role : new String[]{"FARMER", "PARTNER", "OPERATOR", "ADMIN", null}) {
            LoginRequest login = LoginRequest.builder()
                    .mobileOrEmail(MOBILE).otp(otp).role(role).loginType("otp").build();
            assertThatThrownBy(() -> authService.login(login))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage(AuthService.OTP_LOGIN_DISABLED_MESSAGE);
        }

        OtpResponse verified = verify(MOBILE, otp);
        assertThat(verified.getVerified()).isTrue();
        assertThat(verified.getToken()).isNull();
        assertThatThrownBy(() -> verify(MOBILE, otp)).hasMessageContaining("already been used");
    }

    @Test
    @DisplayName("Expired OTP is rejected even when the code is correct")
    void expiredOtpIsRejected() {
        createFarmer(MOBILE, "PENDING_OTP");
        String otp = sendOtp(MOBILE);
        FarmerOtp record = latestOtp(MOBILE);
        record.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        farmerOtpRepository.save(record);

        assertThatThrownBy(() -> verify(MOBILE, otp)).hasMessageContaining("expired");
        assertThat(latestOtp(MOBILE).getVerified()).isFalse();
        assertThat(farmerRepository.findByMobileNumber(MOBILE).orElseThrow().getAccountStatus()).isEqualTo("PENDING_OTP");
    }

    @Test
    @DisplayName("A newer OTP supersedes the previous one")
    void olderOtpIsSupersededByNewOtp() {
        createFarmer(MOBILE, "PENDING_OTP");
        String firstOtp = sendOtp(MOBILE);
        jdbcTemplate.update("UPDATE farmer_otp SET created_at = ? WHERE mobile_number = ?",
                LocalDateTime.now().minusMinutes(1), MOBILE);
        String secondOtp = sendOtp(MOBILE);

        if (!firstOtp.equals(secondOtp)) {
            assertThatThrownBy(() -> verify(MOBILE, firstOtp)).hasMessageContaining("Invalid OTP");
        }
        assertThat(verify(MOBILE, secondOtp).getVerified()).isTrue();
    }

    @Test
    @DisplayName("Verifying an OTP does not reactivate an INACTIVE farmer or issue a session")
    void inactiveFarmerIsNotReactivated() {
        createFarmer(MOBILE, "INACTIVE");
        String otp = sendOtp(MOBILE);

        assertThatThrownBy(() -> verify(MOBILE, otp)).hasMessageContaining("deactivated");
        assertThat(farmerRepository.findByMobileNumber(MOBILE).orElseThrow().getAccountStatus()).isEqualTo("INACTIVE");
    }

    @Test
    @DisplayName("Verifying an OTP leaves an admin-set REJECTED status unchanged")
    void rejectedFarmerStatusIsNotOverwritten() {
        createFarmer(MOBILE, "REJECTED");
        String otp = sendOtp(MOBILE);

        verify(MOBILE, otp);

        assertThat(farmerRepository.findByMobileNumber(MOBILE).orElseThrow().getAccountStatus()).isEqualTo("REJECTED");
    }

    @Test
    @DisplayName("Resend within the cooldown window is rejected")
    void resendCooldownIsEnforced() {
        createFarmer(MOBILE, "PENDING_OTP");
        sendOtp(MOBILE);

        assertThatThrownBy(() -> farmerOtpService.resendOtp(SendOtpRequest.builder().mobileNumber(MOBILE).build()))
                .hasMessageContaining("Please wait");
        assertThat(farmerOtpRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("OTP sends are capped per mobile number per hour")
    void hourlySendLimitIsEnforced() {
        createFarmer(MOBILE, "PENDING_OTP");
        for (int i = 0; i < 5; i++) {
            sendOtp(MOBILE);
            // Move existing OTPs outside the resend cooldown but inside the hourly window
            jdbcTemplate.update("UPDATE farmer_otp SET created_at = ? WHERE mobile_number = ?",
                    LocalDateTime.now().minusMinutes(10), MOBILE);
        }

        assertThatThrownBy(() -> sendOtp(MOBILE)).hasMessageContaining("Too many OTP requests");
        assertThat(farmerOtpRepository.count()).isEqualTo(5);
    }

    @Test
    @DisplayName("Concurrent wrong guesses cannot exceed the attempt limit")
    void concurrentGuessesCannotExceedAttemptLimit() throws Exception {
        createFarmer(MOBILE, "PENDING_OTP");
        String otp = sendOtp(MOBILE);
        String wrong = wrongOtp(otp);

        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<String>> results = new ArrayList<>();
        try {
            for (int i = 0; i < threads; i++) {
                Callable<String> guess = () -> {
                    start.await();
                    try {
                        verify(MOBILE, wrong);
                        return "VERIFIED";
                    } catch (BadRequestException e) {
                        return e.getMessage();
                    }
                };
                results.add(executor.submit(guess));
            }
            start.countDown();

            long comparedGuesses = 0;
            for (Future<String> result : results) {
                String message = result.get(30, TimeUnit.SECONDS);
                assertThat(message).isNotEqualTo("VERIFIED");
                if (message.startsWith("Invalid OTP")) {
                    comparedGuesses++;
                }
            }
            assertThat(comparedGuesses).isLessThanOrEqualTo(3);
        } finally {
            executor.shutdownNow();
        }

        assertThat(latestOtp(MOBILE).getAttemptCount()).isEqualTo(3);
        assertThatThrownBy(() -> verify(MOBILE, otp)).hasMessageContaining("Maximum OTP attempts exceeded");
    }
}
