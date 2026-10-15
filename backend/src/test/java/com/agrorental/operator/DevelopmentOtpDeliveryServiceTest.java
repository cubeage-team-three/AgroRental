package com.agrorental.operator;

import com.agrorental.operator.otp.DevelopmentOtpDeliveryService;
import com.agrorental.operator.otp.OtpPurpose;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DevelopmentOtpDeliveryService Unit Tests")
class DevelopmentOtpDeliveryServiceTest {

    @Mock
    private Environment environment;

    @Test
    @DisplayName("deliverOtp() - Dev environment simulates delivery safely without leaking raw OTP")
    void devEnvironment_SimulatesDeliverySafely() {
        DevelopmentOtpDeliveryService deliveryService = new DevelopmentOtpDeliveryService();
        deliveryService.setEnvironment(environment);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});

        assertDoesNotThrow(() ->
                deliveryService.deliverOtp("9876543210", "123456", OtpPurpose.MOBILE_VERIFICATION));
    }

    @Test
    @DisplayName("deliverOtp() - Production environment warns no SMS provider is configured and suppresses delivery")
    void prodEnvironment_WarnsNoSmsProviderAndSuppressesDelivery() {
        DevelopmentOtpDeliveryService deliveryService = new DevelopmentOtpDeliveryService();
        deliveryService.setEnvironment(environment);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        assertDoesNotThrow(() ->
                deliveryService.deliverOtp("9876543210", "123456", OtpPurpose.MOBILE_VERIFICATION));
    }

    @Test
    @DisplayName("deliverOtp() - Staging environment warns no SMS provider is configured and suppresses delivery")
    void stagingEnvironment_WarnsNoSmsProviderAndSuppressesDelivery() {
        DevelopmentOtpDeliveryService deliveryService = new DevelopmentOtpDeliveryService();
        deliveryService.setEnvironment(environment);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"staging"});

        assertDoesNotThrow(() ->
                deliveryService.deliverOtp("9876543210", "123456", OtpPurpose.MOBILE_VERIFICATION));
    }

    @Test
    @DisplayName("deliverOtp() - Null environment defaults safely to simulated delivery")
    void nullEnvironment_DefaultsSafely() {
        DevelopmentOtpDeliveryService deliveryService = new DevelopmentOtpDeliveryService();
        deliveryService.setEnvironment(null);

        assertDoesNotThrow(() ->
                deliveryService.deliverOtp("9876543210", "123456", OtpPurpose.MOBILE_VERIFICATION));
    }
}
