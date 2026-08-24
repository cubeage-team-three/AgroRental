package com.agrorental.admin.config;

import com.agrorental.admin.entity.Admin;
import com.agrorental.admin.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds a default admin account for local/dev use. Excluded from the "prod"
 * profile so this known dev password is never created in a real deployment —
 * a production admin account must be provisioned separately with its own
 * credentials.
 */
@Slf4j
@Component
@Profile("!prod")
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private static final String DEFAULT_ADMIN_EMAIL = "agrorent@admin.in";
    private static final String DEFAULT_ADMIN_PASSWORD = "agrorent21";

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (adminRepository.existsByEmail(DEFAULT_ADMIN_EMAIL)) {
            log.info("Default admin account already exists. Skipping AdminSeeder.");
            return;
        }

        Admin admin = Admin.builder()
                .fullName("AgroRent Administrator")
                .email(DEFAULT_ADMIN_EMAIL)
                .password(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD))
                .build();

        adminRepository.save(admin);
        log.info("Seeded default admin account: {}", DEFAULT_ADMIN_EMAIL);
    }
}
