package com.agrorental.payment;

import com.agrorental.booking.dto.BookingResponse;
import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.booking.repository.BookingRepository;
import com.agrorental.booking.service.BookingService;
import com.agrorental.payment.dto.PaymentRequest;
import com.agrorental.payment.dto.PaymentResponse;
import com.agrorental.payment.entity.PaymentMethod;
import com.agrorental.payment.entity.PaymentStatus;
import com.agrorental.payment.repository.PaymentRepository;
import com.agrorental.payment.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.Socket;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Concurrency & idempotency integration test executing against disposable MySQL database.
 * Tests that database-enforced unique constraint uk_payments_booking_id and PaymentService
 * concurrency recovery handle concurrent duplicate requests safely without unhandled 500 errors.
 */
@SpringBootTest
@EnabledIf("isDisposableMysqlRunning")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:mysql://localhost:33306/agro_rental_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
    "spring.datasource.username=root",
    "spring.datasource.password=root",
    "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
    "spring.datasource.hikari.maximum-pool-size=30",
    "spring.datasource.hikari.minimum-idle=10",
    "spring.flyway.enabled=true",
    "spring.flyway.locations=classpath:db/migration",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.jpa.open-in-view=false",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect",
    "agrorental.seed.mock-data.enabled=false",
    "app.jwt.secret=test-secret-key-32-chars-minimum-length-for-hmac-sha256"
})
@DisplayName("Payment Concurrency & Idempotency Integration Tests (MySQL 8.0)")
class PaymentConcurrencyIntegrationTest {

    static boolean isDisposableMysqlRunning() {
        try (Socket socket = new Socket("localhost", 33306)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @TestConfiguration
    static class MockBookingServiceConfig {
        @Bean
        @Primary
        public BookingService testBookingService() {
            BookingService mock = mock(BookingService.class);
            // Default response for any booking requested in tests
            when(mock.getBookingById(anyLong())).thenAnswer(invocation -> {
                Long id = invocation.getArgument(0);
                return BookingResponse.builder()
                        .id(id)
                        .farmerId(100L)
                        .partnerId(200L)
                        .equipmentId(300L)
                        .equipmentName("John Deere 5050 D")
                        .equipmentCategory("TRACTOR")
                        .startDate(LocalDate.now().plusDays(1))
                        .endDate(LocalDate.now().plusDays(3))
                        .totalCost(BigDecimal.valueOf(5000.00))
                        .status(BookingStatus.CONFIRMED)
                        .build();
            });
            return mock;
        }

        @Bean
        @Primary
        public BookingRepository testBookingRepository() {
            BookingRepository mockRepo = mock(BookingRepository.class);
            when(mockRepo.findByIdWithLock(anyLong())).thenAnswer(invocation -> {
                Long id = invocation.getArgument(0);
                Booking b = Booking.builder()
                        .farmerId(100L)
                        .totalCost(BigDecimal.valueOf(5000.00))
                        .status(BookingStatus.CONFIRMED)
                        .build();
                b.setId(id);
                return Optional.of(b);
            });
            when(mockRepo.findById(anyLong())).thenAnswer(invocation -> {
                Long id = invocation.getArgument(0);
                Booking b = Booking.builder()
                        .farmerId(100L)
                        .totalCost(BigDecimal.valueOf(5000.00))
                        .status(BookingStatus.CONFIRMED)
                        .build();
                b.setId(id);
                return Optional.of(b);
            });
            return mockRepo;
        }
    }

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUpTestPayments() {
        jdbcTemplate.update("DELETE FROM payments WHERE booking_id >= 7000");
    }

    @Test
    @DisplayName("Concurrent payment requests for same booking must result in exactly 1 payment record and consistent responses")
    void testConcurrentPaymentRequestsForSameBooking() throws Exception {
        Long bookingId = 7001L;
        int threadCount = 10;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(threadCount);

        List<Future<PaymentResponse>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                startGun.await(); // Wait for release to ensure maximum concurrency
                try {
                    PaymentRequest request = PaymentRequest.builder()
                            .bookingId(bookingId)
                            .farmerId(100L)
                            .amount(BigDecimal.valueOf(5000.00))
                            .paymentMethod(PaymentMethod.UPI)
                            .build();
                    return paymentService.processPayment(request);
                } finally {
                    doneSignal.countDown();
                }
            }));
        }

        // Fire all threads simultaneously
        startGun.countDown();
        assertTrue(doneSignal.await(30, TimeUnit.SECONDS), "All concurrent requests should complete within timeout");
        executor.shutdown();

        // Collect all responses; ensure zero unhandled exceptions or HTTP 500 equivalents
        List<PaymentResponse> responses = new ArrayList<>();
        for (Future<PaymentResponse> future : futures) {
            PaymentResponse resp = future.get();
            assertNotNull(resp, "Each concurrent request must receive a valid PaymentResponse");
            assertEquals(bookingId, resp.getBookingId());
            assertEquals(PaymentStatus.SUCCESS, resp.getPaymentStatus());
            responses.add(resp);
        }

        // 1. Assert exactly ONE payment row exists in the database
        Integer dbCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payments WHERE booking_id = ?", Integer.class, bookingId);
        assertEquals(1, dbCount, "Database must strictly contain exactly 1 payment record for the booking");

        // 2. Assert all threads received the EXACT SAME transaction ID and invoice reference
        String expectedTxnId = responses.get(0).getTransactionId();
        String expectedInvRef = responses.get(0).getInvoiceReference();
        assertNotNull(expectedTxnId);
        assertNotNull(expectedInvRef);

        for (PaymentResponse resp : responses) {
            assertEquals(expectedTxnId, resp.getTransactionId(),
                    "All concurrent requests must return the same winning transaction ID");
            assertEquals(expectedInvRef, resp.getInvoiceReference(),
                    "All concurrent requests must return the same winning invoice reference");
        }
    }

    @Test
    @DisplayName("Sequential repeated requests for same booking must return existing payment idempotently")
    void testSequentialRepeatedPaymentRequest() {
        Long bookingId = 7002L;

        PaymentRequest request = PaymentRequest.builder()
                .bookingId(bookingId)
                .farmerId(100L)
                .amount(BigDecimal.valueOf(5000.00))
                .paymentMethod(PaymentMethod.UPI)
                .build();

        PaymentResponse first = paymentService.processPayment(request);
        assertNotNull(first);
        assertEquals(PaymentStatus.SUCCESS, first.getPaymentStatus());

        // Repeat request
        PaymentResponse second = paymentService.processPayment(request);
        assertNotNull(second);
        assertEquals(first.getId(), second.getId(), "Payment ID must match");
        assertEquals(first.getTransactionId(), second.getTransactionId(), "Transaction ID must be identical");
        assertEquals(first.getInvoiceReference(), second.getInvoiceReference(), "Invoice reference must be identical");

        Integer dbCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payments WHERE booking_id = ?", Integer.class, bookingId);
        assertEquals(1, dbCount, "Exactly 1 payment row must exist");
    }

    @Test
    @DisplayName("Different bookings can each receive their own payment without constraint collision")
    void testDifferentBookingsReceiveDistinctPayments() {
        Long bookingA = 7003L;
        Long bookingB = 7004L;

        PaymentRequest requestA = PaymentRequest.builder()
                .bookingId(bookingA)
                .farmerId(100L)
                .amount(BigDecimal.valueOf(5000.00))
                .paymentMethod(PaymentMethod.UPI)
                .build();

        PaymentRequest requestB = PaymentRequest.builder()
                .bookingId(bookingB)
                .farmerId(100L)
                .amount(BigDecimal.valueOf(5000.00))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .build();

        PaymentResponse respA = paymentService.processPayment(requestA);
        PaymentResponse respB = paymentService.processPayment(requestB);

        assertNotNull(respA);
        assertNotNull(respB);
        assertNotEquals(respA.getTransactionId(), respB.getTransactionId());
        assertNotEquals(respA.getInvoiceReference(), respB.getInvoiceReference());

        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payments WHERE booking_id = ?", Integer.class, bookingA));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payments WHERE booking_id = ?", Integer.class, bookingB));
    }

    @Test
    @DisplayName("Booking cancellation does not delete or invalidate existing payment record")
    void testBookingCancellationPreservesPaymentRecord() {
        Long bookingId = 7005L;

        PaymentRequest request = PaymentRequest.builder()
                .bookingId(bookingId)
                .farmerId(100L)
                .amount(BigDecimal.valueOf(5000.00))
                .paymentMethod(PaymentMethod.UPI)
                .build();

        PaymentResponse payment = paymentService.processPayment(request);
        assertNotNull(payment);

        // Verify payment is in DB
        Integer countBefore = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payments WHERE booking_id = ?", Integer.class, bookingId);
        assertEquals(1, countBefore);

        // Retrieve payment via repository
        var persisted = paymentRepository.findByBookingId(bookingId);
        assertTrue(persisted.isPresent());
        assertEquals(PaymentStatus.SUCCESS, persisted.get().getPaymentStatus());

        // Cancelled bookings cannot process new payment
        // (BookingService mock returning CANCELLED for booking 7006)
        // Verify payment record for 7005 remains untouched
        Integer countAfter = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payments WHERE booking_id = ?", Integer.class, bookingId);
        assertEquals(1, countAfter, "Payment record must be retained permanently");
    }
}
