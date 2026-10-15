package com.agrorental.auth;

import com.agrorental.auth.dto.ForgotPasswordRequest;
import com.agrorental.auth.dto.ForgotPasswordResponse;
import com.agrorental.auth.repository.AccountPasswordResetTokenRepository;
import com.agrorental.auth.service.PasswordResetDelivery;
import com.agrorental.auth.service.PasswordResetRateLimiter;
import com.agrorental.auth.service.PasswordResetService;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.partner.repository.PartnerRepository;
import com.agrorental.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The development token disclosure gate fails closed: it needs an explicitly active 'dev' profile AND the
 * opt-in property, and staging, production, mixed, implicit-default or missing profiles never disclose.
 */
@DisplayName("Auth contract 2.4: dev reset-token disclosure gate (fail closed)")
class PasswordResetDevTokenGateTest {

    private static MockEnvironment env(String[] active, String[] defaults) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(active);
        if (defaults != null) {
            environment.setDefaultProfiles(defaults);
        }
        return environment;
    }

    @SuppressWarnings("unchecked")
    private static PasswordResetService service(MockEnvironment environment, boolean flag, FarmerRepository farmerRepository) {
        ObjectProvider<PasswordResetDelivery> noDelivery = mock(ObjectProvider.class);
        when(noDelivery.getIfUnique()).thenReturn(null);
        AccountPasswordResetTokenRepository tokenRepository = mock(AccountPasswordResetTokenRepository.class);
        when(tokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        PasswordResetService service = new PasswordResetService(tokenRepository, mock(UserRepository.class),
                farmerRepository, mock(PartnerRepository.class), mock(OperatorRepository.class),
                mock(PasswordEncoder.class), new PasswordResetRateLimiter(5, 20, 15), noDelivery,
                mock(PlatformTransactionManager.class), environment);
        service.setDevTokenInResponse(flag);
        return service;
    }

    private static FarmerRepository farmerRepositoryWithAccount() {
        FarmerRepository farmerRepository = mock(FarmerRepository.class);
        Farmer farmer = Farmer.builder().fullName("Gate Farmer").mobileNumber("9870000001").accountStatus("ACTIVE").build();
        farmer.setId(5L);
        when(farmerRepository.findByMobileNumber("9870000001")).thenReturn(Optional.of(farmer));
        return farmerRepository;
    }

    private static ForgotPasswordResponse request(PasswordResetService service) {
        return service.requestReset(ForgotPasswordRequest.builder().role("FARMER").identifier("9870000001").build(), "10.50.0.1");
    }

    @Test
    @DisplayName("Explicit dev profile + opt-in: disclosed (and only then)")
    void explicitDevWithOptIn() {
        PasswordResetService service = service(env(new String[]{"dev"}, new String[]{"dev"}), true, farmerRepositoryWithAccount());
        assertThat(service.isDevTokenDisclosureAllowed()).isTrue();
        ForgotPasswordResponse response = request(service);
        assertThat(response.getDevResetToken()).isNotBlank();
        assertThat(response.getDevNotice()).isEqualTo(PasswordResetService.DEV_TOKEN_NOTICE);
    }

    @Test
    @DisplayName("Explicit dev profile without opt-in (the application-dev.properties default): never disclosed")
    void explicitDevWithoutOptIn() {
        PasswordResetService service = service(env(new String[]{"dev"}, null), false, farmerRepositoryWithAccount());
        assertThat(service.isDevTokenDisclosureAllowed()).isFalse();
        ForgotPasswordResponse response = request(service);
        assertThat(response.getDevResetToken()).isNull();
        assertThat(response.getMessage()).isEqualTo(PasswordResetService.DELIVERY_UNAVAILABLE_MESSAGE);
    }

    @Test
    @DisplayName("Implicit default profile (spring.profiles.default=dev, nothing active): never disclosed, even with opt-in")
    void implicitDefaultNeverDiscloses() {
        PasswordResetService service = service(env(new String[]{}, new String[]{"dev"}), true, farmerRepositoryWithAccount());
        assertThat(service.isDevTokenDisclosureAllowed()).isFalse();
        assertThat(request(service).getDevResetToken()).isNull();
    }

    @ParameterizedTest(name = "active profiles [{0}] never disclose, even with opt-in")
    @ValueSource(strings = {"staging", "prod", "dev,staging", "dev,prod", "dev,PROD", "DEV", "development", "test", "staging,dev"})
    void stagingProductionAndMalformedNeverDisclose(String profiles) {
        PasswordResetService service = service(env(profiles.split(","), null), true, farmerRepositoryWithAccount());
        assertThat(service.isDevTokenDisclosureAllowed()).isFalse();
        ForgotPasswordResponse response = request(service);
        assertThat(response.getDevResetToken()).isNull();
        assertThat(response.getDevNotice()).isNull();
    }

    @Test
    @DisplayName("prod or staging configured as a default profile never discloses")
    void prodOrStagingDefaultNeverDiscloses() {
        assertThat(service(env(new String[]{"dev"}, new String[]{"prod"}), true, farmerRepositoryWithAccount())
                .isDevTokenDisclosureAllowed()).isFalse();
        assertThat(service(env(new String[]{"dev"}, new String[]{"staging"}), true, farmerRepositoryWithAccount())
                .isDevTokenDisclosureAllowed()).isFalse();
    }

    @Test
    @DisplayName("Missing environment never discloses")
    void missingEnvironmentNeverDiscloses() {
        PasswordResetService service = service(null, true, farmerRepositoryWithAccount());
        assertThat(service.isDevTokenDisclosureAllowed()).isFalse();
        assertThat(request(service).getDevResetToken()).isNull();
    }
}
