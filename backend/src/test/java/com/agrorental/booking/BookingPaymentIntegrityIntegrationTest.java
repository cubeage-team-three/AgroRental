package com.agrorental.booking;

import org.junit.jupiter.api.DisplayName;
import org.springframework.test.context.TestPropertySource;

/**
 * Runs the BIZ-13/14/20 integrity scenarios on H2 with real threads. H2 validates the application logic and
 * lock usage, but it does not prove MySQL/InnoDB locking semantics; see {@link BookingPaymentIntegrityMySQLTest}.
 */
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:booking_payment_integrity_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "agrorental.seed.mock-data.enabled=false",
    "app.jwt.secret=test-secret-key-32-chars-minimum-length-for-hmac-sha256",
    "server.port=0"
})
@DisplayName("BIZ-13/14/20: Booking and payment integrity (H2)")
class BookingPaymentIntegrityIntegrationTest extends AbstractBookingPaymentIntegrityTest {
}
