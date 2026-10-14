package com.agrorental.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CORS Configuration & Environment Safety Tests")
class CorsConfigurationTest {

    @Mock
    private Environment environment;

    // --- 1. CorsProperties Environment Validation Tests ---

    @Test
    @DisplayName("Dev Profile: Permitted local development origins parse correctly including port 5174")
    void devProfile_WithLocalhostOrigins_ValidatesSuccessfully() {
        CorsProperties corsProperties = new CorsProperties();
        corsProperties.setEnvironment(environment);
        corsProperties.setAllowedOriginsRaw("http://localhost:5173,http://localhost:5174,http://localhost:3000,http://127.0.0.1:5173,http://127.0.0.1:5174");
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});

        corsProperties.validateCorsConfiguration();

        List<String> origins = corsProperties.getAllowedOrigins();
        assertEquals(5, origins.size());
        assertTrue(origins.contains("http://localhost:5173"));
        assertTrue(origins.contains("http://localhost:5174"));
        assertTrue(origins.contains("http://localhost:3000"));
        assertTrue(origins.contains("http://127.0.0.1:5173"));
        assertTrue(origins.contains("http://127.0.0.1:5174"));
    }

    @Test
    @DisplayName("Dev Profile: Defaults to standard localhost origins when property is missing/empty")
    void devProfile_WithEmptyProperty_DefaultsToSafeLocalhostOrigins() {
        CorsProperties corsProperties = new CorsProperties();
        corsProperties.setEnvironment(environment);
        corsProperties.setAllowedOriginsRaw("");
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});

        corsProperties.validateCorsConfiguration();

        List<String> origins = corsProperties.getAllowedOrigins();
        assertFalse(origins.isEmpty());
        assertTrue(origins.contains("http://localhost:5173"));
        assertTrue(origins.contains("http://localhost:5174"));
        assertTrue(origins.contains("http://127.0.0.1:5174"));
        assertFalse(origins.contains("*"));
    }

    @Test
    @DisplayName("Dev Profile: Wildcard origin pattern is strictly prohibited even in development")
    void devProfile_WithWildcardOrigin_ThrowsIllegalStateException() {
        CorsProperties corsProperties = new CorsProperties();
        corsProperties.setEnvironment(environment);
        corsProperties.setAllowedOriginsRaw("*");
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});

        IllegalStateException ex = assertThrows(IllegalStateException.class, corsProperties::validateCorsConfiguration);
        assertTrue(ex.getMessage().contains("Wildcard origin pattern ('*') is not allowed"));
    }

    @Test
    @DisplayName("Production Profile: Valid explicit HTTPS origins validate successfully")
    void prodProfile_WithValidHttpsOrigins_ValidatesSuccessfully() {
        CorsProperties corsProperties = new CorsProperties();
        corsProperties.setEnvironment(environment);
        corsProperties.setAllowedOriginsRaw("https://agrorent.in,https://app.agrorent.in");
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        corsProperties.validateCorsConfiguration();

        List<String> origins = corsProperties.getAllowedOrigins();
        assertEquals(2, origins.size());
        assertTrue(origins.contains("https://agrorent.in"));
        assertTrue(origins.contains("https://app.agrorent.in"));
    }

    @Test
    @DisplayName("Production Profile: Fails fast on startup if CORS origins are missing or empty")
    void prodProfile_WithMissingOrigins_ThrowsIllegalStateException() {
        CorsProperties corsProperties = new CorsProperties();
        corsProperties.setEnvironment(environment);
        corsProperties.setAllowedOriginsRaw("");
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        IllegalStateException ex = assertThrows(IllegalStateException.class, corsProperties::validateCorsConfiguration);
        assertTrue(ex.getMessage().contains("must be explicitly configured in staging/production"));
    }

    @Test
    @DisplayName("Production Profile: Fails fast if wildcard origin pattern is configured")
    void prodProfile_WithWildcardOrigin_ThrowsIllegalStateException() {
        CorsProperties corsProperties = new CorsProperties();
        corsProperties.setEnvironment(environment);
        corsProperties.setAllowedOriginsRaw("*");
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        IllegalStateException ex = assertThrows(IllegalStateException.class, corsProperties::validateCorsConfiguration);
        assertTrue(ex.getMessage().contains("Wildcard origin patterns ('*') are strictly prohibited"));
    }

    @Test
    @DisplayName("Production Profile: Fails fast if localhost or plain HTTP origin is configured")
    void prodProfile_WithLocalhostOrHttp_ThrowsIllegalStateException() {
        CorsProperties corsProperties = new CorsProperties();
        corsProperties.setEnvironment(environment);
        corsProperties.setAllowedOriginsRaw("http://localhost:5173");
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});

        IllegalStateException ex = assertThrows(IllegalStateException.class, corsProperties::validateCorsConfiguration);
        assertTrue(ex.getMessage().contains("frontend origins must strictly use HTTPS"));
    }

    @Test
    @DisplayName("Staging Profile: Valid explicit HTTPS origins validate successfully")
    void stagingProfile_WithValidHttpsOrigins_ValidatesSuccessfully() {
        CorsProperties corsProperties = new CorsProperties();
        corsProperties.setEnvironment(environment);
        corsProperties.setAllowedOriginsRaw("https://staging.agrorent.in");
        when(environment.getActiveProfiles()).thenReturn(new String[]{"staging"});

        corsProperties.validateCorsConfiguration();

        List<String> origins = corsProperties.getAllowedOrigins();
        assertEquals(1, origins.size());
        assertTrue(origins.contains("https://staging.agrorent.in"));
    }

    @Test
    @DisplayName("Staging Profile: Fails fast if CORS origins are missing or empty")
    void stagingProfile_WithMissingOrigins_ThrowsIllegalStateException() {
        CorsProperties corsProperties = new CorsProperties();
        corsProperties.setEnvironment(environment);
        corsProperties.setAllowedOriginsRaw(null);
        when(environment.getActiveProfiles()).thenReturn(new String[]{"staging"});

        IllegalStateException ex = assertThrows(IllegalStateException.class, corsProperties::validateCorsConfiguration);
        assertTrue(ex.getMessage().contains("must be explicitly configured in staging/production"));
    }

    @Test
    @DisplayName("Staging Profile: Fails fast if plain HTTP origin is configured")
    void stagingProfile_WithPlainHttpOrigin_ThrowsIllegalStateException() {
        CorsProperties corsProperties = new CorsProperties();
        corsProperties.setEnvironment(environment);
        corsProperties.setAllowedOriginsRaw("http://staging.agrorent.in");
        when(environment.getActiveProfiles()).thenReturn(new String[]{"staging"});

        IllegalStateException ex = assertThrows(IllegalStateException.class, corsProperties::validateCorsConfiguration);
        assertTrue(ex.getMessage().contains("frontend origins must strictly use HTTPS"));
    }

    // --- 2. SecurityConfig CorsConfigurationSource Request-Matching Tests ---

    @Test
    @DisplayName("SecurityConfig: Requests from allowed origin receive CORS headers and credential authorization")
    void securityConfig_RequestFromAllowedOrigin_MatchesSuccessfully() {
        CorsProperties corsProperties = new CorsProperties();
        corsProperties.setAllowedOrigins(List.of("http://localhost:5173", "https://agrorent.in"));

        SecurityConfig securityConfig = new SecurityConfig(null, corsProperties);
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/bookings");
        request.addHeader("Origin", "http://localhost:5173");

        CorsConfiguration config = source.getCorsConfiguration(request);
        assertNotNull(config);
        assertEquals("http://localhost:5173", config.checkOrigin("http://localhost:5173"));
        assertTrue(config.getAllowCredentials());
        assertTrue(config.getAllowedMethods().containsAll(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")));
        assertTrue(config.getExposedHeaders().contains("Authorization"));
    }

    @Test
    @DisplayName("SecurityConfig: Requests from untrusted origin are rejected by CORS configuration")
    void securityConfig_RequestFromUntrustedOrigin_IsRejected() {
        CorsProperties corsProperties = new CorsProperties();
        corsProperties.setAllowedOrigins(List.of("http://localhost:5173", "https://agrorent.in"));

        SecurityConfig securityConfig = new SecurityConfig(null, corsProperties);
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/bookings");
        request.addHeader("Origin", "https://malicious-attacker.com");

        CorsConfiguration config = source.getCorsConfiguration(request);
        assertNotNull(config);
        assertNull(config.checkOrigin("https://malicious-attacker.com"), "Untrusted origin must return null for checkOrigin");
    }

    @Test
    @DisplayName("SecurityConfig: Preflight OPTIONS request headers are correctly accepted for allowed origin")
    void securityConfig_PreflightOptionsRequest_AllowedHeadersAndMaxAgeVerified() {
        CorsProperties corsProperties = new CorsProperties();
        corsProperties.setAllowedOrigins(List.of("http://localhost:5173"));

        SecurityConfig securityConfig = new SecurityConfig(null, corsProperties);
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("OPTIONS");
        request.setRequestURI("/api/payments/create-intent");
        request.addHeader("Origin", "http://localhost:5173");
        request.addHeader("Access-Control-Request-Method", "POST");
        request.addHeader("Access-Control-Request-Headers", "Authorization, Content-Type");

        CorsConfiguration config = source.getCorsConfiguration(request);
        assertNotNull(config);
        assertEquals("http://localhost:5173", config.checkOrigin("http://localhost:5173"));
        assertEquals(3600L, config.getMaxAge());
        assertTrue(config.getAllowCredentials());
    }

    @Test
    @DisplayName("SecurityConfig: Preflight OPTIONS on /api/auth/login and /api/operators/login allowed for localhost:5174")
    void securityConfig_PreflightLoginEndpoints_AllowedOrigins5174() {
        CorsProperties corsProperties = new CorsProperties();
        corsProperties.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:5174", "http://127.0.0.1:5174"));

        SecurityConfig securityConfig = new SecurityConfig(null, corsProperties);
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();

        // 1. /api/auth/login preflight
        MockHttpServletRequest authRequest = new MockHttpServletRequest();
        authRequest.setMethod("OPTIONS");
        authRequest.setRequestURI("/api/auth/login");
        authRequest.addHeader("Origin", "http://localhost:5174");
        authRequest.addHeader("Access-Control-Request-Method", "POST");
        authRequest.addHeader("Access-Control-Request-Headers", "Content-Type");

        CorsConfiguration authConfig = source.getCorsConfiguration(authRequest);
        assertNotNull(authConfig);
        assertEquals("http://localhost:5174", authConfig.checkOrigin("http://localhost:5174"));
        assertTrue(authConfig.getAllowCredentials());
        assertTrue(authConfig.getAllowedMethods().contains("POST"));

        // 2. /api/operators/login preflight
        MockHttpServletRequest opRequest = new MockHttpServletRequest();
        opRequest.setMethod("OPTIONS");
        opRequest.setRequestURI("/api/operators/login");
        opRequest.addHeader("Origin", "http://localhost:5174");
        opRequest.addHeader("Access-Control-Request-Method", "POST");
        opRequest.addHeader("Access-Control-Request-Headers", "Content-Type");

        CorsConfiguration opConfig = source.getCorsConfiguration(opRequest);
        assertNotNull(opConfig);
        assertEquals("http://localhost:5174", opConfig.checkOrigin("http://localhost:5174"));
        assertTrue(opConfig.getAllowCredentials());
        assertTrue(opConfig.getAllowedMethods().contains("POST"));

        // 3. Unapproved origin rejected
        MockHttpServletRequest unapprovedRequest = new MockHttpServletRequest();
        unapprovedRequest.setMethod("OPTIONS");
        unapprovedRequest.setRequestURI("/api/auth/login");
        unapprovedRequest.addHeader("Origin", "http://unapproved-origin.com");
        unapprovedRequest.addHeader("Access-Control-Request-Method", "POST");

        CorsConfiguration unapprovedConfig = source.getCorsConfiguration(unapprovedRequest);
        assertNotNull(unapprovedConfig);
        assertNull(unapprovedConfig.checkOrigin("http://unapproved-origin.com"));
    }
}
