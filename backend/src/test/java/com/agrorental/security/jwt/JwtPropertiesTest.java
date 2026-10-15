package com.agrorental.security.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("JwtProperties Security and Fail-Fast Validation Tests")
class JwtPropertiesTest {

    private JwtProperties jwtProperties;
    private Environment environment;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        environment = mock(Environment.class);
        jwtProperties.setEnvironment(environment);
    }

    @Nested
    @DisplayName("1. Missing and Blank Secret Validation")
    class MissingAndBlankSecretTests {

        @Test
        @DisplayName("Null secret throws IllegalStateException across all profiles")
        void nullSecret_ThrowsIllegalStateException() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
            jwtProperties.setSecret(null);

            IllegalStateException ex = assertThrows(IllegalStateException.class, jwtProperties::validateSecret);
            assertTrue(ex.getMessage().contains("JWT Secret ('app.jwt.secret' or 'JWT_SECRET') must be explicitly configured"));
            assertFalse(ex.getMessage().contains("null"));
        }

        @Test
        @DisplayName("Empty secret throws IllegalStateException in dev profile")
        void emptySecret_DevProfile_ThrowsIllegalStateException() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
            jwtProperties.setSecret("");

            IllegalStateException ex = assertThrows(IllegalStateException.class, jwtProperties::validateSecret);
            assertTrue(ex.getMessage().contains("JWT Secret ('app.jwt.secret' or 'JWT_SECRET') must be explicitly configured"));
        }

        @Test
        @DisplayName("Empty secret throws IllegalStateException in staging profile")
        void emptySecret_StagingProfile_ThrowsIllegalStateException() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"staging"});
            jwtProperties.setSecret("");

            IllegalStateException ex = assertThrows(IllegalStateException.class, jwtProperties::validateSecret);
            assertTrue(ex.getMessage().contains("JWT Secret ('app.jwt.secret' or 'JWT_SECRET') must be explicitly configured"));
        }

        @Test
        @DisplayName("Empty secret throws IllegalStateException in prod profile")
        void emptySecret_ProdProfile_ThrowsIllegalStateException() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
            jwtProperties.setSecret("");

            IllegalStateException ex = assertThrows(IllegalStateException.class, jwtProperties::validateSecret);
            assertTrue(ex.getMessage().contains("JWT Secret ('app.jwt.secret' or 'JWT_SECRET') must be explicitly configured"));
        }

        @ParameterizedTest(name = "Blank secret with whitespace: ''{0}'' throws IllegalStateException")
        @ValueSource(strings = {"   ", " \t ", "\n\r", "          "})
        void blankSecret_ThrowsIllegalStateException(String blankSecret) {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
            jwtProperties.setSecret(blankSecret);

            IllegalStateException ex = assertThrows(IllegalStateException.class, jwtProperties::validateSecret);
            assertTrue(ex.getMessage().contains("JWT Secret ('app.jwt.secret' or 'JWT_SECRET') must be explicitly configured"));
        }
    }

    @Nested
    @DisplayName("2. Unresolved Placeholder Validation")
    class UnresolvedPlaceholderTests {

        @ParameterizedTest(name = "Unresolved placeholder ''{0}'' throws IllegalStateException")
        @ValueSource(strings = {
                "${JWT_SECRET}",
                "${app.jwt.secret}",
                "${JWT_SECRET:default}",
                "prefix_${UNRESOLVED}_suffix"
        })
        void unresolvedPlaceholder_DevProfile_ThrowsIllegalStateException(String placeholder) {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
            jwtProperties.setSecret(placeholder);

            IllegalStateException ex = assertThrows(IllegalStateException.class, jwtProperties::validateSecret);
            assertTrue(ex.getMessage().contains("contains an unresolved placeholder"));
            // Verify secret placeholder value is not exposed in error message
            assertFalse(ex.getMessage().contains(placeholder));
        }

        @Test
        @DisplayName("Unresolved placeholder in staging profile throws IllegalStateException")
        void unresolvedPlaceholder_StagingProfile_ThrowsIllegalStateException() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"staging"});
            jwtProperties.setSecret("${JWT_SECRET}");

            IllegalStateException ex = assertThrows(IllegalStateException.class, jwtProperties::validateSecret);
            assertTrue(ex.getMessage().contains("JWT Secret ('app.jwt.secret' or 'JWT_SECRET') must be explicitly configured"));
        }
    }

    @Nested
    @DisplayName("3. Known Insecure Placeholder and Default Secret Validation")
    class KnownInsecureSecretTests {

        @Test
        @DisplayName("Predictable default dev secret is strictly rejected in dev profile")
        void defaultDevSecret_DevProfile_ThrowsIllegalStateException() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
            jwtProperties.setSecret(JwtProperties.DEFAULT_DEV_SECRET);

            IllegalStateException ex = assertThrows(IllegalStateException.class, jwtProperties::validateSecret);
            assertTrue(ex.getMessage().contains("JWT Secret is using insecure default development key"));
            // Secret value must not be leaked in message
            assertFalse(ex.getMessage().contains(JwtProperties.DEFAULT_DEV_SECRET));
        }

        @Test
        @DisplayName("Predictable default dev secret is strictly rejected in staging profile")
        void defaultDevSecret_StagingProfile_ThrowsIllegalStateException() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"staging"});
            jwtProperties.setSecret(JwtProperties.DEFAULT_DEV_SECRET);

            IllegalStateException ex = assertThrows(IllegalStateException.class, jwtProperties::validateSecret);
            assertTrue(ex.getMessage().contains("JWT Secret is using insecure default development key in staging/production"));
        }

        @Test
        @DisplayName("Predictable default dev secret is strictly rejected in prod profile")
        void defaultDevSecret_ProdProfile_ThrowsIllegalStateException() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
            jwtProperties.setSecret(JwtProperties.DEFAULT_DEV_SECRET);

            IllegalStateException ex = assertThrows(IllegalStateException.class, jwtProperties::validateSecret);
            assertTrue(ex.getMessage().contains("JWT Secret is using insecure default development key in staging/production"));
        }

        @ParameterizedTest(name = "Known insecure placeholder ''{0}'' throws IllegalStateException")
        @ValueSource(strings = {
                "secret",
                "changeme",
                "change-me",
                "change_me",
                "change-in-prod",
                "default",
                "dev-secret",
                "dev_secret",
                "development-secret",
                "your-secret-key",
                "your-jwt-secret",
                "your-256-bit-secret",
                "jwt-secret",
                "password",
                "admin",
                "12345678",
                "replace-me",
                "replace-with-secure-secret",
                "placeholder",
                "<jwt-secret>",
                "insert_jwt_secret_here",
                "your_jwt_secret_here"
        })
        void knownInsecurePlaceholders_ThrowsIllegalStateException(String insecureSecret) {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
            jwtProperties.setSecret(insecureSecret);

            IllegalStateException ex = assertThrows(IllegalStateException.class, jwtProperties::validateSecret);
            assertTrue(ex.getMessage().contains("JWT Secret is using insecure default development key or known insecure placeholder"));
        }

        @Test
        @DisplayName("Verifies candidate secret is never printed or leaked in error messages")
        void secretIsNeverLeakedInErrorMessage() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
            String canarySecret = "my_canary_key15"; // 15 chars (< 32), guaranteed not in error message text
            jwtProperties.setSecret(canarySecret);

            IllegalStateException ex = assertThrows(IllegalStateException.class, jwtProperties::validateSecret);
            assertFalse(ex.getMessage().contains(canarySecret), "Secret must never be leaked in error message");
        }
    }

    @Nested
    @DisplayName("4. Secret Strength Validation (256-bit / 32 character minimum)")
    class SecretStrengthTests {

        @ParameterizedTest(name = "Short secret of length {0} throws IllegalStateException")
        @ValueSource(strings = {
                "short",
                "12345678901234567890",
                "1234567890123456789012345678901" // 31 chars
        })
        void shortSecret_DevProfile_ThrowsIllegalStateException(String shortSecret) {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
            jwtProperties.setSecret(shortSecret);

            IllegalStateException ex = assertThrows(IllegalStateException.class, jwtProperties::validateSecret);
            assertTrue(ex.getMessage().contains("must be at least 256 bits (32 characters)"));
            assertTrue(ex.getMessage().contains("Current length: " + shortSecret.length()));
            // Secret value must not be leaked
            assertFalse(ex.getMessage().contains(shortSecret));
        }

        @Test
        @DisplayName("Short secret in staging profile throws IllegalStateException with staging message")
        void shortSecret_StagingProfile_ThrowsIllegalStateException() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"staging"});
            jwtProperties.setSecret("too-short-for-staging-12345"); // 27 chars

            IllegalStateException ex = assertThrows(IllegalStateException.class, jwtProperties::validateSecret);
            assertTrue(ex.getMessage().contains("must be at least 256 bits (32 characters)"));
            assertTrue(ex.getMessage().contains("in staging/production environment"));
        }

        @Test
        @DisplayName("Short secret in prod profile throws IllegalStateException with prod message")
        void shortSecret_ProdProfile_ThrowsIllegalStateException() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
            jwtProperties.setSecret("too-short-for-prod-1234567"); // 26 chars

            IllegalStateException ex = assertThrows(IllegalStateException.class, jwtProperties::validateSecret);
            assertTrue(ex.getMessage().contains("must be at least 256 bits (32 characters)"));
            assertTrue(ex.getMessage().contains("in staging/production environment"));
        }
    }

    @Nested
    @DisplayName("5. Valid Explicitly Configured Secrets")
    class ValidSecretTests {

        @Test
        @DisplayName("Exact 32-character secret passes validation in dev profile")
        void exact32CharsSecret_DevProfile_PassesValidation() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});
            jwtProperties.setSecret("12345678901234567890123456789012"); // exactly 32 chars

            assertDoesNotThrow(jwtProperties::validateSecret);
        }

        @Test
        @DisplayName("Valid 64-character hex secret passes validation in staging profile")
        void valid64HexSecret_StagingProfile_PassesValidation() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"staging"});
            jwtProperties.setSecret("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");

            assertDoesNotThrow(jwtProperties::validateSecret);
        }

        @Test
        @DisplayName("Valid 44-character base64 secret passes validation in prod profile")
        void validBase64Secret_ProdProfile_PassesValidation() {
            when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
            jwtProperties.setSecret("4q2+7w5e9y1u3i5o7p9a1s3d5f7g9h1j3k5l7z9x1c3=");

            assertDoesNotThrow(jwtProperties::validateSecret);
        }

        @Test
        @DisplayName("Valid test secret passes validation without active profiles (null environment)")
        void validSecret_NullEnvironment_PassesValidation() {
            jwtProperties.setEnvironment(null);
            jwtProperties.setSecret("valid-test-key-32-chars-minimum-length-for-hmac-sha256-ok");

            assertDoesNotThrow(jwtProperties::validateSecret);
        }
    }
}
