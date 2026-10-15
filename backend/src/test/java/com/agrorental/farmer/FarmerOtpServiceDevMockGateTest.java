package com.agrorental.farmer;

import com.agrorental.farmer.dto.OtpResponse;
import com.agrorental.farmer.dto.SendOtpRequest;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.entity.FarmerOtp;
import com.agrorental.farmer.repository.FarmerOtpRepository;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.farmer.service.FarmerOtpService;
import com.agrorental.operator.otp.OtpDeliveryService;
import com.agrorental.operator.otp.OtpPurpose;
import com.agrorental.security.jwt.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("P0-2: Farmer OTP dev-mock disclosure gate")
class FarmerOtpServiceDevMockGateTest {

    private static final String MOBILE = "9811100002";

    @Mock private FarmerOtpRepository farmerOtpRepository;
    @Mock private FarmerRepository farmerRepository;
    @Mock private JwtProperties jwtProperties;
    @Mock private OtpDeliveryService otpDeliveryService;
    @Mock private Environment environment;

    private FarmerOtpService farmerOtpService;

    @BeforeEach
    void setUp() {
        farmerOtpService = new FarmerOtpService(
                farmerOtpRepository, farmerRepository, jwtProperties, otpDeliveryService);
    }

    private void stubSendOtpDependencies() {
        Farmer farmer = Farmer.builder().fullName("Gate Farmer").mobileNumber(MOBILE).build();
        farmer.setId(42L);
        when(farmerRepository.findByMobileNumber(MOBILE)).thenReturn(Optional.of(farmer));
        when(farmerOtpRepository.findTopByMobileNumberOrderByCreatedAtDesc(MOBILE)).thenReturn(Optional.empty());
        when(farmerOtpRepository.countByMobileNumberAndCreatedAtAfter(eq(MOBILE), any())).thenReturn(0L);
        when(farmerOtpRepository.save(any(FarmerOtp.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtProperties.getSecret()).thenReturn("unit-test-secret-key-32-chars-minimum-length");
    }

    private String deliveredOtp() {
        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(otpDeliveryService).deliverOtp(eq(MOBILE), otpCaptor.capture(), eq(OtpPurpose.MOBILE_VERIFICATION));
        return otpCaptor.getValue();
    }

    private OtpResponse send() {
        return farmerOtpService.sendOtp(SendOtpRequest.builder().mobileNumber(MOBILE).build());
    }

    @Test
    @DisplayName("Default configuration never discloses the OTP")
    void defaultConfigurationDoesNotDisclose() {
        stubSendOtpDependencies();

        OtpResponse response = send();

        assertNull(response.getDevMockOtp());
        assertTrue(deliveredOtp().matches("\\d{6}"));
    }

    @Test
    @DisplayName("Dev profile with explicit opt-in discloses the same OTP that was delivered and stores only its hash")
    void devOptInDisclosesDeliveredOtp() {
        stubSendOtpDependencies();
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
        when(environment.getDefaultProfiles()).thenReturn(new String[]{"default"});
        farmerOtpService.setEnvironment(environment);
        farmerOtpService.setIncludeDevMock(true);

        OtpResponse response = send();

        String delivered = deliveredOtp();
        assertEquals(delivered, response.getDevMockOtp());
        ArgumentCaptor<FarmerOtp> recordCaptor = ArgumentCaptor.forClass(FarmerOtp.class);
        verify(farmerOtpRepository).save(recordCaptor.capture());
        assertNotEquals(delivered, recordCaptor.getValue().getOtpHash());
        assertFalse(recordCaptor.getValue().getOtpHash().contains(delivered));
    }

    @Test
    @DisplayName("Production profile never discloses the OTP even if dev mock is enabled")
    void productionNeverDiscloses() {
        stubSendOtpDependencies();
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        farmerOtpService.setEnvironment(environment);
        farmerOtpService.setIncludeDevMock(true);

        assertFalse(farmerOtpService.isDevMockOtpAllowed());
        assertNull(send().getDevMockOtp());
    }

    @Test
    @DisplayName("Staging profile never discloses the OTP even if dev mock is enabled")
    void stagingNeverDiscloses() {
        stubSendOtpDependencies();
        when(environment.getActiveProfiles()).thenReturn(new String[]{"staging"});
        farmerOtpService.setEnvironment(environment);
        farmerOtpService.setIncludeDevMock(true);

        assertFalse(farmerOtpService.isDevMockOtpAllowed());
        assertNull(send().getDevMockOtp());
    }
}
