package com.agrorental.security;

import com.agrorental.security.jwt.JwtProperties;
import com.agrorental.security.jwt.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

    private JwtService jwtService;
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret("test-secret-key-for-unit-testing-minimum-256-bits-agrorental-test");
        jwtProperties.setExpirationMs(3600000); // 1 hour

        jwtService = new JwtService(jwtProperties);
    }

    @Test
    @DisplayName("Should generate valid JWT token for operator")
    void testGenerateOperatorToken() {
        String token = jwtService.generateOperatorToken(101L, "9876543210");

        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3);
        assertTrue(jwtService.validateToken(token));
        assertEquals(101L, jwtService.extractUserId(token));
        assertEquals("OPERATOR", jwtService.extractRole(token));
        assertEquals("9876543210", jwtService.extractMobileNumber(token));
        assertFalse(jwtService.isTokenExpired(token));
    }

    @Test
    @DisplayName("Should validate token and extract claims correctly")
    void testExtractClaims() {
        String token = jwtService.generateToken(202L, "ADMIN", "9123456780", new HashMap<>());

        assertTrue(jwtService.validateToken(token));
        assertEquals(202L, jwtService.extractUserId(token));
        assertEquals("ADMIN", jwtService.extractRole(token));
        assertEquals("9123456780", jwtService.extractMobileNumber(token));
    }

    @Test
    @DisplayName("Should reject tampered token with invalid signature")
    void testRejectTamperedToken() {
        String token = jwtService.generateOperatorToken(101L, "9876543210");
        String[] parts = token.split("\\.");

        // Tamper with payload
        String tamperedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"999\",\"role\":\"ADMIN\",\"exp\":9999999999}".getBytes());
        String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];

        assertFalse(jwtService.validateToken(tamperedToken));
    }

    @Test
    @DisplayName("Should reject expired token")
    void testRejectExpiredToken() {
        jwtProperties.setExpirationMs(-1000); // Already expired (1 second ago)
        String expiredToken = jwtService.generateOperatorToken(101L, "9876543210");

        assertFalse(jwtService.validateToken(expiredToken));
        assertTrue(jwtService.isTokenExpired(expiredToken));
    }

    @Test
    @DisplayName("Should safely handle null and malformed tokens")
    void testHandleMalformedTokens() {
        assertFalse(jwtService.validateToken(null));
        assertFalse(jwtService.validateToken(""));
        assertFalse(jwtService.validateToken("invalid.token"));
        assertFalse(jwtService.validateToken("header.payload.signature.extra"));

        assertNull(jwtService.extractUserId("malformed-token"));
        assertNull(jwtService.extractRole("malformed-token"));
        assertNull(jwtService.extractMobileNumber("malformed-token"));
    }
}
