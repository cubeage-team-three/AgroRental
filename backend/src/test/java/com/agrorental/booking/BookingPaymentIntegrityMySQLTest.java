package com.agrorental.booking;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.net.Socket;

/**
 * Runs the same BIZ-13/14/20 scenarios against a disposable MySQL 8 on localhost:33306, the only setup that
 * verifies InnoDB row locking (SELECT ... FOR UPDATE) under READ COMMITTED. Skipped when that server is absent.
 * Uses its own throwaway schema; every test creates isolated fixtures and nothing is deleted.
 */
@EnabledIf("isDisposableMysqlRunning")
// Intentional local MySQL run: the profile must be explicit (StartupSafetyValidator)
@ActiveProfiles("dev")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:mysql://127.0.0.1:33306/agrorental_booking_integrity_test?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true",
    "spring.datasource.username=root",
    "spring.datasource.password=root",
    "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
    "spring.datasource.hikari.maximum-pool-size=20",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "agrorental.seed.mock-data.enabled=false",
    "app.jwt.secret=test-secret-key-32-chars-minimum-length-for-hmac-sha256",
    "server.port=0"
})
@DisplayName("BIZ-13/14/20: Booking and payment integrity (disposable MySQL 8)")
class BookingPaymentIntegrityMySQLTest extends AbstractBookingPaymentIntegrityTest {

    static boolean isDisposableMysqlRunning() {
        try (Socket socket = new Socket("127.0.0.1", 33306)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
