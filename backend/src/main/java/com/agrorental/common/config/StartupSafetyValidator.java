package com.agrorental.common.config;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Fails startup before any DataSource, Flyway or Hibernate bean is created when the configuration is unsafe.
 *
 * <ul>
 *   <li>An external (non-embedded) database requires an explicitly activated profile, so a MySQL
 *       deployment can never silently run on the implicit "dev" default (ddl-auto=update, Flyway off,
 *       development seeding).</li>
 *   <li>"dev" must never be active together with "prod" or "staging".</li>
 * </ul>
 * Runs as a BeanFactoryPostProcessor so the check happens before regular singletons are instantiated.
 */
@Component
public class StartupSafetyValidator implements BeanFactoryPostProcessor, EnvironmentAware {

    private static final List<String> EMBEDDED_URL_PREFIXES = List.of("jdbc:h2:", "jdbc:hsqldb:", "jdbc:derby:");

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        validate(environment);
    }

    static void validate(Environment environment) {
        if (environment == null) {
            return;
        }

        List<String> activeProfiles = Arrays.stream(environment.getActiveProfiles())
                .map(p -> p.trim().toLowerCase(Locale.ROOT))
                .toList();
        if (activeProfiles.contains(DeploymentProfiles.DEV)
                && (activeProfiles.contains(DeploymentProfiles.PROD) || activeProfiles.contains(DeploymentProfiles.STAGING))) {
            throw new IllegalStateException(
                    "CRITICAL CONFIGURATION ERROR: The 'dev' profile must not be active together with 'prod' or 'staging'. "
                            + "Active profiles: " + activeProfiles + ". Application startup aborted.");
        }

        String datasourceUrl;
        try {
            datasourceUrl = environment.getProperty("spring.datasource.url", "");
        } catch (IllegalArgumentException unresolvedPlaceholder) {
            // e.g. staging's mandatory ${SPRING_DATASOURCE_URL}; StagingConfigurationValidator reports it clearly
            return;
        }
        if (StringUtils.hasText(datasourceUrl) && !isEmbeddedDatabase(datasourceUrl)
                && !hasRecognizedExplicitProfile(environment)) {
            throw new IllegalStateException(
                    "CRITICAL CONFIGURATION ERROR: An external database is configured but no Spring profile was "
                            + "explicitly activated as exactly 'prod', 'staging' or 'dev' (active: "
                            + Arrays.toString(environment.getActiveProfiles()) + "), so only the implicit/base settings "
                            + "would apply (ddl-auto=update, Flyway disabled). Set SPRING_PROFILES_ACTIVE=prod (or staging) "
                            + "for deployments, or SPRING_PROFILES_ACTIVE=dev for intentional local development. "
                            + "Application startup aborted.");
        }
    }

    // Exact, case-sensitive names: Spring only loads application-{profile}.properties for the exact name
    private static boolean hasRecognizedExplicitProfile(Environment environment) {
        return Arrays.stream(environment.getActiveProfiles())
                .map(String::trim)
                .anyMatch(p -> p.equals(DeploymentProfiles.PROD) || p.equals(DeploymentProfiles.STAGING)
                        || p.equals(DeploymentProfiles.DEV));
    }

    private static boolean isEmbeddedDatabase(String datasourceUrl) {
        String normalized = datasourceUrl.trim().toLowerCase(Locale.ROOT);
        return EMBEDDED_URL_PREFIXES.stream().anyMatch(normalized::startsWith);
    }
}
