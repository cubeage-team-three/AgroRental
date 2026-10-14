package com.agrorental.common.config;

import com.agrorental.booking.repository.BookingRepository;
import com.agrorental.equipment.repository.EquipmentRepository;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.operator.otp.OperatorOtpService;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.partner.repository.PartnerRepository;
import com.agrorental.security.jwt.JwtProperties;
import com.agrorental.user.repository.UserRepository;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke test verifying complete application startup in the staging profile
 * against an isolated disposable MySQL 8 instance.
 */
@SpringBootTest
@ActiveProfiles("staging")
@EnabledIf("isDisposableMysqlRunning")
@TestPropertySource(properties = {
    "SPRING_DATASOURCE_URL=jdbc:mysql://localhost:33306/agro_rental_staging_smoke?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
    "SPRING_DATASOURCE_USERNAME=root",
    "SPRING_DATASOURCE_PASSWORD=root",
    "JWT_SECRET=staging-smoke-test-secret-key-must-be-at-least-256-bits-for-hmac-sha256",
    "APP_CORS_ALLOWED_ORIGINS=https://staging.agrorent.in,https://app-staging.agrorent.in",
    "server.port=0"
})
@DisplayName("Priority 5.2.5.1: Disposable-MySQL Staging Startup Smoke Test")
class StagingStartupSmokeTest {

    static boolean isDisposableMysqlRunning() {
        try (Socket socket = new Socket("localhost", 33306)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Autowired
    private Environment environment;

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private FarmerRepository farmerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OperatorRepository operatorRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private OperatorOtpService operatorOtpService;

    @Autowired
    private CorsProperties corsProperties;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private StagingConfigurationValidator stagingConfigurationValidator;

    @Test
    @DisplayName("Verify staging profile exclusivity: 'staging' is active, 'dev' and 'prod' are NOT active")
    void verifyStagingProfileExclusivity() {
        List<String> activeProfiles = Arrays.asList(environment.getActiveProfiles());
        assertTrue(activeProfiles.contains("staging"), "Staging profile must be active");
        assertFalse(activeProfiles.contains("dev"), "Dev profile must NOT be active");
        assertFalse(activeProfiles.contains("prod"), "Prod profile must NOT be active");
    }

    @Test
    @DisplayName("Verify datasource resolves to MySQL and H2 fallback is not present")
    void verifyDatasourceConfiguration() {
        String datasourceUrl = environment.getProperty("spring.datasource.url");
        assertNotNull(datasourceUrl, "Datasource URL must be resolved");
        assertTrue(datasourceUrl.startsWith("jdbc:mysql://"), "Datasource URL must be MySQL");
        assertFalse(datasourceUrl.toLowerCase().contains("h2"), "Datasource must not use H2");

        String driverClass = environment.getProperty("spring.datasource.driver-class-name");
        assertEquals("com.mysql.cj.jdbc.Driver", driverClass);

        assertEquals("false", environment.getProperty("spring.h2.console.enabled"));
    }

    @Test
    @DisplayName("Verify Flyway migrations V1 and V1.1 applied successfully on disposable MySQL")
    void verifyFlywayMigrationsApplied() {
        assertNotNull(flyway, "Flyway bean must be present");

        MigrationInfo current = flyway.info().current();
        assertNotNull(current, "Current migration must exist");
        assertEquals("1.1", current.getVersion().getVersion(), "Current migration version must be 1.1");
        assertTrue(current.getState().isApplied(), "Migration 1.1 must be applied");

        MigrationInfo[] applied = flyway.info().applied();
        assertEquals(2, applied.length, "Exactly 2 migrations (V1 and V1.1) must be applied");
        assertEquals("1", applied[0].getVersion().getVersion());
        assertEquals("1.1", applied[1].getVersion().getVersion());

        // Verify exactly 23 entity tables plus flyway_schema_history exist
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'agro_rental_staging_smoke'",
                String.class
        );
        assertTrue(tables.contains("flyway_schema_history"));
        assertEquals(24, tables.size(), "Database must contain 23 domain tables plus flyway_schema_history");

        // Verify uk_payments_booking_id unique constraint exists
        List<String> constraints = jdbcTemplate.queryForList(
                "SELECT constraint_name FROM information_schema.table_constraints " +
                        "WHERE table_schema = 'agro_rental_staging_smoke' AND table_name = 'payments' AND constraint_name = 'uk_payments_booking_id'",
                String.class
        );
        assertEquals(1, constraints.size(), "uk_payments_booking_id unique constraint must exist");
    }

    @Test
    @DisplayName("Verify Hibernate ddl-auto=validate completed schema validation successfully")
    void verifyHibernateDdlAutoValidate() {
        assertEquals("validate", environment.getProperty("spring.jpa.hibernate.ddl-auto"));
        assertEquals("true", environment.getProperty("spring.flyway.enabled"));
        assertNotNull(stagingConfigurationValidator);
    }

    @Test
    @DisplayName("Verify mock-data seeding is strictly disabled and zero records exist in empty database")
    void verifyMockDataSeedingDisabled() {
        assertEquals("false", environment.getProperty("agrorental.seed.mock-data.enabled"));
        assertEquals(0, partnerRepository.count(), "No mock partners should be seeded in staging");
        assertEquals(0, farmerRepository.count(), "No mock farmers should be seeded in staging");
        assertEquals(0, userRepository.count(), "No mock users should be seeded in staging");
        assertEquals(0, operatorRepository.count(), "No mock operators should be seeded in staging");
        assertEquals(0, equipmentRepository.count(), "No mock equipment should be seeded in staging");
        assertEquals(0, bookingRepository.count(), "No mock bookings should be seeded in staging");
    }

    @Test
    @DisplayName("Verify Operator OTP development responses are disabled in staging")
    void verifyOperatorOtpDevelopmentResponsesDisabled() {
        assertEquals("false", environment.getProperty("app.otp.include-dev-mock"));
        assertFalse(operatorOtpService.isDevMockOtpAllowed(), "Dev mock OTP disclosure must be disabled in staging");
    }

    @Test
    @DisplayName("Verify CORS allowed origins strictly enforce HTTPS and reject wildcards/localhost")
    void verifyCorsAllowedOrigins() {
        List<String> allowedOrigins = corsProperties.getAllowedOrigins();
        assertFalse(allowedOrigins.isEmpty(), "Allowed origins must not be empty");
        for (String origin : allowedOrigins) {
            assertTrue(origin.startsWith("https://"), "Origins must be HTTPS: " + origin);
            assertFalse(origin.contains("*"), "Origins must not contain wildcard: " + origin);
            assertFalse(origin.contains("localhost"), "Origins must not contain localhost: " + origin);
        }
    }

    @Test
    @DisplayName("Verify JWT signing secret is externally supplied and meets 256-bit security requirement")
    void verifyJwtSecretSafety() {
        String secret = jwtProperties.getSecret();
        assertNotNull(secret);
        assertNotEquals(JwtProperties.DEFAULT_DEV_SECRET, secret, "Must not use default dev secret");
        assertTrue(secret.length() >= 32, "Secret must be at least 32 characters (256 bits)");
    }
}
