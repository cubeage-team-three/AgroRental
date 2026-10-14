package com.agrorental.admin.config;

import com.agrorental.admin.entity.Admin;
import com.agrorental.admin.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * Seeds an initial admin account for local/dev use or when explicitly configured.
 * Excluded from the "prod" profile so this known dev password is never created in production.
 * In the "staging" profile, hardcoded default credentials are strictly prohibited:
 * an initial admin will ONLY be seeded if AGRORENTAL_ADMIN_PASSWORD is explicitly set to
 * a secure, non-default value.
 */
@Slf4j
@Component
@Profile("!prod")
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    public static final String DEFAULT_ADMIN_EMAIL = "agrorent@admin.in";
    public static final String DEFAULT_ADMIN_PASSWORD = "agrorent21";

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Value("${agrorental.admin.seed.email:}")
    private String configuredAdminEmail;

    @Value("${agrorental.admin.seed.password:}")
    private String configuredAdminPassword;

    @Override
    public void run(String... args) {
        boolean isStaging = isStagingProfileActive();

        String email = StringUtils.hasText(configuredAdminEmail)
                ? configuredAdminEmail.trim()
                : DEFAULT_ADMIN_EMAIL;

        if (adminRepository.existsByEmail(email)) {
            log.info("Admin account ({}) already exists. Skipping AdminSeeder.", email);
            return;
        }

        if (isStaging) {
            // In staging, hardcoded default credentials are strictly prohibited
            if (!StringUtils.hasText(configuredAdminPassword) || DEFAULT_ADMIN_PASSWORD.equals(configuredAdminPassword.trim())) {
                log.warn("AdminSeeder: Hardcoded or missing admin password detected under 'staging' profile. " +
                        "Default admin seeding is disabled in staging to prevent hardcoded credentials. " +
                        "To seed an initial admin account in staging, set AGRORENTAL_ADMIN_PASSWORD to a secure non-default password.");
                return;
            }
        }

        String password = StringUtils.hasText(configuredAdminPassword)
                ? configuredAdminPassword.trim()
                : DEFAULT_ADMIN_PASSWORD;

        Admin admin = Admin.builder()
                .fullName("AgroRent Administrator")
                .email(email)
                .password(passwordEncoder.encode(password))
                .build();

        adminRepository.save(admin);
        log.info("Seeded initial admin account: {}", email);
    }

    private boolean isStagingProfileActive() {
        if (environment == null) {
            return false;
        }
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        return activeProfiles.contains("staging");
    }
}

