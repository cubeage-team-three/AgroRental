package com.agrorental.booking.workflow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * The concurrency scenarios on a disposable MySQL 8 (InnoDB row locks, READ COMMITTED). Opt-in only: runs when
 * the system property {@code agrorent.workflow.mysql.url} names a {@code jdbc:mysql://127.0.0.1:<port>/<db>} URL
 * (never port 33306, which is reserved for the shared MySQL-gated suites); credentials come from
 * {@code agrorent.workflow.mysql.user} / {@code agrorent.workflow.mysql.password}. Example:
 * {@code mvn test -Dtest=BookingWorkflowConcurrencyMySQLTest -Dagrorent.workflow.mysql.url=jdbc:mysql://127.0.0.1:33412/wb_booking?createDatabaseIfNotExist=true}
 */
@EnabledIf("isDisposableMysqlRequested")
// Intentional local MySQL run: the profile must be explicit (StartupSafetyValidator)
@ActiveProfiles("dev")
@DisplayName("Booking workflow 4b: duplicate and concurrent transitions (disposable MySQL 8, opt-in)")
class BookingWorkflowConcurrencyMySQLTest extends AbstractBookingWorkflowConcurrencyTest {

    private static final String URL_PROPERTY = "agrorent.workflow.mysql.url";

    static boolean isDisposableMysqlRequested() {
        String url = System.getProperty(URL_PROPERTY, "");
        return url.startsWith("jdbc:mysql://127.0.0.1:") && !url.startsWith("jdbc:mysql://127.0.0.1:33306");
    }

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        if (!isDisposableMysqlRequested()) {
            return;
        }
        registry.add("spring.datasource.url", () -> System.getProperty(URL_PROPERTY));
        registry.add("spring.datasource.username", () -> System.getProperty("agrorent.workflow.mysql.user", "root"));
        registry.add("spring.datasource.password", () -> System.getProperty("agrorent.workflow.mysql.password", "root"));
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "20");
    }
}
