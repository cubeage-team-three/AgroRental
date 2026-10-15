package com.agrorental.common.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Configuration properties holder and validator for CORS allowed origins.
 * Guarantees that production and staging environments require explicit, valid HTTPS origins
 * and prevents wildcard or silent localhost fallbacks.
 */
@Slf4j
@Getter
@Setter
@Component
public class CorsProperties {

    private static final List<String> DEFAULT_DEV_ORIGINS = List.of(
            "http://localhost:5173",
            "http://localhost:5174",
            "http://localhost:3000",
            "http://127.0.0.1:5173",
            "http://127.0.0.1:5174"
    );

    @Value("${app.cors.allowed-origins:}")
    private String allowedOriginsRaw;

    @Autowired
    private Environment environment;

    private List<String> allowedOrigins = new ArrayList<>();

    @PostConstruct
    public void validateCorsConfiguration() {
        boolean isProdOrStaging = isProductionOrStagingEnvironment();
        List<String> parsedOrigins = parseOrigins(allowedOriginsRaw);

        if (isProdOrStaging) {
            if (parsedOrigins.isEmpty() || (allowedOriginsRaw != null && allowedOriginsRaw.contains("${"))) {
                throw new IllegalStateException(
                        "CRITICAL SECURITY ERROR: Allowed CORS origins ('app.cors.allowed-origins' or 'APP_CORS_ALLOWED_ORIGINS') " +
                                "must be explicitly configured in staging/production environment. Application startup aborted."
                );
            }

            for (String origin : parsedOrigins) {
                if (origin.equals("*") || origin.contains("*")) {
                    throw new IllegalStateException(
                            "CRITICAL SECURITY ERROR: Wildcard origin patterns ('*') are strictly prohibited in staging/production " +
                                    "environments when credentials are enabled. Invalid origin: " + origin
                    );
                }
                if (!origin.startsWith("https://")) {
                    throw new IllegalStateException(
                            "CRITICAL SECURITY ERROR: Staging and production frontend origins must strictly use HTTPS. " +
                                    "Insecure or localhost origin rejected: " + origin
                    );
                }
            }

            this.allowedOrigins = Collections.unmodifiableList(parsedOrigins);
            log.info("CORS successfully configured for secure production/staging origins: {}", this.allowedOrigins);
        } else {
            // Development / Local environment
            for (String origin : parsedOrigins) {
                if (origin.equals("*") || origin.contains("*")) {
                    throw new IllegalStateException(
                            "CRITICAL SECURITY ERROR: Wildcard origin pattern ('*') is not allowed when credentials are enabled. " +
                                    "Please specify explicit local development origins."
                    );
                }
            }

            if (parsedOrigins.isEmpty()) {
                this.allowedOrigins = Collections.unmodifiableList(DEFAULT_DEV_ORIGINS);
                log.info("CORS allowed origins defaulting to local development origins: {}", this.allowedOrigins);
            } else {
                this.allowedOrigins = Collections.unmodifiableList(parsedOrigins);
                log.info("CORS allowed origins configured for development: {}", this.allowedOrigins);
            }
        }
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

    private List<String> parseOrigins(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }
}
