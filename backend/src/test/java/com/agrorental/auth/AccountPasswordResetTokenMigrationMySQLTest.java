package com.agrorental.auth;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Flyway V3 (account_password_reset_tokens) against a real, disposable MySQL 8 server: a fresh database migrates
 * V1 -> V1.1 -> V2 -> V3, and an existing V2 database holding data gets only the additive V3 with every row preserved.
 *
 * <p>Opt-in only, so it can never reach a database by accident: run with
 * {@code -Dagrorent.auth.mysql.server=jdbc:mysql://127.0.0.1:<port>} (and optionally
 * {@code -Dagrorent.auth.mysql.user / .password}, default root/root). It creates and finally drops the schemas
 * {@code agro_v3_fresh} and {@code agro_v3_upgrade} on that server.
 */
@EnabledIfSystemProperty(named = "agrorent.auth.mysql.server", matches = "jdbc:mysql://.+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Flyway V3 account_password_reset_tokens on disposable MySQL (opt-in)")
class AccountPasswordResetTokenMigrationMySQLTest {

    private static final String FRESH = "agro_v3_fresh";
    private static final String UPGRADE = "agro_v3_upgrade";

    private DriverManagerDataSource dataSource(String schema) {
        String server = System.getProperty("agrorent.auth.mysql.server");
        return new DriverManagerDataSource(server + "/" + schema
                + "?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                System.getProperty("agrorent.auth.mysql.user", "root"),
                System.getProperty("agrorent.auth.mysql.password", "root"));
    }

    private Flyway flyway(DriverManagerDataSource dataSource, String target) {
        var configuration = Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").cleanDisabled(false);
        if (target != null) {
            configuration.target(target);
        }
        return configuration.load();
    }

    @AfterAll
    void dropSchemas() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource(FRESH));
        jdbc.execute("DROP DATABASE IF EXISTS " + FRESH);
        jdbc.execute("DROP DATABASE IF EXISTS " + UPGRADE);
    }

    @Test
    @DisplayName("Fresh database: V1 -> V1.1 -> V2 -> V3, with the hashed-token table, unique hash and account index")
    void freshDatabaseMigratesToV3() {
        DriverManagerDataSource ds = dataSource(FRESH);
        Flyway flyway = flyway(ds, null);
        flyway.clean();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(4);
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("3");
        assertThat(flyway.info().current().getDescription()).isEqualTo("account password reset tokens");

        JdbcTemplate jdbc = new JdbcTemplate(ds);
        List<Map<String, Object>> columns = jdbc.queryForList(
                "SELECT column_name, data_type, is_nullable, character_maximum_length FROM information_schema.columns "
                        + "WHERE table_schema = ? AND table_name = 'account_password_reset_tokens' ORDER BY ordinal_position", FRESH);
        assertThat(columns).extracting(row -> String.valueOf(row.get("COLUMN_NAME")).toLowerCase())
                .containsExactly("id", "account_type", "account_id", "token_hash", "expires_at", "used_at", "created_at");
        assertThat(columns).filteredOn(row -> "used_at".equalsIgnoreCase(String.valueOf(row.get("COLUMN_NAME"))))
                .allMatch(row -> "YES".equals(row.get("IS_NULLABLE")));

        List<String> constraints = jdbc.queryForList("SELECT constraint_name FROM information_schema.table_constraints "
                + "WHERE table_schema = ? AND table_name = 'account_password_reset_tokens'", String.class, FRESH);
        assertThat(constraints).contains("uk_account_password_reset_tokens_token_hash");
        List<String> indexes = jdbc.queryForList("SELECT DISTINCT index_name FROM information_schema.statistics "
                + "WHERE table_schema = ? AND table_name = 'account_password_reset_tokens'", String.class, FRESH);
        assertThat(indexes).contains("idx_account_password_reset_tokens_account");

        String insert = "INSERT INTO account_password_reset_tokens (account_type, account_id, token_hash, expires_at, created_at) "
                + "VALUES (?, 1, ?, NOW(6) + INTERVAL 30 MINUTE, NOW(6))";
        jdbc.update(insert, "FARMER", "a".repeat(64));
        assertThatThrownBy(() -> jdbc.update(insert, "PARTNER", "a".repeat(64)))
                .as("token hashes are unique").isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(insert, "ADMIN", "b".repeat(64)))
                .as("admins have no self-service reset tokens (enum rejects the value)")
                .isInstanceOf(org.springframework.dao.DataAccessException.class);
    }

    @Test
    @DisplayName("Existing V2 database with data: only V3 runs, every existing row is preserved and no token is created")
    void upgradeFromV2PreservesData() {
        DriverManagerDataSource ds = dataSource(UPGRADE);
        Flyway v2 = flyway(ds, "2");
        v2.clean();
        v2.migrate();
        assertThat(v2.info().current().getVersion().getVersion()).isEqualTo("2");

        JdbcTemplate jdbc = new JdbcTemplate(ds);
        jdbc.update("INSERT INTO users (created_at, updated_at, is_active, version, name, email, password, role, enabled, verified) "
                + "VALUES (NOW(6), NOW(6), 1, 0, 'Legacy Farmer', 'legacy@upgrade.test', '$2a$10$abcdefghijklmnopqrstuuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ012', 'FARMER', 1, 0)");
        Long userId = jdbc.queryForObject("SELECT id FROM users WHERE email = 'legacy@upgrade.test'", Long.class);
        jdbc.update("INSERT INTO farmers (created_at, updated_at, is_active, version, user_id, full_name, email, mobile_number, password, account_status, preferred_language) "
                + "VALUES (NOW(6), NOW(6), 1, 0, ?, 'Legacy Farmer', 'legacy@upgrade.test', '9890000001', NULL, 'PENDING_OTP', 'English')", userId);
        jdbc.update("INSERT INTO partners (created_at, updated_at, is_active, version, full_name, mobile_number, email, password, verification_status, otp_code, otp_expiry, otp_verified) "
                + "VALUES (NOW(6), NOW(6), 1, 0, 'Legacy Partner', '9690000001', 'partner@upgrade.test', 'x', 'PENDING', '123456', NOW(6), 0)");
        jdbc.update("INSERT INTO operators (created_at, updated_at, is_active, version, full_name, mobile_number, email, password, experience, skills, address, aadhaar_number, driving_license_number, status, mobile_verified) "
                + "VALUES (NOW(6), NOW(6), 1, 0, 'Legacy Operator', '9790000001', 'op@upgrade.test', 'x', 3, 'Tractor', 'Pune', '123412341234', 'MH12-UPG', 'PENDING', 0)");
        jdbc.update("INSERT INTO password_reset_tokens (created_at, updated_at, is_active, version, token, user_id, expiry_date, used) "
                + "VALUES (NOW(6), NOW(6), 1, 0, 'legacy-plaintext-token', ?, NOW(6), 0)", userId);

        String snapshotSql = "SELECT 'users' t, COUNT(*) c FROM users UNION ALL SELECT 'farmers', COUNT(*) FROM farmers "
                + "UNION ALL SELECT 'partners', COUNT(*) FROM partners UNION ALL SELECT 'operators', COUNT(*) FROM operators "
                + "UNION ALL SELECT 'password_reset_tokens', COUNT(*) FROM password_reset_tokens";
        List<Map<String, Object>> before = jdbc.queryForList(snapshotSql);
        List<Map<String, Object>> farmersBefore = jdbc.queryForList("SELECT * FROM farmers ORDER BY id");
        List<Map<String, Object>> partnersBefore = jdbc.queryForList("SELECT * FROM partners ORDER BY id");

        Flyway latest = flyway(ds, null);
        assertThat(latest.migrate().migrationsExecuted).as("only V3 runs on a V2 database").isEqualTo(1);
        assertThat(latest.info().current().getVersion().getVersion()).isEqualTo("3");

        assertThat(jdbc.queryForList(snapshotSql)).isEqualTo(before);
        assertThat(jdbc.queryForList("SELECT * FROM farmers ORDER BY id")).isEqualTo(farmersBefore);
        assertThat(jdbc.queryForList("SELECT * FROM partners ORDER BY id")).isEqualTo(partnersBefore);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM account_password_reset_tokens", Integer.class)).isZero();
    }
}
