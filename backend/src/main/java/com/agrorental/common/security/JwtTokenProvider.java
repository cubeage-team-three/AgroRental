package com.agrorental.common.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtTokenProvider {

    private static final String HMAC_SHA256 = "HmacSHA256";

    @Value("${jwt.secret:AgroRentalPlatformSuperSecretJwtKey2026MustBeAtLeast32BytesLongForHmacSha256}")
    private String jwtSecret;

    @Value("${jwt.expiration-hours:24}")
    private int expirationHours;

    public String generateToken(Long operatorId, String mobileNumber, String fullName, String role, String status) {
        try {
            Instant now = Instant.now();
            Instant expiry = now.plus(expirationHours, ChronoUnit.HOURS);

            // 1. Header
            String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
            String encodedHeader = base64UrlEncode(headerJson);

            // 2. Payload
            String payloadJson = String.format(
                    "{\"sub\":\"%s\",\"operatorId\":%d,\"fullName\":\"%s\",\"role\":\"%s\",\"status\":\"%s\",\"iat\":%d,\"exp\":%d}",
                    escapeJson(mobileNumber),
                    operatorId,
                    escapeJson(fullName),
                    escapeJson(role),
                    escapeJson(status),
                    now.getEpochSecond(),
                    expiry.getEpochSecond()
            );
            String encodedPayload = base64UrlEncode(payloadJson);

            // 3. Signature
            String dataToSign = encodedHeader + "." + encodedPayload;
            String signature = hmacSha256(dataToSign, jwtSecret);

            return dataToSign + "." + signature;
        } catch (Exception e) {
            log.error("Failed to generate JWT token: {}", e.getMessage());
            throw new RuntimeException("Error generating authentication token", e);
        }
    }

    public boolean validateToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }

            String dataToSign = parts[0] + "." + parts[1];
            String expectedSignature = hmacSha256(dataToSign, jwtSecret);

            if (!expectedSignature.equals(parts[2])) {
                return false;
            }

            // Check expiration
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            long exp = extractLongFromJson(payloadJson, "exp");

            if (exp > 0 && Instant.now().getEpochSecond() > exp) {
                return false;
            }

            return true;
        } catch (Exception e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    private long extractLongFromJson(String json, String key) {
        String pattern = "\"" + key + "\":";
        int idx = json.indexOf(pattern);
        if (idx == -1) return 0;
        int start = idx + pattern.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        if (end == -1) return 0;
        try {
            return Long.parseLong(json.substring(start, end).trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private String hmacSha256(String data, String secret) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
        mac.init(secretKeySpec);
        byte[] rawHmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(rawHmac);
    }

    private String base64UrlEncode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    public Long getOperatorIdFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            long opId = extractLongFromJson(payloadJson, "operatorId");
            return opId > 0 ? opId : null;
        } catch (Exception e) {
            log.error("Failed to extract operatorId from token: {}", e.getMessage());
            return null;
        }
    }

    public String getMobileNumberFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            return extractStringFromJson(payloadJson, "sub");
        } catch (Exception e) {
            log.error("Failed to extract mobileNumber from token: {}", e.getMessage());
            return null;
        }
    }

    private String extractStringFromJson(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int idx = json.indexOf(pattern);
        if (idx == -1) return null;
        int start = idx + pattern.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;
        return json.substring(start, end);
    }

    private String escapeJson(String raw) {
        if (raw == null) return "";
        return raw.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
