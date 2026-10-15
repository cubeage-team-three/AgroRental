package com.agrorental.booking.workflow;

import org.junit.jupiter.api.DisplayName;

/**
 * Concurrency scenarios on H2 (application logic and lock usage). H2 does not prove InnoDB locking semantics;
 * {@link BookingWorkflowConcurrencyMySQLTest} runs the same scenarios on a disposable MySQL 8 when requested.
 */
@DisplayName("Booking workflow 4b: duplicate and concurrent transitions (H2)")
class BookingWorkflowConcurrencyTest extends AbstractBookingWorkflowConcurrencyTest {
}
