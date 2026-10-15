package com.agrorental.admin.config;

import com.agrorental.admin.entity.Admin;
import com.agrorental.admin.repository.AdminRepository;
import com.agrorental.common.config.DeploymentProfiles;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Seeds an initial admin account only when explicitly configured.
 * Excluded from the "prod" profile. There is no fallback password: unless AGRORENTAL_ADMIN_PASSWORD
 * (agrorental.admin.seed.password) is set, no admin is seeded in any profile. The formerly committed
 * default password is rejected everywhere except an explicitly active "dev" profile.
 */
@Slf4j
@Component
@Profile("!prod")
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    public static final String DEFAULT_ADMIN_EMAIL = "agrorent@admin.in";

    /** Previously committed default password (public in history); never accepted outside explicit dev. */
    public static final String KNOWN_INSECURE_ADMIN_PASSWORD = "agrorent21";

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Value("${agrorental.admin.seed.email:}")
    private String configuredAdminEmail;

    @Value("${agrorental.admin.seed.password:}")
    private String configuredAdminPassword;

    @Override
    public void run(String... args) {
        if (!StringUtils.hasText(configuredAdminPassword)) {
            log.warn("AdminSeeder: No admin seed password configured (AGRORENTAL_ADMIN_PASSWORD). "
                    + "Skipping admin seeding; no default credentials are used.");
            return;
        }

        String password = configuredAdminPassword.trim();
        if (KNOWN_INSECURE_ADMIN_PASSWORD.equals(password) && !DeploymentProfiles.isExplicitDevelopment(environment)) {
            log.warn("AdminSeeder: The configured admin password is a known, publicly committed default. "
                    + "It is only accepted with an explicitly active 'dev' profile. Skipping admin seeding.");
            return;
        }

        String email = StringUtils.hasText(configuredAdminEmail)
                ? configuredAdminEmail.trim()
                : DEFAULT_ADMIN_EMAIL;

        if (adminRepository.existsByEmail(email)) {
            log.info("Admin account ({}) already exists. Skipping AdminSeeder.", email);
            return;
        }

        Admin admin = Admin.builder()
                .fullName("AgroRent Administrator")
                .email(email)
                .password(passwordEncoder.encode(password))
                .build();

        adminRepository.save(admin);
        log.info("Seeded initial admin account: {}", email);
    }
}
