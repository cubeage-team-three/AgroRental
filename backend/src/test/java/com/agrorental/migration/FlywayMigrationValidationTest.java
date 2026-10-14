package com.agrorental.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@EnabledIf("isDisposableMysqlRunning")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:mysql://localhost:33306/agro_rental_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
    "spring.datasource.username=root",
    "spring.datasource.password=root",
    "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
    "spring.flyway.enabled=true",
    "spring.flyway.locations=classpath:db/migration",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect",
    "agrorental.seed.mock-data.enabled=false",
    "app.jwt.secret=test-secret-key-32-chars-minimum-length-for-hmac-sha256"
})
class FlywayMigrationValidationTest {

    static boolean isDisposableMysqlRunning() {
        try (Socket socket = new Socket("localhost", 33306)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Autowired
    private ApplicationContext context;

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testFlywayMigrationAndHibernateValidationAgainstEmptyMysql() {
        assertNotNull(context, "Application context must load cleanly");
        assertNotNull(flyway, "Flyway bean must be present and executed");

        MigrationInfo currentMigration = flyway.info().current();
        assertNotNull(currentMigration, "A current migration must be registered in Flyway");
        assertEquals("1.1", currentMigration.getVersion().getVersion(), "Migration version should be 1.1");
        assertEquals("add unique constraint payments booking id", currentMigration.getDescription(), "Migration description should match");
        assertTrue(currentMigration.getState().isApplied(), "Migration should be in APPLIED state");

        MigrationInfo[] appliedMigrations = flyway.info().applied();
        assertEquals(2, appliedMigrations.length, "Exactly 2 migrations (V1 and V1.1) should be applied");

        // Verify that all 23 domain tables + flyway_schema_history exist
        List<String> tables = jdbcTemplate.queryForList(
            "SELECT table_name FROM information_schema.tables WHERE table_schema = 'agro_rental_test'",
            String.class
        );

        assertTrue(tables.contains("flyway_schema_history"), "flyway_schema_history must exist");

        List<String> expectedTables = List.of(
            "admins", "bookings", "complaints", "email_verification_tokens",
            "equipment", "equipment_images", "farmer_otp", "farmers", "farms",
            "live_tracking", "notifications", "operator_documents", "operator_job_assignments",
            "operator_job_pause_intervals", "operator_locations", "operator_otps",
            "operator_reviews", "operators", "partners", "password_reset_tokens",
            "payments", "reviews", "users"
        );

        for (String expectedTable : expectedTables) {
            assertTrue(
                tables.contains(expectedTable),
                "Table " + expectedTable + " must exist in database"
            );
        }

        assertEquals(24, tables.size(), "Database must contain exactly 23 entity tables plus flyway_schema_history");

        // Verify unique constraint uk_payments_booking_id exists in payments table
        List<String> constraints = jdbcTemplate.queryForList(
            "SELECT constraint_name FROM information_schema.table_constraints " +
            "WHERE table_schema = 'agro_rental_test' AND table_name = 'payments' AND constraint_name = 'uk_payments_booking_id'",
            String.class
        );
        assertEquals(1, constraints.size(), "uk_payments_booking_id unique constraint must exist on payments table");
    }

    @Test
    void testUniqueConstraintRejectsDuplicatePaymentForSameBooking() {
        Long testBookingId = 88881L;
        // Clean up any test records
        jdbcTemplate.update("DELETE FROM payments WHERE booking_id = ?", testBookingId);

        // First insert succeeds
        jdbcTemplate.update(
            "INSERT INTO payments (created_at, updated_at, is_active, version, booking_id, farmer_id, amount, payment_method, payment_status, payment_date, transaction_id, invoice_reference) " +
            "VALUES (NOW(6), NOW(6), 1, 0, ?, 1, 1500.00, 'UPI', 'SUCCESS', NOW(6), 'TXN-TEST-1', 'INV-TEST-1')",
            testBookingId
        );

        Integer countAfterFirst = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM payments WHERE booking_id = ?", Integer.class, testBookingId);
        assertEquals(1, countAfterFirst);

        // Second insert with SAME booking_id MUST fail with DataIntegrityViolationException
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> {
            jdbcTemplate.update(
                "INSERT INTO payments (created_at, updated_at, is_active, version, booking_id, farmer_id, amount, payment_method, payment_status, payment_date, transaction_id, invoice_reference) " +
                "VALUES (NOW(6), NOW(6), 1, 0, ?, 1, 1500.00, 'UPI', 'SUCCESS', NOW(6), 'TXN-TEST-2', 'INV-TEST-2')",
                testBookingId
            );
        });

        // Verify the original record remains completely intact and unaltered
        Integer countAfterFailedSecond = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM payments WHERE booking_id = ?", Integer.class, testBookingId);
        assertEquals(1, countAfterFailedSecond, "Original payment must remain intact without alteration");

        String originalTxn = jdbcTemplate.queryForObject(
            "SELECT transaction_id FROM payments WHERE booking_id = ?", String.class, testBookingId);
        assertEquals("TXN-TEST-1", originalTxn, "Original transaction record must be preserved");

        // Clean up
        jdbcTemplate.update("DELETE FROM payments WHERE booking_id = ?", testBookingId);
    }

    @Test
    void testConstraintAdditionFailsOnDuplicateDataWithoutAlteringRecords() {
        // Create an isolated test table simulating pre-existing schema without unique constraint
        jdbcTemplate.execute("DROP TABLE IF EXISTS test_payments_dup_check");
        jdbcTemplate.execute(
            "CREATE TABLE test_payments_dup_check (" +
            "  id BIGINT NOT NULL AUTO_INCREMENT, " +
            "  booking_id BIGINT NOT NULL, " +
            "  amount DECIMAL(10,2) NOT NULL, " +
            "  PRIMARY KEY (id)" +
            ") ENGINE=InnoDB"
        );

        // Insert duplicate rows
        jdbcTemplate.update("INSERT INTO test_payments_dup_check (booking_id, amount) VALUES (555, 1000.00)");
        jdbcTemplate.update("INSERT INTO test_payments_dup_check (booking_id, amount) VALUES (555, 1000.00)");

        Integer countBefore = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM test_payments_dup_check WHERE booking_id = 555", Integer.class);
        assertEquals(2, countBefore, "Table contains 2 duplicate records");

        // Attempting to apply ALTER TABLE ... ADD CONSTRAINT UNIQUE must fail with DataIntegrityViolationException
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> {
            jdbcTemplate.execute("ALTER TABLE test_payments_dup_check ADD CONSTRAINT uk_test_booking_id UNIQUE (booking_id)");
        });

        // Verify that the operation failed safely without silently deleting, altering, or dropping any records
        Integer countAfter = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM test_payments_dup_check WHERE booking_id = 555", Integer.class);
        assertEquals(2, countAfter, "Both records must remain completely intact; no silent deletion or data alteration");

        // Clean up
        jdbcTemplate.execute("DROP TABLE IF EXISTS test_payments_dup_check");
    }
}
