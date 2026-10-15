package com.agrorental.common.config;

import com.agrorental.operator.otp.OperatorOtpService;
import com.agrorental.security.jwt.JwtProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Priority 5.2.5: Explicit Staging Configuration and Fail-Fast Validation Tests")
class StagingConfigurationTest {

    @Mock
    private Environment environment;

    @Nested
    @DisplayName("1. StagingConfigurationValidator Fail-Fast Rules")
    class StagingValidatorTests {

        private StagingConfigurationValidator createValidStagingValidator() {
            StagingConfigurationValidator validator = new StagingConfigurationValidator();
            validator.setEnvironment(environment);
            validator.setDatasourceUrl("jdbc:mysql://staging-db.agrorent.internal:3306/agro_rental_staging?useSSL=true");
            validator.setDatasourceUsername("agro_staging_user");
            validator.setDatasourcePassword("secure_staging_password_123");
            validator.setDdlAuto("validate");
            validator.setFlywayEnabled(true);
            validator.setMockDataEnabled(false);
            validator.setIncludeDevMock(false);
            validator.setH2ConsoleEnabled(false);
            return validator;
        }

        @Test
        @DisplayName("Staging Profile: Valid staging configuration passes validation")
        void stagingProfile_ValidConfiguration_PassesSuccessfully() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"staging"});
            StagingConfigurationValidator validator = createValidStagingValidator();

            assertDoesNotThrow(validator::validateStagingConfiguration);
        }

        @Test
        @DisplayName("Staging Profile: Missing or empty datasource URL throws IllegalStateException")
        void stagingProfile_MissingDatasourceUrl_ThrowsIllegalStateException() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"staging"});
            StagingConfigurationValidator validator = createValidStagingValidator();
            validator.setDatasourceUrl("");

            IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateStagingConfiguration);
            assertTrue(ex.getMessage().contains("Missing required datasource URL in staging environment"));
        }

        @Test
        @DisplayName("Staging Profile: H2 in-memory or embedded database URL throws IllegalStateException")
        void stagingProfile_H2DatasourceUrl_ThrowsIllegalStateException() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"staging"});
            StagingConfigurationValidator validator = createValidStagingValidator();
            validator.setDatasourceUrl("jdbc:h2:mem:staging_db");

            IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateStagingConfiguration);
            assertTrue(ex.getMessage().contains("H2 and embedded databases are strictly prohibited in staging"));
        }

        @Test
        @DisplayName("Staging Profile: Non-MySQL datasource URL throws IllegalStateException")
        void stagingProfile_NonMysqlDatasourceUrl_ThrowsIllegalStateException() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"staging"});
            StagingConfigurationValidator validator = createValidStagingValidator();
            validator.setDatasourceUrl("jdbc:postgresql://localhost:5432/agro_rental");

            IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateStagingConfiguration);
            assertTrue(ex.getMessage().contains("Staging datasource must be a MySQL database"));
        }

        @Test
        @DisplayName("Staging Profile: Missing datasource username throws IllegalStateException")
        void stagingProfile_MissingDatasourceUsername_ThrowsIllegalStateException() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"staging"});
            StagingConfigurationValidator validator = createValidStagingValidator();
            validator.setDatasourceUsername(null);

            IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateStagingConfiguration);
            assertTrue(ex.getMessage().contains("Missing required datasource username in staging environment"));
        }

        @Test
        @DisplayName("Staging Profile: Missing datasource password throws IllegalStateException")
        void stagingProfile_MissingDatasourcePassword_ThrowsIllegalStateException() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"staging"});
            StagingConfigurationValidator validator = createValidStagingValidator();
            validator.setDatasourcePassword(null);

            IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateStagingConfiguration);
            assertTrue(ex.getMessage().contains("Missing required datasource password in staging environment"));
        }

        @Test
        @DisplayName("Staging Profile: DDL-auto not set to 'validate' (e.g. 'update') throws IllegalStateException")
        void stagingProfile_InvalidDdlAuto_ThrowsIllegalStateException() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"staging"});
            StagingConfigurationValidator validator = createValidStagingValidator();
            validator.setDdlAuto("update");

            IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateStagingConfiguration);
            assertTrue(ex.getMessage().contains("'spring.jpa.hibernate.ddl-auto' must be 'validate' in staging"));
        }

        @Test
        @DisplayName("Staging Profile: Flyway disabled throws IllegalStateException")
        void stagingProfile_FlywayDisabled_ThrowsIllegalStateException() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"staging"});
            StagingConfigurationValidator validator = createValidStagingValidator();
            validator.setFlywayEnabled(false);

            IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateStagingConfiguration);
            assertTrue(ex.getMessage().contains("Flyway database migrations must be enabled"));
        }

        @Test
        @DisplayName("Staging Profile: Mock-data seeding enabled throws IllegalStateException")
        void stagingProfile_MockDataEnabled_ThrowsIllegalStateException() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"staging"});
            StagingConfigurationValidator validator = createValidStagingValidator();
            validator.setMockDataEnabled(true);

            IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateStagingConfiguration);
            assertTrue(ex.getMessage().contains("Mock-data seeding ('agrorental.seed.mock-data.enabled') must be disabled in staging"));
        }

        @Test
        @DisplayName("Staging Profile: Dev mock OTP disclosure enabled throws IllegalStateException")
        void stagingProfile_DevMockOtpEnabled_ThrowsIllegalStateException() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"staging"});
            StagingConfigurationValidator validator = createValidStagingValidator();
            validator.setIncludeDevMock(true);

            IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateStagingConfiguration);
            assertTrue(ex.getMessage().contains("Mock OTP disclosure ('app.otp.include-dev-mock') must be disabled in staging"));
        }

        @Test
        @DisplayName("Staging Profile: H2 web console enabled throws IllegalStateException")
        void stagingProfile_H2ConsoleEnabled_ThrowsIllegalStateException() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"staging"});
            StagingConfigurationValidator validator = createValidStagingValidator();
            validator.setH2ConsoleEnabled(true);

            IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateStagingConfiguration);
            assertTrue(ex.getMessage().contains("H2 web console ('spring.h2.console.enabled') must be disabled in staging"));
        }

        @Test
        @DisplayName("Staging Profile: Conflicting 'dev' and 'staging' profiles throws IllegalStateException")
        void stagingProfile_ConflictingDevProfile_ThrowsIllegalStateException() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"staging", "dev"});
            StagingConfigurationValidator validator = createValidStagingValidator();

            IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateStagingConfiguration);
            assertTrue(ex.getMessage().contains("Conflicting active profiles detected ('dev' and 'staging')"));
        }

        @Test
        @DisplayName("Staging Profile: Conflicting 'prod' and 'staging' profiles throws IllegalStateException")
        void stagingProfile_ConflictingProdProfile_ThrowsIllegalStateException() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"staging", "prod"});
            StagingConfigurationValidator validator = createValidStagingValidator();

            IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validateStagingConfiguration);
            assertTrue(ex.getMessage().contains("Conflicting active profiles detected ('prod' and 'staging')"));
        }

        @Test
        @DisplayName("Dev Profile: Staging validator does not enforce staging constraints in development")
        void devProfile_AllowsDevSettingsWithoutEnforcingStagingRules() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
            StagingConfigurationValidator validator = new StagingConfigurationValidator();
            validator.setEnvironment(environment);
            validator.setDatasourceUrl("jdbc:h2:mem:agro_rental_db");
            validator.setDdlAuto("update");
            validator.setFlywayEnabled(false);
            validator.setMockDataEnabled(true);
            validator.setIncludeDevMock(true);
            validator.setH2ConsoleEnabled(true);

            assertDoesNotThrow(validator::validateStagingConfiguration);
        }
    }

    @Nested
    @DisplayName("2. JWT Signing Secret Validation in Staging")
    class JwtSecretValidationTests {

        @Test
        @DisplayName("Staging Profile: Missing or blank JWT secret throws IllegalStateException")
        void stagingProfile_MissingSecret_ThrowsIllegalStateException() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"staging"});
            JwtProperties jwtProperties = new JwtProperties();
            jwtProperties.setEnvironment(environment);
            jwtProperties.setSecret("");

            IllegalStateException ex = assertThrows(IllegalStateException.class, jwtProperties::validateSecret);
            assertTrue(ex.getMessage().contains("JWT Secret ('app.jwt.secret' or 'JWT_SECRET') must be explicitly configured"));
        }

        @Test
        @DisplayName("Staging Profile: Default dev JWT secret throws IllegalStateException")
        void stagingProfile_DefaultDevSecret_ThrowsIllegalStateException() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"staging"});
            JwtProperties jwtProperties = new JwtProperties();
            jwtProperties.setEnvironment(environment);
            jwtProperties.setSecret(JwtProperties.DEFAULT_DEV_SECRET);

            IllegalStateException ex = assertThrows(IllegalStateException.class, jwtProperties::validateSecret);
            assertTrue(ex.getMessage().contains("JWT Secret is using insecure default development key in staging/production"));
        }

        @Test
        @DisplayName("Staging Profile: JWT secret shorter than 32 characters (256 bits) throws IllegalStateException")
        void stagingProfile_ShortSecret_ThrowsIllegalStateException() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"staging"});
            JwtProperties jwtProperties = new JwtProperties();
            jwtProperties.setEnvironment(environment);
            jwtProperties.setSecret("too-short-secret-key-12345"); // 26 chars

            IllegalStateException ex = assertThrows(IllegalStateException.class, jwtProperties::validateSecret);
            assertTrue(ex.getMessage().contains("must be at least 256 bits (32 characters)"));
        }

        @Test
        @DisplayName("Staging Profile: Valid 256-bit externally supplied secret passes validation")
        void stagingProfile_ValidExternalSecret_PassesValidation() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"staging"});
            JwtProperties jwtProperties = new JwtProperties();
            jwtProperties.setEnvironment(environment);
            jwtProperties.setSecret("staging-super-secret-key-that-is-at-least-256-bits-long-for-hmac-sha256");

            assertDoesNotThrow(jwtProperties::validateSecret);
        }

        @Test
        @DisplayName("Dev Profile: Default dev JWT secret is strictly prohibited and throws IllegalStateException")
        void devProfile_DefaultDevSecret_ThrowsIllegalStateException() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
            JwtProperties jwtProperties = new JwtProperties();
            jwtProperties.setEnvironment(environment);
            jwtProperties.setSecret(JwtProperties.DEFAULT_DEV_SECRET);

            IllegalStateException ex = assertThrows(IllegalStateException.class, jwtProperties::validateSecret);
            assertTrue(ex.getMessage().contains("JWT Secret is using insecure default development key"));
        }

        @Test
        @DisplayName("Dev Profile: Missing or blank JWT secret throws IllegalStateException")
        void devProfile_MissingSecret_ThrowsIllegalStateException() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
            JwtProperties jwtProperties = new JwtProperties();
            jwtProperties.setEnvironment(environment);
            jwtProperties.setSecret("");

            IllegalStateException ex = assertThrows(IllegalStateException.class, jwtProperties::validateSecret);
            assertTrue(ex.getMessage().contains("JWT Secret ('app.jwt.secret' or 'JWT_SECRET') must be explicitly configured"));
        }

        @Test
        @DisplayName("Dev Profile: Unresolved placeholder throws IllegalStateException")
        void devProfile_UnresolvedPlaceholder_ThrowsIllegalStateException() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
            JwtProperties jwtProperties = new JwtProperties();
            jwtProperties.setEnvironment(environment);
            jwtProperties.setSecret("${JWT_SECRET}");

            IllegalStateException ex = assertThrows(IllegalStateException.class, jwtProperties::validateSecret);
            assertTrue(ex.getMessage().contains("contains an unresolved placeholder"));
        }

        @Test
        @DisplayName("Dev Profile: Known insecure placeholder throws IllegalStateException")
        void devProfile_KnownInsecurePlaceholder_ThrowsIllegalStateException() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
            JwtProperties jwtProperties = new JwtProperties();
            jwtProperties.setEnvironment(environment);
            jwtProperties.setSecret("changeme");

            IllegalStateException ex = assertThrows(IllegalStateException.class, jwtProperties::validateSecret);
            assertTrue(ex.getMessage().contains("JWT Secret is using insecure default development key or known insecure placeholder"));
        }

        @Test
        @DisplayName("Dev Profile: Short secret (< 32 chars) throws IllegalStateException")
        void devProfile_ShortSecret_ThrowsIllegalStateException() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
            JwtProperties jwtProperties = new JwtProperties();
            jwtProperties.setEnvironment(environment);
            jwtProperties.setSecret("short-dev-secret-12345");

            IllegalStateException ex = assertThrows(IllegalStateException.class, jwtProperties::validateSecret);
            assertTrue(ex.getMessage().contains("must be at least 256 bits (32 characters)"));
        }

        @Test
        @DisplayName("Dev Profile: Valid 256-bit explicitly configured secret passes validation")
        void devProfile_ValidExternalSecret_PassesValidation() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
            JwtProperties jwtProperties = new JwtProperties();
            jwtProperties.setEnvironment(environment);
            jwtProperties.setSecret("dev-explicit-secret-key-that-is-at-least-256-bits-long-ok");

            assertDoesNotThrow(jwtProperties::validateSecret);
        }
    }

    @Nested
    @DisplayName("3. CORS Allowed Origins Validation in Staging")
    class CorsValidationTests {

        @Test
        @DisplayName("Staging Profile: Missing CORS origins throws IllegalStateException")
        void stagingProfile_MissingCorsOrigins_ThrowsIllegalStateException() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"staging"});
            CorsProperties corsProperties = new CorsProperties();
            corsProperties.setEnvironment(environment);
            corsProperties.setAllowedOriginsRaw("");

            IllegalStateException ex = assertThrows(IllegalStateException.class, corsProperties::validateCorsConfiguration);
            assertTrue(ex.getMessage().contains("must be explicitly configured in staging/production"));
        }

        @Test
        @DisplayName("Staging Profile: Wildcard '*' origin throws IllegalStateException")
        void stagingProfile_WildcardCorsOrigins_ThrowsIllegalStateException() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"staging"});
            CorsProperties corsProperties = new CorsProperties();
            corsProperties.setEnvironment(environment);
            corsProperties.setAllowedOriginsRaw("*");

            IllegalStateException ex = assertThrows(IllegalStateException.class, corsProperties::validateCorsConfiguration);
            assertTrue(ex.getMessage().contains("Wildcard origin patterns ('*') are strictly prohibited"));
        }

        @Test
        @DisplayName("Staging Profile: Localhost origin throws IllegalStateException")
        void stagingProfile_LocalhostCorsOrigins_ThrowsIllegalStateException() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"staging"});
            CorsProperties corsProperties = new CorsProperties();
            corsProperties.setEnvironment(environment);
            corsProperties.setAllowedOriginsRaw("http://localhost:5173");

            IllegalStateException ex = assertThrows(IllegalStateException.class, corsProperties::validateCorsConfiguration);
            assertTrue(ex.getMessage().contains("frontend origins must strictly use HTTPS"));
        }

        @Test
        @DisplayName("Staging Profile: Insecure plain HTTP origin throws IllegalStateException")
        void stagingProfile_InsecureHttpCorsOrigins_ThrowsIllegalStateException() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"staging"});
            CorsProperties corsProperties = new CorsProperties();
            corsProperties.setEnvironment(environment);
            corsProperties.setAllowedOriginsRaw("http://staging.agrorent.in");

            IllegalStateException ex = assertThrows(IllegalStateException.class, corsProperties::validateCorsConfiguration);
            assertTrue(ex.getMessage().contains("frontend origins must strictly use HTTPS"));
        }

        @Test
        @DisplayName("Staging Profile: Explicit HTTPS origins validate successfully")
        void stagingProfile_ValidHttpsCorsOrigins_ValidatesSuccessfully() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"staging"});
            CorsProperties corsProperties = new CorsProperties();
            corsProperties.setEnvironment(environment);
            corsProperties.setAllowedOriginsRaw("https://staging.agrorent.in,https://admin-staging.agrorent.in");

            assertDoesNotThrow(corsProperties::validateCorsConfiguration);
            assertEquals(2, corsProperties.getAllowedOrigins().size());
            assertTrue(corsProperties.getAllowedOrigins().contains("https://staging.agrorent.in"));
            assertTrue(corsProperties.getAllowedOrigins().contains("https://admin-staging.agrorent.in"));
        }
    }

    @Nested
    @DisplayName("4. Operator OTP Safety in Staging")
    class OperatorOtpSafetyTests {

        @Test
        @DisplayName("Staging Profile: Dev mock OTP is strictly blocked in staging even if property was true")
        void stagingProfile_DevMockOtpDisclosure_StrictlyBlocked() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"staging"});
            OperatorOtpService otpService = new OperatorOtpService(null, null, null, null);
            otpService.setEnvironment(environment);
            otpService.setIncludeDevMock(true);

            assertFalse(otpService.isDevMockOtpAllowed(), "Mock OTP disclosure must always be false in staging profile");
        }
    }

    @Nested
    @DisplayName("5. Spring Boot ApplicationContextRunner Configuration Loading Tests")
    class ContextRunnerTests {

        // Drop the host OS environment so these tests see only the properties they supply. CI exports
        // SPRING_DATASOURCE_URL etc., which would otherwise satisfy application-staging.properties placeholders.
        private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                .withInitializer(context -> context.getEnvironment().getPropertySources()
                        .remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME))
                .withInitializer(new org.springframework.boot.test.context.ConfigDataApplicationContextInitializer())
                .withUserConfiguration(
                        StagingConfigurationValidator.class,
                        CorsProperties.class,
                        JwtProperties.class
                );

        @Test
        @DisplayName("Staging Profile: Application context fails fast when required datasource URL placeholder is missing")
        void stagingProfile_MissingRequiredDatasourceUrl_FailsContextStartup() {
            contextRunner
                    .withPropertyValues("spring.profiles.active=staging")
                    .run(context -> {
                        assertThat(context).hasFailed();
                        assertThat(context.getStartupFailure().getCause())
                                .isInstanceOf(IllegalStateException.class)
                                .hasMessageContaining("Missing required datasource URL in staging environment");
                    });
        }

        @Test
        @DisplayName("Staging Profile: Application context starts cleanly when valid staging environment is supplied")
        void stagingProfile_ValidEnvironmentSupplied_StartsContextSuccessfully() {
            contextRunner
                    .withPropertyValues(
                            "spring.profiles.active=staging",
                            "SPRING_DATASOURCE_URL=jdbc:mysql://staging-db.internal:3306/agro_staging?useSSL=true",
                            "SPRING_DATASOURCE_USERNAME=staging_app",
                            "SPRING_DATASOURCE_PASSWORD=staging_strong_password_2026",
                            "JWT_SECRET=staging-environment-jwt-secret-minimum-256-bits-for-agrorent",
                            "APP_CORS_ALLOWED_ORIGINS=https://staging.agrorent.in"
                    )
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).hasSingleBean(StagingConfigurationValidator.class);
                        assertThat(context).hasSingleBean(CorsProperties.class);
                        assertThat(context).hasSingleBean(JwtProperties.class);

                        // Verify property values loaded from application-staging.properties
                        Environment env = context.getEnvironment();
                        assertThat(env.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
                        assertThat(env.getProperty("spring.flyway.enabled")).isEqualTo("true");
                        assertThat(env.getProperty("agrorental.seed.mock-data.enabled")).isEqualTo("false");
                        assertThat(env.getProperty("app.otp.include-dev-mock")).isEqualTo("false");
                        assertThat(env.getProperty("spring.h2.console.enabled")).isEqualTo("false");
                    });
        }

        @Test
        @DisplayName("Development Profile: Context loads development defaults safely with explicitly supplied JWT secret")
        void devProfile_LoadsDevelopmentDefaultsSafely() {
            contextRunner
                    .withPropertyValues(
                            "spring.profiles.active=dev",
                            "JWT_SECRET=dev-test-secret-key-must-be-at-least-256-bits-for-agrorent"
                    )
                    .run(context -> {
                        assertThat(context).hasNotFailed();

                        Environment env = context.getEnvironment();
                        assertThat(env.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("update");
                        assertThat(env.getProperty("spring.flyway.enabled")).isEqualTo("false");
                        assertThat(env.getProperty("agrorental.seed.mock-data.enabled")).isEqualTo("true");
                        assertThat(env.getProperty("app.otp.include-dev-mock")).isEqualTo("true");
                        assertThat(env.getProperty("spring.h2.console.enabled")).isEqualTo("true");
                    });
        }

        @Test
        @DisplayName("Development Profile: Context startup fails fast when JWT secret is missing or blank")
        void devProfile_MissingJwtSecret_FailsContextStartup() {
            contextRunner
                    .withPropertyValues("spring.profiles.active=dev")
                    .run(context -> {
                        assertThat(context).hasFailed();
                        assertThat(context.getStartupFailure().getCause())
                                .isInstanceOf(IllegalStateException.class)
                                .hasMessageContaining("JWT Secret ('app.jwt.secret' or 'JWT_SECRET') must be explicitly configured");
                    });
        }

        @Test
        @DisplayName("Development Profile: Context startup fails fast when JWT secret uses default dev secret")
        void devProfile_DefaultDevSecret_FailsContextStartup() {
            contextRunner
                    .withPropertyValues(
                            "spring.profiles.active=dev",
                            "JWT_SECRET=" + JwtProperties.DEFAULT_DEV_SECRET
                    )
                    .run(context -> {
                        assertThat(context).hasFailed();
                        assertThat(context.getStartupFailure().getCause())
                                .isInstanceOf(IllegalStateException.class)
                                .hasMessageContaining("JWT Secret is using insecure default development key");
                    });
        }
    }
}
