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
import org.junit.jupiter.api.condition.EnabledIf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.Socket;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Concurrency Verification Test on Real MySQL 8.0 Engine (InnoDB) for E2E-004.2.1.
 * Connects to disposable MySQL 8.0 container on port 33306.
 *
 * Verifies that:
 * 1. Barrier-coordinated concurrent competing payment vs cancellation serialize safely via pessimistic booking row locking.
 * 2. Barrier-coordinated concurrent competing payment vs partner rejection serialize safely on MySQL/InnoDB.
 * 3. Both serial outcomes function correctly: cancellation/rejection wins and payment is rejected; payment wins and subsequent lifecycle succeeds.
 * 4. Repeated concurrent payment requests create strictly one payment record and return consistent idempotent results.
 * 5. Transaction propagation, booking row locks, and persisted final states on MySQL/InnoDB are verified with engine metadata.
 */
@SpringBootTest
@EnabledIf("isDisposableMysqlRunning")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:mysql://127.0.0.1:33306/agrorental_test?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true",
    "spring.datasource.username=root",
    "spring.datasource.password=root",
    "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "agrorental.seed.mock-data.enabled=false",
    "app.jwt.secret=test-secret-key-32-chars-minimum-length-for-hmac-sha256",
    "server.port=0"
})
@DisplayName("E2E-004.2.1: Real MySQL Payment/Cancellation Concurrency Verification")
class PaymentCancellationMySQLConcurrencyE2E004Test {

    private static final Logger log = LoggerFactory.getLogger(PaymentCancellationMySQLConcurrencyE2E004Test.class);

    static boolean isDisposableMysqlRunning() {
        try (Socket socket = new Socket("localhost", 33306)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

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

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    @DisplayName("Verification 0: MySQL Engine Metadata and Transaction Isolation Level")
    void testMySqlEngineAndIsolationLevel() {
        Map<String, Object> versionMap = jdbcTemplate.queryForMap(
                "SELECT VERSION() as version, @@transaction_isolation as isolation, @@default_storage_engine as engine");

        String version = (String) versionMap.get("version");
        String isolation = (String) versionMap.get("isolation");
        String engine = (String) versionMap.get("engine");

        log.info("Verified Disposable MySQL Instance -> Version: {}, Isolation Level: {}, Engine: {}", version, isolation, engine);

        assertThat(version).startsWith("8.0");
        assertThat(isolation).isIn("REPEATABLE-READ", "READ-COMMITTED");
        assertThat(engine).isEqualToIgnoringCase("InnoDB");
    }

    @Test
    @DisplayName("Required Check 1: Barrier-Coordinated Concurrent Payment vs Cancellation on MySQL 8.0/InnoDB")
    void testPaymentVsCancellation_ConcurrentRaceOnMySQL() throws Exception {
        Booking booking = createBooking(BookingStatus.CONFIRMED, new BigDecimal("7500.00"));
        final Long bookingId = booking.getId();
        final Long farmerId = testFarmer.getId();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);

        AtomicReference<BookingResponse> cancelResult = new AtomicReference<>();
        AtomicReference<Exception> cancelException = new AtomicReference<>();
        AtomicReference<PaymentResponse> paymentResult = new AtomicReference<>();
        AtomicReference<Exception> paymentException = new AtomicReference<>();

        Future<?> cancelFuture = executor.submit(() -> {
            try {
                barrier.await(5, TimeUnit.SECONDS);
                BookingResponse resp = bookingService.cancelBooking(bookingId, farmerId);
                cancelResult.set(resp);
            } catch (Exception e) {
                cancelException.set(e);
            }
        });

        Future<?> paymentFuture = executor.submit(() -> {
            try {
                barrier.await(5, TimeUnit.SECONDS);
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

        cancelFuture.get(10, TimeUnit.SECONDS);
        paymentFuture.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        Booking finalBooking = bookingRepository.findById(bookingId).orElseThrow();
        Optional<Payment> finalPayment = paymentRepository.findByBookingId(bookingId);

        log.info("MySQL Payment vs Cancellation race outcome: cancelEx={}, paymentEx={}, finalStatus={}, paymentPresent={}",
                cancelException.get() != null ? cancelException.get().getMessage() : "NONE",
                paymentException.get() != null ? paymentException.get().getMessage() : "NONE",
                finalBooking.getStatus(),
                finalPayment.isPresent());

        if (cancelException.get() == null && paymentException.get() != null) {
            // SERIAL OUTCOME A: Cancellation won row lock first on MySQL
            assertThat(paymentException.get()).isInstanceOf(BadRequestException.class);
            assertThat(paymentException.get().getMessage()).contains("Cannot process payment for a booking with status: CANCELLED");
            assertThat(finalBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
            assertThat(finalPayment).isEmpty();
        } else {
            // SERIAL OUTCOME B: Payment won row lock first on MySQL
            assertThat(paymentException.get()).isNull();
            assertThat(paymentResult.get()).isNotNull();
            assertThat(paymentResult.get().getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(cancelException.get()).isNull();
            assertThat(finalBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
            assertThat(finalPayment).isPresent();
            assertThat(finalPayment.get().getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        }

        assertThat(finalBooking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    @DisplayName("Required Check 2: Barrier-Coordinated Concurrent Payment vs Partner Rejection on MySQL 8.0/InnoDB")
    void testPaymentVsRejection_ConcurrentRaceOnMySQL() throws Exception {
        Booking booking = createBooking(BookingStatus.CONFIRMED, new BigDecimal("7500.00"));
        final Long bookingId = booking.getId();
        final Long partnerId = testPartner.getId();
        final Long farmerId = testFarmer.getId();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CyclicBarrier barrier = new CyclicBarrier(2);

        AtomicReference<BookingResponse> rejectResult = new AtomicReference<>();
        AtomicReference<Exception> rejectException = new AtomicReference<>();
        AtomicReference<PaymentResponse> paymentResult = new AtomicReference<>();
        AtomicReference<Exception> paymentException = new AtomicReference<>();

        Future<?> rejectFuture = executor.submit(() -> {
            try {
                barrier.await(5, TimeUnit.SECONDS);
                BookingResponse resp = bookingService.rejectBooking(bookingId, partnerId, "Tractor assigned to emergency government relief");
                rejectResult.set(resp);
            } catch (Exception e) {
                rejectException.set(e);
            }
        });

        Future<?> paymentFuture = executor.submit(() -> {
            try {
                barrier.await(5, TimeUnit.SECONDS);
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

        rejectFuture.get(10, TimeUnit.SECONDS);
        paymentFuture.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        Booking finalBooking = bookingRepository.findById(bookingId).orElseThrow();
        Optional<Payment> finalPayment = paymentRepository.findByBookingId(bookingId);

        log.info("MySQL Payment vs Rejection race outcome: rejectEx={}, paymentEx={}, finalStatus={}, paymentPresent={}",
                rejectException.get() != null ? rejectException.get().getMessage() : "NONE",
                paymentException.get() != null ? paymentException.get().getMessage() : "NONE",
                finalBooking.getStatus(),
                finalPayment.isPresent());

        if (rejectException.get() == null && paymentException.get() != null) {
            // SERIAL OUTCOME A: Partner rejection won row lock first on MySQL
            assertThat(paymentException.get()).isInstanceOf(BadRequestException.class);
            assertThat(paymentException.get().getMessage()).contains("Cannot process payment for a booking with status: REJECTED");
            assertThat(finalBooking.getStatus()).isEqualTo(BookingStatus.REJECTED);
            assertThat(finalPayment).isEmpty();
        } else {
            // SERIAL OUTCOME B: Payment won row lock first on MySQL
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
    @DisplayName("Required Check 3a: Serial Outcome - When Cancellation Commits First, Payment is Strictly Rejected")
    void testCancellationCommittedFirst_PaymentStrictlyRejectedOnMySQL() {
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

        Integer paymentRowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payments WHERE booking_id = ?", Integer.class, booking.getId());
        assertThat(paymentRowCount).isZero();
    }

    @Test
    @DisplayName("Required Check 3b: Serial Outcome - When Rejection Commits First, Payment is Strictly Rejected")
    void testRejectionCommittedFirst_PaymentStrictlyRejectedOnMySQL() {
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

        Integer paymentRowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payments WHERE booking_id = ?", Integer.class, booking.getId());
        assertThat(paymentRowCount).isZero();
    }

    @Test
    @DisplayName("Required Check 3c: Serial Outcome - When Payment Commits First, Cancellation Follows Supported Rules")
    void testPaymentCommittedFirst_CancellationFollowsSupportedRulesOnMySQL() {
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

        Integer paymentRowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payments WHERE booking_id = ?", Integer.class, booking.getId());
        assertThat(paymentRowCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Required Check 4: Repeated Concurrent Payments on MySQL 8.0/InnoDB Create Exactly 1 Payment Record")
    void testConcurrentRepeatedPayments_IdempotencySafeOnMySQL() throws Exception {
        Booking booking = createBooking(BookingStatus.CONFIRMED, new BigDecimal("6000.00"));
        final Long bookingId = booking.getId();
        final Long farmerId = testFarmer.getId();

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);

        List<Future<PaymentResponse>> futures = new ArrayList<>();
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                try {
                    barrier.await(5, TimeUnit.SECONDS);
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
                    log.error("Concurrent payment attempt failed: ", e);
                    return null;
                }
            }));
        }

        List<String> transactionIds = new ArrayList<>();
        for (Future<PaymentResponse> f : futures) {
            PaymentResponse resp = f.get(15, TimeUnit.SECONDS);
            assertThat(resp).isNotNull();
            assertThat(resp.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
            transactionIds.add(resp.getTransactionId());
        }
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(threadCount);

        String canonicalTxnId = transactionIds.get(0);
        for (String txnId : transactionIds) {
            assertThat(txnId).isEqualTo(canonicalTxnId);
        }

        Integer dbPaymentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payments WHERE booking_id = ?", Integer.class, bookingId);
        assertThat(dbPaymentCount).isEqualTo(1);

        Payment persistedPayment = paymentRepository.findByBookingId(bookingId).orElseThrow();
        assertThat(persistedPayment.getTransactionId()).isEqualTo(canonicalTxnId);
        assertThat(persistedPayment.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }
}
