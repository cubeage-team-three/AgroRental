package com.agrorental.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@EnabledIf("isDisposableMysqlRunning")
// Intentional local MySQL run: the profile must be explicit (StartupSafetyValidator); same properties as the implicit dev default
@ActiveProfiles("dev")
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
        assertEquals("3", currentMigration.getVersion().getVersion(), "Migration version should be 3");
        assertEquals("account password reset tokens", currentMigration.getDescription(), "Migration description should match");
        assertTrue(currentMigration.getState().isApplied(), "Migration should be in APPLIED state");

        MigrationInfo[] appliedMigrations = flyway.info().applied();
        assertEquals(4, appliedMigrations.length, "Exactly 4 migrations (V1, V1.1, V2 and V3) should be applied");

        // Verify that all 25 domain tables + flyway_schema_history exist
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
            "payments", "reviews", "users",
            // V2 (BIZ-20)
            "payment_refunds", "payment_refund_events",
            // V3 (password reset tokens, hashed)
            "account_password_reset_tokens"
        );

        for (String expectedTable : expectedTables) {
            assertTrue(
                tables.contains(expectedTable),
                "Table " + expectedTable + " must exist in database"
            );
        }

        assertEquals(27, tables.size(), "Database must contain exactly 26 entity tables plus flyway_schema_history");

        // Verify unique constraint uk_payments_booking_id exists in payments table
        List<String> constraints = jdbcTemplate.queryForList(
            "SELECT constraint_name FROM information_schema.table_constraints " +
            "WHERE table_schema = 'agro_rental_test' AND table_name = 'payments' AND constraint_name = 'uk_payments_booking_id'",
            String.class
        );
        assertEquals(1, constraints.size(), "uk_payments_booking_id unique constraint must exist on payments table");

        // V2: one refund obligation per payment, one refund per transfer reference, refunds tied to real payments
        List<String> refundConstraints = jdbcTemplate.queryForList(
            "SELECT constraint_name FROM information_schema.table_constraints " +
            "WHERE table_schema = 'agro_rental_test' AND table_name = 'payment_refunds'",
            String.class
        );
        // V3: a reset-token hash can exist only once
        List<String> resetTokenConstraints = jdbcTemplate.queryForList(
            "SELECT constraint_name FROM information_schema.table_constraints " +
            "WHERE table_schema = 'agro_rental_test' AND table_name = 'account_password_reset_tokens'",
            String.class
        );
        assertTrue(resetTokenConstraints.contains("uk_account_password_reset_tokens_token_hash"),
            "account_password_reset_tokens must keep token hashes unique: " + resetTokenConstraints);
        assertTrue(refundConstraints.containsAll(List.of(
                "uk_payment_refunds_payment_id", "uk_payment_refunds_transfer_reference", "fk_payment_refunds_payment")),
            "payment_refunds constraints must exist: " + refundConstraints);
    }

    @Test
    void testRefundTableRejectsSecondObligationForSamePaymentAndDanglingPayment() {
        Long testBookingId = 88882L;
        jdbcTemplate.update("DELETE FROM payment_refunds WHERE booking_id = ?", testBookingId);
        jdbcTemplate.update("DELETE FROM payments WHERE booking_id = ?", testBookingId);
        jdbcTemplate.update(
            "INSERT INTO payments (created_at, updated_at, is_active, version, booking_id, farmer_id, amount, payment_method, payment_status, payment_date, transaction_id, invoice_reference) " +
            "VALUES (NOW(6), NOW(6), 1, 0, ?, 1, 1500.00, 'UPI', 'SUCCESS', NOW(6), 'TXN-TEST-R1', 'INV-TEST-R1')",
            testBookingId
        );
        Long paymentId = jdbcTemplate.queryForObject("SELECT id FROM payments WHERE booking_id = ?", Long.class, testBookingId);
        String insertRefund = "INSERT INTO payment_refunds (created_at, updated_at, is_active, version, payment_id, booking_id, farmer_id, amount, status, initiation_reason, initiated_at, initiated_by, attempt_count) " +
            "VALUES (NOW(6), NOW(6), 1, 0, ?, ?, 1, 1500.00, 'REFUND_PENDING', 'test', NOW(6), 'TEST', 1)";

        jdbcTemplate.update(insertRefund, paymentId, testBookingId);
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
            () -> jdbcTemplate.update(insertRefund, paymentId, testBookingId), "A second refund for the same payment must be rejected");
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class,
            () -> jdbcTemplate.update(insertRefund, 987654321L, testBookingId), "A refund must reference an existing payment");
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM payment_refunds WHERE booking_id = ?", Integer.class, testBookingId));
        // The original payment row is untouched by refund tracking
        assertEquals("SUCCESS", jdbcTemplate.queryForObject("SELECT payment_status FROM payments WHERE id = ?", String.class, paymentId));

        jdbcTemplate.update("DELETE FROM payment_refunds WHERE booking_id = ?", testBookingId);
        jdbcTemplate.update("DELETE FROM payments WHERE booking_id = ?", testBookingId);
    }

    /**
     * Upgrade path: a database already at V1.1 holding payments is migrated to V2 in a separate disposable schema.
     * V2 is additive, so every existing payment row is preserved unchanged and has no refund row (refund status NONE).
     */
    @Test
    void testUpgradeFromV11WithExistingPaymentsPreservesDataAndCreatesNoRefunds() {
        String upgradeSchema = "agro_rental_upgrade_test";
        String url = "jdbc:mysql://localhost:33306/" + upgradeSchema
            + "?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        org.springframework.jdbc.datasource.DriverManagerDataSource ds =
            new org.springframework.jdbc.datasource.DriverManagerDataSource(url, "root", "root");
        JdbcTemplate upgradeJdbc = new JdbcTemplate(ds);
        try {
            Flyway v11 = Flyway.configure().dataSource(ds).locations("classpath:db/migration")
                .cleanDisabled(false).target("1.1").load();
            v11.clean();
            v11.migrate();
            assertEquals("1.1", v11.info().current().getVersion().getVersion());

            upgradeJdbc.update(
                "INSERT INTO payments (created_at, updated_at, is_active, version, booking_id, farmer_id, partner_id, amount, payment_method, payment_status, payment_date, transaction_id, invoice_reference) " +
                "VALUES (NOW(6), NOW(6), 1, 0, 501, 11, 21, 5000.00, 'UPI', 'SUCCESS', NOW(6), 'TXN-UPG-1', 'INV-UPG-1'), " +
                "       (NOW(6), NOW(6), 1, 0, 502, 12, 21, 7500.00, 'NET_BANKING', 'FAILED', NOW(6), 'TXN-UPG-2', 'INV-UPG-2')");
            List<java.util.Map<String, Object>> before = upgradeJdbc.queryForList(
                "SELECT id, booking_id, amount, payment_status, transaction_id FROM payments ORDER BY id");

            Flyway v2 = Flyway.configure().dataSource(ds).locations("classpath:db/migration").target("2").load();
            assertEquals(1, v2.migrate().migrationsExecuted, "Only V2 must run on the upgrade");
            assertEquals("2", v2.info().current().getVersion().getVersion());

            assertEquals(before, upgradeJdbc.queryForList(
                "SELECT id, booking_id, amount, payment_status, transaction_id FROM payments ORDER BY id"),
                "Existing payments must be preserved exactly");
            assertEquals(0, upgradeJdbc.queryForObject("SELECT COUNT(*) FROM payment_refunds", Integer.class),
                "No refunds are backfilled: existing payments report refund status NONE");
            assertEquals(0, upgradeJdbc.queryForObject("SELECT COUNT(*) FROM payment_refund_events", Integer.class));

            // ...and V2 -> V3 (hashed password-reset tokens) is additive too: only V3 runs, payments untouched
            Flyway v3 = Flyway.configure().dataSource(ds).locations("classpath:db/migration").load();
            assertEquals(1, v3.migrate().migrationsExecuted, "Only V3 must run on top of V2");
            assertEquals("3", v3.info().current().getVersion().getVersion());
            assertEquals(before, upgradeJdbc.queryForList(
                "SELECT id, booking_id, amount, payment_status, transaction_id FROM payments ORDER BY id"),
                "Existing payments must still be preserved exactly after V3");
            assertEquals(0, upgradeJdbc.queryForObject("SELECT COUNT(*) FROM account_password_reset_tokens", Integer.class),
                "No reset tokens are created by the migration");
        } finally {
            upgradeJdbc.execute("DROP DATABASE IF EXISTS " + upgradeSchema);
        }
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
