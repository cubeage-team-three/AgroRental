package com.agrorental.security.jwt;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for generating, signing, parsing, and validating JSON Web Tokens (JWT)
 * using RFC 7519 standard HMAC-SHA256 (HS256) signature verification.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String JWT_HEADER_BASE64 = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

    private final JwtProperties jwtProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Generates a standard JWT access token for an operator.
     *
     * @param operatorId   Primary key ID of the operator.
     * @param mobileNumber Registered mobile number.
     * @return Signed compact JWT string.
     */
    public String generateOperatorToken(Long operatorId, String mobileNumber) {
        return generateToken(operatorId, "OPERATOR", mobileNumber, new HashMap<>());
    }

    /**
     * Generates a JWT token with custom claims.
     *
     * @param userId       Subject ID.
     * @param role         User role name (e.g. OPERATOR, FARMER, PARTNER, ADMIN).
     * @param mobileNumber User mobile number.
     * @param extraClaims  Additional metadata claims.
     * @return Signed compact JWT string.
     */
    public String generateToken(Long userId, String role, String mobileNumber, Map<String, Object> extraClaims) {
        long nowSeconds = Instant.now().getEpochSecond();
        long expSeconds = nowSeconds + (jwtProperties.getExpirationMs() / 1000);

        Map<String, Object> claims = new HashMap<>(extraClaims);
        claims.put("sub", String.valueOf(userId));
        claims.put("role", role);
        if (mobileNumber != null) {
            claims.put("mobile", mobileNumber);
        }
        claims.put("iat", nowSeconds);
        claims.put("exp", expSeconds);

        try {
            String payloadJson = objectMapper.writeValueAsString(claims);
            String payloadBase64 = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

            String dataToSign = JWT_HEADER_BASE64 + "." + payloadBase64;
            String signature = sign(dataToSign, jwtProperties.getSecret());

            return dataToSign + "." + signature;
        } catch (Exception e) {
            log.error("Failed to generate JWT token: {}", e.getMessage());
            throw new IllegalStateException("Could not generate authentication token", e);
        }
    }

    /**
     * Validates whether a token is well-formed, correctly signed, and unexpired.
     *
     * @param token Compact JWT string.
     * @return true if valid and active, false otherwise.
     */
    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        String[] parts = token.trim().split("\\.");
        if (parts.length != 3) {
            return false;
        }

        String headerBase64 = parts[0];
        String payloadBase64 = parts[1];
        String signature = parts[2];

        try {
            // Verify HMAC signature
            String dataToSign = headerBase64 + "." + payloadBase64;
            String expectedSignature = sign(dataToSign, jwtProperties.getSecret());
            if (!MessageDigest.isEqual(
                    signature.getBytes(StandardCharsets.UTF_8),
                    expectedSignature.getBytes(StandardCharsets.UTF_8))) {
                return false;
            }

            // Verify expiration
            JsonNode payload = parsePayload(payloadBase64);
            if (payload == null || !payload.has("exp")) {
                return false;
            }

            long exp = payload.get("exp").asLong();
            long now = Instant.now().getEpochSecond();
            return exp > now;
        } catch (Exception e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extracts the Subject (User ID) from a JWT token.
     */
    public Long extractUserId(String token) {
        JsonNode payload = extractClaims(token);
        if (payload != null && payload.has("sub")) {
            try {
                return Long.parseLong(payload.get("sub").asText());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Extracts the Role from a JWT token.
     */
    public String extractRole(String token) {
        JsonNode payload = extractClaims(token);
        if (payload != null && payload.has("role")) {
            return payload.get("role").asText();
        }
        return null;
    }

    /**
     * Extracts the Mobile Number from a JWT token.
     */
    public String extractMobileNumber(String token) {
        JsonNode payload = extractClaims(token);
        if (payload != null && payload.has("mobile")) {
            return payload.get("mobile").asText();
        }
        return null;
    }

    /**
     * Checks if the token is expired.
     */
    public boolean isTokenExpired(String token) {
        JsonNode payload = extractClaims(token);
        if (payload == null || !payload.has("exp")) {
            return true;
        }
        long exp = payload.get("exp").asLong();
        return exp <= Instant.now().getEpochSecond();
    }

    /**
     * Returns token lifetime in seconds.
     */
    public long getExpiresInSeconds() {
        return jwtProperties.getExpirationMs() / 1000;
    }

    /**
     * Extracts and parses the claims JSON payload from a valid JWT token.
     */
    public JsonNode extractClaims(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String[] parts = token.trim().split("\\.");
        if (parts.length != 3) {
            return null;
        }
        return parsePayload(parts[1]);
    }

    private JsonNode parsePayload(String payloadBase64) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(payloadBase64);
            return objectMapper.readTree(new String(decoded, StandardCharsets.UTF_8));
        } catch (Exception e) {
            return null;
        }
    }

    private String sign(String data, String secret) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
        mac.init(secretKeySpec);
        byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
    }
}
