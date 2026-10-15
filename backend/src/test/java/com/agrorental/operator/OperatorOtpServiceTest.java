package com.agrorental.operator;

import com.agrorental.common.exception.BadRequestException;
import com.agrorental.operator.dto.OperatorOtpResponse;
import com.agrorental.operator.dto.OperatorOtpSendRequest;
import com.agrorental.operator.dto.OperatorOtpVerifyRequest;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.otp.*;
import com.agrorental.operator.repository.OperatorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OperatorOtpService Unit Tests")
class OperatorOtpServiceTest {

    @Mock
    private OperatorOtpRepository operatorOtpRepository;

    @Mock
    private OperatorRepository operatorRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OtpDeliveryService otpDeliveryService;

    @Mock
    private Environment environment;

    @InjectMocks
    private OperatorOtpService operatorOtpService;

    private Operator testOperator;

    @BeforeEach
    void setUp() {
        testOperator = Operator.builder()
                .fullName("Suresh Shinde")
                .mobileNumber("9876543210")
                .mobileVerified(false)
                .build();
        testOperator.setId(1L);
    }

    @Test
    @DisplayName("sendOtp() - Should generate hashed OTP, invalidate old OTPs, and dispatch")
    void shouldSendOtpSuccessfully() {
        OperatorOtpSendRequest request = OperatorOtpSendRequest.builder()
                .mobileNumber("9876543210")
                .purpose(OtpPurpose.MOBILE_VERIFICATION)
                .build();

        when(operatorOtpRepository.findTopByMobileNumberAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(
                "9876543210", OtpPurpose.MOBILE_VERIFICATION))
                .thenReturn(Optional.empty());

        when(operatorOtpRepository.findByMobileNumberAndPurposeAndVerifiedFalse(
                "9876543210", OtpPurpose.MOBILE_VERIFICATION))
                .thenReturn(Collections.emptyList());

        when(passwordEncoder.encode(anyString())).thenReturn("hashed_otp_bcrypt");
        when(operatorRepository.findByMobileNumber("9876543210")).thenReturn(Optional.of(testOperator));

        OperatorOtpResponse response = operatorOtpService.sendOtp(request);

        assertNotNull(response);
        assertEquals("9876543210", response.getMobileNumber());
        assertFalse(response.isVerified());
        assertNotNull(response.getExpiresAt());
        assertNull(response.getDevMockOtp(), "Default configuration must not leak mock OTP");

        verify(operatorOtpRepository).save(any(OperatorOtp.class));
        verify(otpDeliveryService).deliverOtp(eq("9876543210"), anyString(), eq(OtpPurpose.MOBILE_VERIFICATION));
    }

    @Test
    @DisplayName("sendOtp() - Should throw BadRequestException when requested within resend cooldown")
    void shouldEnforceResendCooldown() {
        OperatorOtp activeOtp = OperatorOtp.builder()
                .mobileNumber("9876543210")
                .purpose(OtpPurpose.MOBILE_VERIFICATION)
                .createdAt(LocalDateTime.now().minusSeconds(10)) // requested 10s ago, cooldown 30s
                .build();

        when(operatorOtpRepository.findTopByMobileNumberAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(
                "9876543210", OtpPurpose.MOBILE_VERIFICATION))
                .thenReturn(Optional.of(activeOtp));

        OperatorOtpSendRequest request = OperatorOtpSendRequest.builder()
                .mobileNumber("9876543210")
                .purpose(OtpPurpose.MOBILE_VERIFICATION)
                .build();

        assertThrows(BadRequestException.class, () -> operatorOtpService.sendOtp(request));
        verify(operatorOtpRepository, never()).save(any(OperatorOtp.class));
    }

    @Test
    @DisplayName("verifyOtp() - Should verify matching OTP and update Operator mobileVerified=true")
    void shouldVerifyOtpAndUpdateOperator() {
        OperatorOtp otpRecord = OperatorOtp.builder()
                .mobileNumber("9876543210")
                .otpHash("hashed_otp")
                .purpose(OtpPurpose.MOBILE_VERIFICATION)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attemptCount(0)
                .maxAttempts(3)
                .verified(false)
                .build();

        when(operatorOtpRepository.findTopByMobileNumberAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(
                "9876543210", OtpPurpose.MOBILE_VERIFICATION))
                .thenReturn(Optional.of(otpRecord));

        when(passwordEncoder.matches("654321", "hashed_otp")).thenReturn(true);
        when(operatorRepository.findByMobileNumber("9876543210")).thenReturn(Optional.of(testOperator));

        OperatorOtpVerifyRequest verifyRequest = OperatorOtpVerifyRequest.builder()
                .mobileNumber("9876543210")
                .otp("654321")
                .purpose(OtpPurpose.MOBILE_VERIFICATION)
                .build();

        OperatorOtpResponse response = operatorOtpService.verifyOtp(verifyRequest);

        assertNotNull(response);
        assertTrue(response.isVerified());
        assertTrue(otpRecord.getVerified());
        assertTrue(testOperator.isMobileVerified());

        verify(operatorOtpRepository).save(otpRecord);
        verify(operatorRepository).save(testOperator);
    }

    @Test
    @DisplayName("verifyOtp() - Should throw BadRequestException when OTP does not match")
    void shouldFailOnIncorrectOtp() {
        OperatorOtp otpRecord = OperatorOtp.builder()
                .mobileNumber("9876543210")
                .otpHash("hashed_otp")
                .purpose(OtpPurpose.MOBILE_VERIFICATION)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attemptCount(0)
                .maxAttempts(3)
                .verified(false)
                .build();

        when(operatorOtpRepository.findTopByMobileNumberAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(
                "9876543210", OtpPurpose.MOBILE_VERIFICATION))
                .thenReturn(Optional.of(otpRecord));

        when(passwordEncoder.matches("999999", "hashed_otp")).thenReturn(false);

        OperatorOtpVerifyRequest verifyRequest = OperatorOtpVerifyRequest.builder()
                .mobileNumber("9876543210")
                .otp("999999")
                .purpose(OtpPurpose.MOBILE_VERIFICATION)
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> operatorOtpService.verifyOtp(verifyRequest));

        assertTrue(ex.getMessage().contains("Invalid OTP"));
        assertEquals(1, otpRecord.getAttemptCount());
        assertFalse(otpRecord.getVerified());
        verify(operatorOtpRepository).save(otpRecord);
        verify(operatorRepository, never()).save(any(Operator.class));
    }

    @Test
    @DisplayName("verifyOtp() - Should throw BadRequestException when OTP is expired")
    void shouldFailOnExpiredOtp() {
        OperatorOtp otpRecord = OperatorOtp.builder()
                .mobileNumber("9876543210")
                .otpHash("hashed_otp")
                .purpose(OtpPurpose.MOBILE_VERIFICATION)
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .attemptCount(0)
                .maxAttempts(3)
                .verified(false)
                .build();

        when(operatorOtpRepository.findTopByMobileNumberAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(
                "9876543210", OtpPurpose.MOBILE_VERIFICATION))
                .thenReturn(Optional.of(otpRecord));

        OperatorOtpVerifyRequest verifyRequest = OperatorOtpVerifyRequest.builder()
                .mobileNumber("9876543210")
                .otp("654321")
                .purpose(OtpPurpose.MOBILE_VERIFICATION)
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> operatorOtpService.verifyOtp(verifyRequest));

        assertTrue(ex.getMessage().contains("expired"));
    }

    // --- Focused Priority 5.2.3 Tests: Environment-Safe OTP Responses ---

    @Test
    @DisplayName("sendOtp() - Dev environment with app.otp.include-dev-mock=true returns mock OTP for local dev workflow")
    void devProfile_WithMockExplicitlyEnabled_ReturnsMockOtp() {
        operatorOtpService.setEnvironment(environment);
        operatorOtpService.setIncludeDevMock(true);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});

        OperatorOtpSendRequest request = OperatorOtpSendRequest.builder()
                .mobileNumber("9876543210")
                .purpose(OtpPurpose.MOBILE_VERIFICATION)
                .build();

        when(operatorOtpRepository.findTopByMobileNumberAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(
                "9876543210", OtpPurpose.MOBILE_VERIFICATION))
                .thenReturn(Optional.empty());
        when(operatorOtpRepository.findByMobileNumberAndPurposeAndVerifiedFalse(
                "9876543210", OtpPurpose.MOBILE_VERIFICATION))
                .thenReturn(Collections.emptyList());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_otp");
        when(operatorRepository.findByMobileNumber("9876543210")).thenReturn(Optional.of(testOperator));

        OperatorOtpResponse response = operatorOtpService.sendOtp(request);

        assertNotNull(response);
        assertNotNull(response.getDevMockOtp(), "Dev mode with opt-in mock enabled should provide devMockOtp");
        assertEquals(6, response.getDevMockOtp().length(), "Generated OTP must be 6 digits");
        assertTrue(response.getDevMockOtp().matches("\\d{6}"), "Generated OTP must be numeric");
    }

    @Test
    @DisplayName("sendOtp() - Dev environment with app.otp.include-dev-mock=false does NOT return mock OTP")
    void devProfile_WithMockDisabled_DoesNotReturnMockOtp() {
        operatorOtpService.setEnvironment(environment);
        operatorOtpService.setIncludeDevMock(false);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});

        OperatorOtpSendRequest request = OperatorOtpSendRequest.builder()
                .mobileNumber("9876543210")
                .purpose(OtpPurpose.MOBILE_VERIFICATION)
                .build();

        when(operatorOtpRepository.findTopByMobileNumberAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(
                "9876543210", OtpPurpose.MOBILE_VERIFICATION))
                .thenReturn(Optional.empty());
        when(operatorOtpRepository.findByMobileNumberAndPurposeAndVerifiedFalse(
                "9876543210", OtpPurpose.MOBILE_VERIFICATION))
                .thenReturn(Collections.emptyList());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_otp");
        when(operatorRepository.findByMobileNumber("9876543210")).thenReturn(Optional.of(testOperator));

        OperatorOtpResponse response = operatorOtpService.sendOtp(request);

        assertNotNull(response);
        assertNull(response.getDevMockOtp(), "Dev mode with mock disabled must not provide devMockOtp");
    }

    @Test
    @DisplayName("sendOtp() - Production environment NEVER returns mock OTP, even if includeDevMock=true")
    void prodProfile_WithMockExplicitlyEnabled_StrictlySuppressesMockOtp() {
        operatorOtpService.setEnvironment(environment);
        operatorOtpService.setIncludeDevMock(true); // Attempted misconfiguration
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        OperatorOtpSendRequest request = OperatorOtpSendRequest.builder()
                .mobileNumber("9876543210")
                .purpose(OtpPurpose.MOBILE_VERIFICATION)
                .build();

        when(operatorOtpRepository.findTopByMobileNumberAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(
                "9876543210", OtpPurpose.MOBILE_VERIFICATION))
                .thenReturn(Optional.empty());
        when(operatorOtpRepository.findByMobileNumberAndPurposeAndVerifiedFalse(
                "9876543210", OtpPurpose.MOBILE_VERIFICATION))
                .thenReturn(Collections.emptyList());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_otp");
        when(operatorRepository.findByMobileNumber("9876543210")).thenReturn(Optional.of(testOperator));

        OperatorOtpResponse response = operatorOtpService.sendOtp(request);

        assertNotNull(response);
        assertNull(response.getDevMockOtp(), "Production environment must strictly suppress devMockOtp");
    }

    @Test
    @DisplayName("sendOtp() - Staging environment NEVER returns mock OTP, even if includeDevMock=true")
    void stagingProfile_WithMockExplicitlyEnabled_StrictlySuppressesMockOtp() {
        operatorOtpService.setEnvironment(environment);
        operatorOtpService.setIncludeDevMock(true); // Attempted misconfiguration
        when(environment.getActiveProfiles()).thenReturn(new String[]{"staging"});

        OperatorOtpSendRequest request = OperatorOtpSendRequest.builder()
                .mobileNumber("9876543210")
                .purpose(OtpPurpose.MOBILE_VERIFICATION)
                .build();

        when(operatorOtpRepository.findTopByMobileNumberAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(
                "9876543210", OtpPurpose.MOBILE_VERIFICATION))
                .thenReturn(Optional.empty());
        when(operatorOtpRepository.findByMobileNumberAndPurposeAndVerifiedFalse(
                "9876543210", OtpPurpose.MOBILE_VERIFICATION))
                .thenReturn(Collections.emptyList());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_otp");
        when(operatorRepository.findByMobileNumber("9876543210")).thenReturn(Optional.of(testOperator));

        OperatorOtpResponse response = operatorOtpService.sendOtp(request);

        assertNotNull(response);
        assertNull(response.getDevMockOtp(), "Staging environment must strictly suppress devMockOtp");
    }

    @Test
    @DisplayName("sendOtp() - Missing environment/configuration defaults safely to suppressing mock OTP")
    void missingConfiguration_DefaultsSafelyToNullMockOtp() {
        operatorOtpService.setEnvironment(null);
        operatorOtpService.setIncludeDevMock(false);

        OperatorOtpSendRequest request = OperatorOtpSendRequest.builder()
                .mobileNumber("9876543210")
                .purpose(OtpPurpose.MOBILE_VERIFICATION)
                .build();

        when(operatorOtpRepository.findTopByMobileNumberAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(
                "9876543210", OtpPurpose.MOBILE_VERIFICATION))
                .thenReturn(Optional.empty());
        when(operatorOtpRepository.findByMobileNumberAndPurposeAndVerifiedFalse(
                "9876543210", OtpPurpose.MOBILE_VERIFICATION))
                .thenReturn(Collections.emptyList());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_otp");
        when(operatorRepository.findByMobileNumber("9876543210")).thenReturn(Optional.of(testOperator));

        OperatorOtpResponse response = operatorOtpService.sendOtp(request);

        assertNotNull(response);
        assertNull(response.getDevMockOtp(), "Missing property must default safely to null mock OTP");
    }

    @Test
    @DisplayName("verifyOtp() - Successful verification response does NOT contain devMockOtp")
    void verifyOtp_Success_NeverExposesMockOtp() {
        OperatorOtp otpRecord = OperatorOtp.builder()
                .mobileNumber("9876543210")
                .otpHash("hashed_otp")
                .purpose(OtpPurpose.MOBILE_VERIFICATION)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attemptCount(0)
                .maxAttempts(3)
                .verified(false)
                .build();

        when(operatorOtpRepository.findTopByMobileNumberAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(
                "9876543210", OtpPurpose.MOBILE_VERIFICATION))
                .thenReturn(Optional.of(otpRecord));

        when(passwordEncoder.matches("654321", "hashed_otp")).thenReturn(true);
        when(operatorRepository.findByMobileNumber("9876543210")).thenReturn(Optional.of(testOperator));

        OperatorOtpVerifyRequest verifyRequest = OperatorOtpVerifyRequest.builder()
                .mobileNumber("9876543210")
                .otp("654321")
                .purpose(OtpPurpose.MOBILE_VERIFICATION)
                .build();

        OperatorOtpResponse response = operatorOtpService.verifyOtp(verifyRequest);

        assertNotNull(response);
        assertTrue(response.isVerified());
        assertNull(response.getDevMockOtp(), "Verification response must never have devMockOtp");
    }

    @Test
    @DisplayName("verifyOtp() - Failed attempt does not leak the real OTP value in exception message")
    void verifyOtp_Failure_DoesNotLeakOtpValue() {
        OperatorOtp otpRecord = OperatorOtp.builder()
                .mobileNumber("9876543210")
                .otpHash("hashed_otp")
                .purpose(OtpPurpose.MOBILE_VERIFICATION)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attemptCount(0)
                .maxAttempts(3)
                .verified(false)
                .build();

        when(operatorOtpRepository.findTopByMobileNumberAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(
                "9876543210", OtpPurpose.MOBILE_VERIFICATION))
                .thenReturn(Optional.of(otpRecord));

        when(passwordEncoder.matches("111111", "hashed_otp")).thenReturn(false);

        OperatorOtpVerifyRequest verifyRequest = OperatorOtpVerifyRequest.builder()
                .mobileNumber("9876543210")
                .otp("111111")
                .purpose(OtpPurpose.MOBILE_VERIFICATION)
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> operatorOtpService.verifyOtp(verifyRequest));

        assertFalse(ex.getMessage().contains("654321"));
        assertFalse(ex.getMessage().contains("hashed_otp"));
        assertTrue(ex.getMessage().contains("Invalid OTP"));
    }
}
