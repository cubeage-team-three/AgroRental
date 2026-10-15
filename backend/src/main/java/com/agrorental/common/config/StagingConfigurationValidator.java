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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Startup validator ensuring staging environment configuration is explicit, safe,
 * and fails fast on any misconfiguration, fallback, or insecure defaults.
 */
@Slf4j
@Getter
@Setter
@Component
public class StagingConfigurationValidator {

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:}")
    private String datasourceUsername;

    @Value("${spring.datasource.password:}")
    private String datasourcePassword;

    @Value("${spring.jpa.hibernate.ddl-auto:}")
    private String ddlAuto;

    @Value("${spring.flyway.enabled:false}")
    private boolean flywayEnabled;

    @Value("${agrorental.seed.mock-data.enabled:false}")
    private boolean mockDataEnabled;

    @Value("${app.otp.include-dev-mock:false}")
    private boolean includeDevMock;

    @Value("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleEnabled;

    @Autowired(required = false)
    private Environment environment;

    @PostConstruct
    public void validateStagingConfiguration() {
        if (!isStagingProfileActive()) {
            return;
        }

        List<String> activeProfiles = getActiveProfilesList();

        // 1. Enforce profile exclusivity: staging must never run simultaneously with dev or prod
        if (activeProfiles.contains("dev")) {
            throw new IllegalStateException(
                    "CRITICAL CONFIGURATION ERROR: Conflicting active profiles detected ('dev' and 'staging'). " +
                            "Staging must not run alongside the development profile."
            );
        }
        if (activeProfiles.contains("prod")) {
            throw new IllegalStateException(
                    "CRITICAL CONFIGURATION ERROR: Conflicting active profiles detected ('prod' and 'staging'). " +
                            "Staging must not run alongside the production profile."
            );
        }

        // 2. Reject missing, empty, unresolved placeholder, or non-MySQL datasource URLs; strictly forbid H2 and embedded DBs
        if (!StringUtils.hasText(datasourceUrl) || datasourceUrl.contains("${")) {
            throw new IllegalStateException(
                    "CRITICAL CONFIGURATION ERROR: Missing required datasource URL in staging environment " +
                            "('spring.datasource.url' or 'SPRING_DATASOURCE_URL'). Application startup aborted."
            );
        }
        String normalizedUrl = datasourceUrl.trim().toLowerCase();
        if (normalizedUrl.contains("h2") || normalizedUrl.startsWith("jdbc:h2:")) {
            throw new IllegalStateException(
                    "CRITICAL CONFIGURATION ERROR: H2 and embedded databases are strictly prohibited in staging. " +
                            "Invalid datasource URL: " + datasourceUrl
            );
        }
        if (!normalizedUrl.startsWith("jdbc:mysql://") && !normalizedUrl.startsWith("jdbc:mysql:")) {
            throw new IllegalStateException(
                    "CRITICAL CONFIGURATION ERROR: Staging datasource must be a MySQL database ('jdbc:mysql://...'). " +
                            "Invalid datasource URL: " + datasourceUrl
            );
        }

        // 3. Reject missing or unresolved datasource username
        if (!StringUtils.hasText(datasourceUsername) || datasourceUsername.contains("${")) {
            throw new IllegalStateException(
                    "CRITICAL CONFIGURATION ERROR: Missing required datasource username in staging environment " +
                            "('spring.datasource.username' or 'SPRING_DATASOURCE_USERNAME'). Application startup aborted."
            );
        }

        // 4. Reject missing or unresolved datasource password
        if (datasourcePassword == null || datasourcePassword.contains("${")) {
            throw new IllegalStateException(
                    "CRITICAL CONFIGURATION ERROR: Missing required datasource password in staging environment " +
                            "('spring.datasource.password' or 'SPRING_DATASOURCE_PASSWORD'). Application startup aborted."
            );
        }

        // 5. Enforce ddl-auto=validate (reject update, create, create-drop, none)
        if (!"validate".equalsIgnoreCase(ddlAuto != null ? ddlAuto.trim() : "")) {
            throw new IllegalStateException(
                    "CRITICAL CONFIGURATION ERROR: 'spring.jpa.hibernate.ddl-auto' must be 'validate' in staging. " +
                            "Current value: '" + ddlAuto + "'. Automatic schema modification is strictly prohibited."
            );
        }

        // 6. Enforce Flyway enabled
        if (!flywayEnabled) {
            throw new IllegalStateException(
                    "CRITICAL CONFIGURATION ERROR: Flyway database migrations must be enabled ('spring.flyway.enabled=true') in staging."
            );
        }

        // 7. Enforce mock-data seeding disabled
        if (mockDataEnabled) {
            throw new IllegalStateException(
                    "CRITICAL CONFIGURATION ERROR: Mock-data seeding ('agrorental.seed.mock-data.enabled') must be disabled in staging."
            );
        }

        // 8. Enforce mock OTP disclosure disabled
        if (includeDevMock) {
            throw new IllegalStateException(
                    "CRITICAL CONFIGURATION ERROR: Mock OTP disclosure ('app.otp.include-dev-mock') must be disabled in staging."
            );
        }

        // 9. Enforce H2 console disabled
        if (h2ConsoleEnabled) {
            throw new IllegalStateException(
                    "CRITICAL CONFIGURATION ERROR: H2 web console ('spring.h2.console.enabled') must be disabled in staging."
            );
        }

        log.info("Staging configuration validated successfully: MySQL datasource enforced, DDL-auto validate, Flyway enabled, mock seeding and dev OTP responses disabled.");
    }

    private boolean isStagingProfileActive() {
        if (environment == null) {
            return false;
        }
        List<String> activeProfiles = getActiveProfilesList();
        String[] defaultProfilesArr = environment.getDefaultProfiles();
        List<String> defaultProfiles = defaultProfilesArr != null ? Arrays.asList(defaultProfilesArr) : Collections.emptyList();

        return activeProfiles.contains("staging") || defaultProfiles.contains("staging");
    }

    private List<String> getActiveProfilesList() {
        if (environment == null || environment.getActiveProfiles() == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(environment.getActiveProfiles());
    }
}
