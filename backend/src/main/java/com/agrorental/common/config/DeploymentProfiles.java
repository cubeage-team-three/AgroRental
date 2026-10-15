package com.agrorental.common.config;

import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Central profile checks for development-only conveniences (OTP disclosure, mock seeding, H2 console).
 *
 * <p>{@code spring.profiles.default=dev} means a deployment started without any profile still loads the
 * dev property file. Development conveniences therefore require the {@code dev} profile to be
 * <em>explicitly activated</em> (e.g. {@code --spring.profiles.active=dev}); an implicit default never
 * enables them, and {@code prod} or {@code staging} always disables them.
 */
public final class DeploymentProfiles {

    public static final String DEV = "dev";
    public static final String PROD = "prod";
    public static final String STAGING = "staging";

    private DeploymentProfiles() {
    }

    /** True when "prod" or "staging" is active or configured as a default profile. */
    public static boolean isProductionOrStaging(Environment environment) {
        if (environment == null) {
            return false;
        }
        List<String> profiles = profiles(environment.getActiveProfiles());
        List<String> defaults = profiles(environment.getDefaultProfiles());
        return profiles.contains(PROD) || profiles.contains(STAGING)
                || defaults.contains(PROD) || defaults.contains(STAGING);
    }

    /**
     * True only when "dev" (exact, case-sensitive like Spring's profile files) is explicitly active and
     * neither "prod" nor "staging" is present in any letter case.
     */
    public static boolean isExplicitDevelopment(Environment environment) {
        if (environment == null || isProductionOrStaging(environment)) {
            return false;
        }
        String[] active = environment.getActiveProfiles();
        return active != null && Arrays.stream(active).map(String::trim).anyMatch(DEV::equals);
    }

    /** The H2 web console is served only in explicit development with the console property enabled. */
    public static boolean isH2ConsoleEnabled(Environment environment) {
        return isExplicitDevelopment(environment)
                && environment.getProperty("spring.h2.console.enabled", Boolean.class, false);
    }

    private static List<String> profiles(String[] profiles) {
        if (profiles == null) {
            return List.of();
        }
        return Arrays.stream(profiles).map(p -> p.trim().toLowerCase(Locale.ROOT)).toList();
    }
}
