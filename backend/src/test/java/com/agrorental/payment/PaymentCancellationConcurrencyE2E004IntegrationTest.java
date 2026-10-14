package com.agrorental.payment;

import com.agrorental.booking.dto.BookingResponse;
import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.booking.repository.BookingRepository;
import com.agrorental.booking.service.BookingService;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.enums.AvailabilityStatus;
import com.agrorental.equipment.enums.EquipmentCategory;
import com.agrorental.equipment.enums.FuelType;
import com.agrorental.equipment.repository.EquipmentRepository;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.partner.entity.Partner;
import com.agrorental.partner.repository.PartnerRepository;
import com.agrorental.payment.dto.PaymentRequest;
import com.agrorental.payment.dto.PaymentResponse;
import com.agrorental.payment.entity.Payment;
import com.agrorental.payment.entity.PaymentMethod;
import com.agrorental.payment.entity.PaymentStatus;
import com.agrorental.payment.repository.PaymentRepository;
import com.agrorental.payment.service.PaymentService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Barrier-Coordinated Concurrency Integration Tests for E2E-004.2 (Defect F-04):
 * Verifies that concurrent competing payment, cancellation, and rejection requests targeting
 * the same booking are safely serialized via pessimistic booking row locking and transactional boundaries.
 *
 * Guarantees:
 * 1. A payment attempt cannot record a successful payment for a booking that is cancelled or rejected.
 * 2. Cancellation and rejection serialize safely with in-flight payments without corrupting ledger state.
 * 3. Concurrent duplicate payments resolve idempotently with strictly one persisted payment record.
 * 4. Runs against an isolated, disposable in-memory database in MySQL mode without touching any live database.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:e2e004_payment_concurrency_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "agrorental.seed.mock-data.enabled=false",
    "app.jwt.secret=test-secret-key-32-chars-minimum-length-for-hmac-sha256",
    "server.port=0"
})
@DisplayName("E2E-004.2: Payment vs Cancellation/Rejection Concurrency Integration Tests")
class PaymentCancellationConcurrencyE2E004IntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private FarmerRepository farmerRepository;

    @Autowired
    private PartnerRepository partnerRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    private Partner testPartner;
    private Farmer testFarmer;
    private Equipment testEquipment;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        paymentRepository.deleteAll();
        bookingRepository.deleteAll();
        equipmentRepository.deleteAll();
        farmerRepository.deleteAll();
        partnerRepository.deleteAll();

        testPartner = Partner.builder()
                .fullName("Vikram Shinde")
                .businessName("Shinde Agro Solutions")
                .email("vikram.shinde@agrotest.in")
                .mobileNumber("9823000001")
                .password("hashed_pwd")
                .build();
        testPartner.setActive(true);
        testPartner = partnerRepository.save(testPartner);

        testFarmer = Farmer.builder()
                .fullName("Anand Patil")
                .mobileNumber("9823000002")
                .email("anand.patil@agrotest.in")
                .address("Village Shirur, Pune")
                .build();
        testFarmer.setActive(true);
        testFarmer = farmerRepository.save(testFarmer);

        testEquipment = Equipment.builder()
                .name("Mahindra 575 DI SP Plus")
                .category(EquipmentCategory.TRACTOR)
                .brand("Mahindra")
                .model("575 DI")
                .manufacturingYear(2023)
                .capacity("47 HP")
                .rentalPrice(new BigDecimal("2500.00"))
                .fuelType(FuelType.DIESEL)
                .description("High efficiency agricultural tractor")
                .locationAddress("Shirur, Pune")
                .latitude(18.5204)
                .longitude(73.8567)
                .partner(testPartner)
                .availabilityStatus(AvailabilityStatus.BOOKED)
                .build();
        testEquipment = equipmentRepository.save(testEquipment);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Booking createBooking(BookingStatus initialStatus, BigDecimal cost) {
        Booking booking = Booking.builder()
                .farmerId(testFarmer.getId())
                .partner(testPartner)
                .equipment(testEquipment)
                .status(initialStatus)
                .startDate(LocalDate.of(2026, 11, 10))
                .endDate(LocalDate.of(2026, 11, 12))
                .totalCost(cost)
                .deliveryAddress("Patil Farms, Shirur")
                .build();
        return bookingRepository.save(booking);
    }

    @Test
    @DisplayName("Race Condition 1: Payment vs. Cancellation on the same booking serializes safely with explicit permitted states")
    void testPaymentVsCancellation_ConcurrentRace() throws Exception {
        Booking booking = createBooking(BookingStatus.CONFIRMED, new BigDecimal("7500.00"));
        final Long bookingId = booking.getId();
        final Long farmerId = testFarmer.getId();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startGun = new CountDownLatch(1);

        AtomicReference<BookingResponse> cancelResult = new AtomicReference<>();
        AtomicReference<Exception> cancelException = new AtomicReference<>();
        AtomicReference<PaymentResponse> paymentResult = new AtomicReference<>();
        AtomicReference<Exception> paymentException = new AtomicReference<>();

        Future<?> cancelFuture = executor.submit(() -> {
            readyLatch.countDown();
            try {
                startGun.await();
                BookingResponse resp = bookingService.cancelBooking(bookingId, farmerId);
                cancelResult.set(resp);
            } catch (Exception e) {
                cancelException.set(e);
            }
        });

        Future<?> paymentFuture = executor.submit(() -> {
            readyLatch.countDown();
            try {
                startGun.await();
                PaymentRequest req = PaymentRequest.builder()
                        .bookingId(bookingId)
                        .farmerId(farmerId)
                        .amount(new BigDecimal("7500.00"))
                        .paymentMethod(PaymentMethod.UPI)
                        .build();
                PaymentResponse resp = paymentService.processPayment(req);
                paymentResult.set(resp);
            } catch (Exception e) {
                paymentException.set(e);
            }
        });

        assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue();
        startGun.countDown(); // Release both competing operations simultaneously

        cancelFuture.get(10, TimeUnit.SECONDS);
        paymentFuture.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Database inspection after race resolution
        Booking finalBooking = bookingRepository.findById(bookingId).orElseThrow();
        Optional<Payment> finalPayment = paymentRepository.findByBookingId(bookingId);

        // Verification: Exactly one of the two serialized orderings must occur
        if (cancelException.get() == null && paymentException.get() != null) {
            // ORDERING A: Cancellation won the row lock first
            // Payment attempt unblocked, observed CANCELLED status, and was strictly rejected
            assertThat(paymentException.get()).isInstanceOf(BadRequestException.class);
            assertThat(paymentException.get().getMessage()).contains("Cannot process payment for a booking with status: CANCELLED");
            assertThat(finalBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
            assertThat(finalPayment).isEmpty(); // NEVER fabricated or saved a payment
        } else {
            // ORDERING B: Payment won the row lock first
            // Payment completed successfully; cancellation then completed per supported rules
            assertThat(paymentException.get()).isNull();
            assertThat(paymentResult.get()).isNotNull();
            assertThat(paymentResult.get().getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(cancelException.get()).isNull();
            assertThat(finalBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
            assertThat(finalPayment).isPresent();
            assertThat(finalPayment.get().getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        }

        // Integrity assertion: Final booking state must never remain unexamined or corrupted
        assertThat(finalBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    @DisplayName("Race Condition 2: Payment vs. Partner Rejection on the same booking serializes safely with explicit permitted states")
    void testPaymentVsRejection_ConcurrentRace() throws Exception {
        Booking booking = createBooking(BookingStatus.CONFIRMED, new BigDecimal("7500.00"));
        final Long bookingId = booking.getId();
        final Long partnerId = testPartner.getId();
        final Long farmerId = testFarmer.getId();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startGun = new CountDownLatch(1);

        AtomicReference<BookingResponse> rejectResult = new AtomicReference<>();
        AtomicReference<Exception> rejectException = new AtomicReference<>();
        AtomicReference<PaymentResponse> paymentResult = new AtomicReference<>();
        AtomicReference<Exception> paymentException = new AtomicReference<>();

        Future<?> rejectFuture = executor.submit(() -> {
            readyLatch.countDown();
            try {
                startGun.await();
                BookingResponse resp = bookingService.rejectBooking(bookingId, partnerId, "Tractor assigned to emergency government relief");
                rejectResult.set(resp);
            } catch (Exception e) {
                rejectException.set(e);
            }
        });

        Future<?> paymentFuture = executor.submit(() -> {
            readyLatch.countDown();
            try {
                startGun.await();
                PaymentRequest req = PaymentRequest.builder()
                        .bookingId(bookingId)
                        .farmerId(farmerId)
                        .amount(new BigDecimal("7500.00"))
                        .paymentMethod(PaymentMethod.NET_BANKING)
                        .build();
                PaymentResponse resp = paymentService.processPayment(req);
                paymentResult.set(resp);
            } catch (Exception e) {
                paymentException.set(e);
            }
        });

        assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue();
        startGun.countDown();

        rejectFuture.get(10, TimeUnit.SECONDS);
        paymentFuture.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        Booking finalBooking = bookingRepository.findById(bookingId).orElseThrow();
        Optional<Payment> finalPayment = paymentRepository.findByBookingId(bookingId);

        if (rejectException.get() == null && paymentException.get() != null) {
            // ORDERING A: Partner Rejection won the lock first
            assertThat(paymentException.get()).isInstanceOf(BadRequestException.class);
            assertThat(paymentException.get().getMessage()).contains("Cannot process payment for a booking with status: REJECTED");
            assertThat(finalBooking.getStatus()).isEqualTo(BookingStatus.REJECTED);
            assertThat(finalPayment).isEmpty();
        } else {
            // ORDERING B: Payment won the lock first
            assertThat(paymentException.get()).isNull();
            assertThat(paymentResult.get()).isNotNull();
            assertThat(paymentResult.get().getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(rejectException.get()).isNull();
            assertThat(finalBooking.getStatus()).isEqualTo(BookingStatus.REJECTED);
            assertThat(finalPayment).isPresent();
            assertThat(finalPayment.get().getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        }

        assertThat(finalBooking.getStatus()).isEqualTo(BookingStatus.REJECTED);
        assertThat(finalBooking.getRejectionReason()).contains("emergency government relief");
    }

    @Test
    @DisplayName("Ordering Verification: When Cancellation commits first, subsequent payment attempt is strictly rejected without recording payment")
    void testCancellationCommittedFirst_PaymentStrictlyRejected() {
        Booking booking = createBooking(BookingStatus.CONFIRMED, new BigDecimal("5000.00"));
        bookingService.cancelBooking(booking.getId(), testFarmer.getId());

        Booking cancelledBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(cancelledBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);

        PaymentRequest paymentRequest = PaymentRequest.builder()
                .bookingId(booking.getId())
                .farmerId(testFarmer.getId())
                .amount(new BigDecimal("5000.00"))
                .paymentMethod(PaymentMethod.UPI)
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                paymentService.processPayment(paymentRequest)
        );

        assertThat(ex.getMessage()).contains("Cannot process payment for a booking with status: CANCELLED");
        assertThat(paymentRepository.findByBookingId(booking.getId())).isEmpty();
    }

    @Test
    @DisplayName("Ordering Verification: When Rejection commits first, subsequent payment attempt is strictly rejected without recording payment")
    void testRejectionCommittedFirst_PaymentStrictlyRejected() {
        Booking booking = createBooking(BookingStatus.CONFIRMED, new BigDecimal("5000.00"));
        bookingService.rejectBooking(booking.getId(), testPartner.getId(), "Machinery undergoing scheduled maintenance");

        Booking rejectedBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(rejectedBooking.getStatus()).isEqualTo(BookingStatus.REJECTED);

        PaymentRequest paymentRequest = PaymentRequest.builder()
                .bookingId(booking.getId())
                .farmerId(testFarmer.getId())
                .amount(new BigDecimal("5000.00"))
                .paymentMethod(PaymentMethod.UPI)
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                paymentService.processPayment(paymentRequest)
        );

        assertThat(ex.getMessage()).contains("Cannot process payment for a booking with status: REJECTED");
        assertThat(paymentRepository.findByBookingId(booking.getId())).isEmpty();
    }

    @Test
    @DisplayName("Ordering Verification: When Payment commits first, cancellation follows supported rules without data corruption")
    void testPaymentCommittedFirst_CancellationFollowsSupportedRules() {
        Booking booking = createBooking(BookingStatus.CONFIRMED, new BigDecimal("5000.00"));

        PaymentRequest paymentRequest = PaymentRequest.builder()
                .bookingId(booking.getId())
                .farmerId(testFarmer.getId())
                .amount(new BigDecimal("5000.00"))
                .paymentMethod(PaymentMethod.UPI)
                .build();

        PaymentResponse paymentResp = paymentService.processPayment(paymentRequest);
        assertThat(paymentResp.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);

        BookingResponse cancelResp = bookingService.cancelBooking(booking.getId(), testFarmer.getId());
        assertThat(cancelResp.getStatus()).isEqualTo(BookingStatus.CANCELLED);

        Booking reloadedBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        Payment reloadedPayment = paymentRepository.findByBookingId(booking.getId()).orElseThrow();

        assertThat(reloadedBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(reloadedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    @DisplayName("Idempotency: Repeated concurrent payment attempts serialize safely and result in strictly 1 persisted payment record")
    void testConcurrentRepeatedPayments_IdempotencySafe() throws Exception {
        Booking booking = createBooking(BookingStatus.CONFIRMED, new BigDecimal("6000.00"));
        final Long bookingId = booking.getId();
        final Long farmerId = testFarmer.getId();

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startGun = new CountDownLatch(1);

        List<Future<PaymentResponse>> futures = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startGun.await();
                    PaymentRequest req = PaymentRequest.builder()
                            .bookingId(bookingId)
                            .farmerId(farmerId)
                            .amount(new BigDecimal("6000.00"))
                            .paymentMethod(PaymentMethod.UPI)
                            .build();
                    PaymentResponse resp = paymentService.processPayment(req);
                    successCount.incrementAndGet();
                    return resp;
                } catch (Exception e) {
                    return null;
                }
            }));
        }

        assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue();
        startGun.countDown(); // Fire simultaneous payment requests

        List<String> transactionIds = new ArrayList<>();
        for (Future<PaymentResponse> f : futures) {
            PaymentResponse resp = f.get(10, TimeUnit.SECONDS);
            assertThat(resp).isNotNull();
            assertThat(resp.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
            transactionIds.add(resp.getTransactionId());
        }
        executor.shutdown();

        // All 5 threads received a successful payment response
        assertThat(successCount.get()).isEqualTo(threadCount);

        // All 5 threads received the exact same transaction ID (idempotent resolution)
        String canonicalTxnId = transactionIds.get(0);
        for (String txnId : transactionIds) {
            assertThat(txnId).isEqualTo(canonicalTxnId);
        }

        // Database verification: strictly ONE payment record exists in the database
        List<Payment> allPayments = paymentRepository.findAll();
        List<Payment> bookingPayments = allPayments.stream()
                .filter(p -> p.getBookingId().equals(bookingId))
                .toList();
        assertThat(bookingPayments).hasSize(1);
        assertThat(bookingPayments.get(0).getTransactionId()).isEqualTo(canonicalTxnId);
        assertThat(bookingPayments.get(0).getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }
}
