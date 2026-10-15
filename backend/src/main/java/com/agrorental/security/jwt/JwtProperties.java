package com.agrorental.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Configuration properties holder for JWT authentication parameters.
 * Validates secret complexity, ensures explicit configuration, and strictly prohibits
 * missing, blank, unresolved, or insecure placeholder secrets across all runtime profiles.
 */
@Getter
@Setter
@Component
public class JwtProperties {

    /**
     * Known insecure legacy development secret. Retained strictly as a blacklisted key identifier
     * to prevent its use and maintain test compatibility.
     */
    public static final String DEFAULT_DEV_SECRET = "agrorental-super-secret-jwt-key-for-development-mode-only-must-be-256-bits-minimum-change-in-prod";

    private static final Set<String> KNOWN_INSECURE_SECRETS = Set.of(
            DEFAULT_DEV_SECRET.toLowerCase(Locale.ROOT),
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
    );

    @Value("${app.jwt.secret:}")
    private String secret;

    @Value("${app.jwt.expiration:3600000}")
    private long expirationMs;

    @Autowired(required = false)
    private Environment environment;

    @PostConstruct
    public void validateSecret() {
        boolean isProdOrStaging = isProductionOrStagingEnvironment();

        // 1. Secret must be explicitly configured and non-blank across ALL runtime profiles
        if (!StringUtils.hasText(secret)) {
            if (isProdOrStaging) {
                throw new IllegalStateException("CRITICAL SECURITY ERROR: JWT Secret ('app.jwt.secret' or 'JWT_SECRET') " +
                        "must be explicitly configured in staging/production environment. Application startup aborted.");
            } else {
                throw new IllegalStateException("CRITICAL SECURITY ERROR: JWT Secret ('app.jwt.secret' or 'JWT_SECRET') " +
                        "must be explicitly configured. Application startup aborted.");
            }
        }

        // 2. Secret must not be an unresolved placeholder
        if (secret.contains("${")) {
            if (isProdOrStaging) {
                throw new IllegalStateException("CRITICAL SECURITY ERROR: JWT Secret ('app.jwt.secret' or 'JWT_SECRET') " +
                        "must be explicitly configured in staging/production environment. Application startup aborted.");
            } else {
                throw new IllegalStateException("CRITICAL SECURITY ERROR: JWT Secret ('app.jwt.secret' or 'JWT_SECRET') " +
                        "contains an unresolved placeholder. Application startup aborted.");
            }
        }

        // 3. Secret must not be a known insecure development or default placeholder
        if (isKnownInsecureSecret(secret)) {
            if (isProdOrStaging) {
                throw new IllegalStateException("CRITICAL SECURITY ERROR: JWT Secret is using insecure default development key " +
                        "in staging/production environment. Application startup aborted. Please set the JWT_SECRET environment variable.");
            } else {
                throw new IllegalStateException("CRITICAL SECURITY ERROR: JWT Secret is using insecure default development key " +
                        "or known insecure placeholder. Application startup aborted. Please set the JWT_SECRET environment variable.");
            }
        }

        // 4. Secret must meet minimum 256-bit (32 character) HMAC-SHA256 complexity
        if (secret.length() < 32) {
            if (isProdOrStaging) {
                throw new IllegalStateException("CRITICAL SECURITY ERROR: JWT Secret must be at least 256 bits (32 characters) " +
                        "in staging/production environment for HMAC-SHA256. Current length: " + secret.length() + ". Application startup aborted.");
            } else {
                throw new IllegalStateException("CRITICAL SECURITY ERROR: JWT Secret must be at least 256 bits (32 characters) " +
                        "for HMAC-SHA256. Current length: " + secret.length() + ". Application startup aborted.");
            }
        }
    }

    private boolean isKnownInsecureSecret(String candidate) {
        if (candidate == null) {
            return false;
        }
        String trimmed = candidate.trim();
        if (DEFAULT_DEV_SECRET.equals(trimmed)) {
            return true;
        }
        return KNOWN_INSECURE_SECRETS.contains(trimmed.toLowerCase(Locale.ROOT));
    }

    private boolean isProductionOrStagingEnvironment() {
        if (environment == null) {
            return false;
        }
        String[] activeProfilesArr = environment.getActiveProfiles();
        List<String> activeProfiles = activeProfilesArr != null ? Arrays.asList(activeProfilesArr) : Collections.emptyList();

        String[] defaultProfilesArr = environment.getDefaultProfiles();
        List<String> defaultProfiles = defaultProfilesArr != null ? Arrays.asList(defaultProfilesArr) : Collections.emptyList();

        return activeProfiles.contains("prod") || activeProfiles.contains("staging")
                || defaultProfiles.contains("prod") || defaultProfiles.contains("staging");
    }
}
