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

    /** Claim naming the single account table the subject ID belongs to (see {@link AccountType}). */
    public static final String ACCOUNT_TYPE_CLAIM = "acct";

    /**
     * Claim marking a restricted-purpose token. Session tokens never carry it; the JWT filter refuses to treat
     * a scoped token as a session token.
     */
    public static final String SCOPE_CLAIM = "scope";

    /** Scope (and role) of the short-lived token a just-registered operator uses to upload KYC documents. */
    public static final String OPERATOR_ONBOARDING_SCOPE = "OPERATOR_ONBOARDING";
    public static final String OPERATOR_ONBOARDING_ROLE = "OPERATOR_ONBOARDING";

    /** Lifetime of an operator onboarding token (contract: at most 2 hours). */
    public static final long OPERATOR_ONBOARDING_TTL_SECONDS = 2 * 60 * 60;

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
        return generateToken(operatorId, "OPERATOR", mobileNumber, accountTypeClaims(AccountType.OPERATOR));
    }

    /**
     * Issues the onboarding token returned by operator registration. It is bound to operators.id
     * ({@code acct=OPERATOR}) but carries role and scope {@value #OPERATOR_ONBOARDING_SCOPE} instead of
     * OPERATOR, so it can never authenticate as an operator session: the JWT filter grants it only
     * ROLE_OPERATOR_ONBOARDING, and only for uploading that operator's own KYC documents while the account
     * is still PENDING.
     */
    public String generateOperatorOnboardingToken(Long operatorId) {
        if (operatorId == null) {
            throw new IllegalArgumentException("An operator ID is required to issue an onboarding token");
        }
        long nowSeconds = Instant.now().getEpochSecond();
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", String.valueOf(operatorId));
        claims.put("role", OPERATOR_ONBOARDING_ROLE);
        claims.put(SCOPE_CLAIM, OPERATOR_ONBOARDING_SCOPE);
        claims.put(ACCOUNT_TYPE_CLAIM, AccountType.OPERATOR.name());
        claims.put("iat", nowSeconds);
        claims.put("exp", nowSeconds + OPERATOR_ONBOARDING_TTL_SECONDS);
        return sign(claims);
    }

    /** Claims map carrying the mandatory account type, for use with {@link #generateToken}. */
    public static Map<String, Object> accountTypeClaims(AccountType accountType) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(ACCOUNT_TYPE_CLAIM, accountType.name());
        return claims;
    }

    /**
     * Generates a JWT token with custom claims.
     *
     * @param userId       Subject ID.
     * @param role         User role name (e.g. OPERATOR, FARMER, PARTNER, ADMIN).
     * @param mobileNumber User mobile number.
     * @param extraClaims  Additional metadata claims; must contain {@link #ACCOUNT_TYPE_CLAIM} with an
     *                     {@link AccountType} that permits the given role (see {@link #accountTypeClaims}).
     * @return Signed compact JWT string.
     * @throws IllegalArgumentException if the account type is missing, unsupported or inconsistent with the role.
     */
    public String generateToken(Long userId, String role, String mobileNumber, Map<String, Object> extraClaims) {
        Object accountTypeClaim = extraClaims != null ? extraClaims.get(ACCOUNT_TYPE_CLAIM) : null;
        AccountType accountType = AccountType.fromClaim(accountTypeClaim != null ? accountTypeClaim.toString() : null);
        if (userId == null || accountType == null || !accountType.permitsRole(role)) {
            // Fail closed: never issue a token whose subject could be resolved against an ambiguous table
            throw new IllegalArgumentException("A valid account type matching role '" + role + "' is required to issue a token");
        }
        if (extraClaims.containsKey(SCOPE_CLAIM)) {
            // Session tokens are never scoped; scoped tokens have dedicated issuers
            throw new IllegalArgumentException("Session tokens cannot carry a '" + SCOPE_CLAIM + "' claim");
        }

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
        return sign(claims);
    }

    private String sign(Map<String, Object> claims) {
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
     * Extracts the account type claim from a JWT token (null if absent).
     */
    public String extractAccountType(String token) {
        JsonNode payload = extractClaims(token);
        if (payload != null && payload.has(ACCOUNT_TYPE_CLAIM)) {
            return payload.get(ACCOUNT_TYPE_CLAIM).asText();
        }
        return null;
    }

    /**
     * Extracts the scope claim from a JWT token (null for ordinary session tokens).
     */
    public String extractScope(String token) {
        JsonNode payload = extractClaims(token);
        if (payload != null && payload.has(SCOPE_CLAIM)) {
            return payload.get(SCOPE_CLAIM).asText();
        }
        return null;
    }

    /**
     * Extracts the Mobile Number or Email from a JWT token.
     */
    public String extractMobileNumber(String token) {
        JsonNode payload = extractClaims(token);
        if (payload != null && payload.has("mobile")) {
            return payload.get("mobile").asText();
        }
        return null;
    }

    /**
     * Extracts the Email from a JWT token payload.
     */
    public String extractEmail(String token) {
        return extractMobileNumber(token);
    }

    /**
     * Extracts the Username/Subject from a JWT token payload.
     */
    public String extractUsername(String token) {
        JsonNode payload = extractClaims(token);
        if (payload != null && payload.has("sub")) {
            return payload.get("sub").asText();
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
