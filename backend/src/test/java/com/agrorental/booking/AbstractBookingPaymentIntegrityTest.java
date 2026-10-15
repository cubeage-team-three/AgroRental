package com.agrorental.booking;

import com.agrorental.admin.dto.AdminBookingStatsResponse;
import com.agrorental.admin.dto.AdminDashboardStatsResponse;
import com.agrorental.admin.service.AdminBookingService;
import com.agrorental.admin.service.AdminDashboardService;
import com.agrorental.booking.dto.BookingCreateRequest;
import com.agrorental.booking.dto.BookingStatusUpdateRequest;
import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.booking.repository.BookingRepository;
import com.agrorental.booking.service.BookingService;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.ForbiddenException;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.enums.AvailabilityStatus;
import com.agrorental.equipment.enums.EquipmentCategory;
import com.agrorental.equipment.enums.FuelType;
import com.agrorental.equipment.repository.EquipmentRepository;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.partner.entity.Partner;
import com.agrorental.partner.dto.PartnerDashboardResponse;
import com.agrorental.partner.repository.PartnerRepository;
import com.agrorental.partner.service.PartnerService;
import com.agrorental.payment.dto.InvoiceResponse;
import com.agrorental.payment.dto.PartnerBookingRevenueReportDto;
import com.agrorental.payment.dto.PartnerEarningsDetailResponse;
import com.agrorental.payment.dto.PartnerEarningsSummary;
import com.agrorental.payment.dto.PaymentCreateRequest;
import com.agrorental.payment.dto.PaymentRequest;
import com.agrorental.payment.dto.PaymentResponse;
import com.agrorental.payment.dto.RefundResponse;
import com.agrorental.payment.entity.Payment;
import com.agrorental.payment.entity.PaymentMethod;
import com.agrorental.payment.entity.PaymentRefund;
import com.agrorental.payment.entity.PaymentStatus;
import com.agrorental.payment.entity.RefundStatus;
import com.agrorental.payment.repository.PaymentRefundEventRepository;
import com.agrorental.payment.repository.PaymentRefundRepository;
import com.agrorental.payment.repository.PaymentRepository;
import com.agrorental.payment.service.PaymentService;
import com.agrorental.payment.service.RefundService;
import com.agrorental.security.principal.CurrentCaller;
import com.agrorental.security.principal.FarmerPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * BIZ-13 / BIZ-14 / BIZ-20 regression scenarios, run against H2 by {@link BookingPaymentIntegrityIntegrationTest}
 * and against a disposable MySQL 8 by {@link BookingPaymentIntegrityMySQLTest} (gated; skipped when unavailable).
 * Every test creates its own partner, equipment and farmers, so tests are independent of each other and of
 * pre-existing rows. Concurrency tests use real threads released together by a latch.
 */
@SpringBootTest
abstract class AbstractBookingPaymentIntegrityTest {

    private static final AtomicInteger SEQUENCE = new AtomicInteger(1000);
    private static final BigDecimal DAILY_RATE = new BigDecimal("2500.00");

    @Autowired protected BookingService bookingService;
    @Autowired protected PaymentService paymentService;
    @Autowired protected BookingRepository bookingRepository;
    @Autowired protected PaymentRepository paymentRepository;
    @Autowired protected EquipmentRepository equipmentRepository;
    @Autowired protected PartnerRepository partnerRepository;
    @Autowired protected FarmerRepository farmerRepository;
    @Autowired protected RefundService refundService;
    @Autowired protected PaymentRefundRepository paymentRefundRepository;
    @Autowired protected PaymentRefundEventRepository paymentRefundEventRepository;
    @Autowired protected PlatformTransactionManager transactionManager;
    @Autowired protected PartnerService partnerService;
    @Autowired protected AdminDashboardService adminDashboardService;
    @Autowired protected AdminBookingService adminBookingService;

    protected Partner partner;
    protected Equipment equipment;

    @BeforeEach
    void createIsolatedFixtures() {
        int n = SEQUENCE.incrementAndGet();
        partner = partnerRepository.save(Partner.builder().fullName("Integrity Partner " + n).businessName("Rentals " + n)
                .mobileNumber(uniqueMobile("96", n)).email("partner" + n + "@integrity.test").password("x").otpVerified(true)
                .verificationStatus(Partner.VerificationStatus.APPROVED).build());
        equipment = equipmentRepository.save(Equipment.builder().name("Tractor " + n).category(EquipmentCategory.TRACTOR)
                .brand("Mahindra").model("575 DI").manufacturingYear(2023).capacity("47 HP").rentalPrice(DAILY_RATE)
                .fuelType(FuelType.DIESEL).description("Tractor").locationAddress("Pune").latitude(18.52).longitude(73.85)
                .partner(partner).availabilityStatus(AvailabilityStatus.AVAILABLE).build());
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ---- helpers ------------------------------------------------------------------------------------

    private static String uniqueMobile(String prefix, int n) {
        return prefix + String.format("%08d", n);
    }

    protected Farmer newFarmer() {
        int n = SEQUENCE.incrementAndGet();
        return farmerRepository.save(Farmer.builder().fullName("Integrity Farmer " + n).mobileNumber(uniqueMobile("98", n))
                .email("farmer" + n + "@integrity.test").accountStatus("ACTIVE").build());
    }

    protected static void actAsFarmer(Farmer farmer) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                FarmerPrincipal.builder().id(farmer.getId()).role("FARMER").build(), null,
                List.of(new SimpleGrantedAuthority("ROLE_FARMER"))));
    }

    /** Far-future dates so @FutureOrPresent-style rules never interfere; offset keeps ranges distinct. */
    protected static LocalDate day(int offset) {
        return LocalDate.of(2031, 1, 1).plusDays(offset);
    }

    protected Long createBookingAs(Farmer farmer, LocalDate start, LocalDate end) {
        actAsFarmer(farmer);
        try {
            return bookingService.createBooking(BookingCreateRequest.builder().equipmentId(equipment.getId())
                    .startDate(start).endDate(end).deliveryAddress("Field").build()).getId();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /** Inserts a booking directly (bypassing creation rules), e.g. to model data from before the fix. */
    protected Booking insertBooking(Farmer farmer, BookingStatus status, LocalDate start, LocalDate end) {
        return bookingRepository.save(Booking.builder().farmerId(farmer.getId()).partner(partner).equipment(equipment)
                .status(status).startDate(start).endDate(end)
                .totalCost(DAILY_RATE.multiply(BigDecimal.valueOf(end.toEpochDay() - start.toEpochDay() + 1)))
                .deliveryAddress("Field").build());
    }

    /**
     * Models equipment that already carries other bookings: the status is already BOOKED, so a new reservation
     * does not change the equipment row and its optimistic @Version cannot serialize concurrent requests.
     */
    protected void markEquipmentAlreadyBooked() {
        equipment.setAvailabilityStatus(AvailabilityStatus.BOOKED);
        equipment = equipmentRepository.save(equipment);
    }

    protected long reservingBookingsFor(LocalDate start, LocalDate end) {
        return bookingRepository.findAll().stream()
                .filter(b -> b.getEquipment() != null && b.getEquipment().getId().equals(equipment.getId()))
                .filter(b -> b.getStatus() != BookingStatus.CANCELLED && b.getStatus() != BookingStatus.REJECTED
                        && b.getStatus() != BookingStatus.COMPLETED)
                .filter(b -> !b.getStartDate().isAfter(end) && !b.getEndDate().isBefore(start))
                .count();
    }

    /** Runs all tasks at the same moment; each result is either the returned value or the thrown exception. */
    protected static List<Object> runConcurrently(List<Callable<Object>> tasks) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch ready = new CountDownLatch(tasks.size());
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Object>> futures = new ArrayList<>();
        try {
            for (Callable<Object> task : tasks) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    go.await();
                    try {
                        return task.call();
                    } catch (Exception e) {
                        return e;
                    } finally {
                        SecurityContextHolder.clearContext();
                    }
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            go.countDown();
            List<Object> results = new ArrayList<>();
            for (Future<Object> future : futures) {
                results.add(future.get(60, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

    private static long successes(List<Object> results) {
        return results.stream().filter(r -> !(r instanceof Exception)).count();
    }

    // ---- BIZ-13: double booking ----------------------------------------------------------------------

    @Test
    void concurrentOverlappingCreationsReserveTheEquipmentExactlyOnce() throws Exception {
        markEquipmentAlreadyBooked();
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            Farmer farmer = newFarmer();
            LocalDate start = day(10 + (i % 2));          // 10..12 and 11..13 all overlap each other
            tasks.add(() -> createBookingAs(farmer, start, start.plusDays(2)));
        }

        List<Object> results = runConcurrently(tasks);

        assertThat(successes(results)).isEqualTo(1);
        assertThat(results).filteredOn(r -> r instanceof Exception)
                .allSatisfy(r -> assertThat((Exception) r).isInstanceOf(BadRequestException.class)
                        .hasMessageContaining("already reserved"));
        assertThat(reservingBookingsFor(day(10), day(13))).isEqualTo(1);
    }

    @Test
    void concurrentNonOverlappingCreationsAllSucceed() throws Exception {
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Farmer farmer = newFarmer();
            LocalDate start = day(40 + i * 3);            // [40-41], [43-44], ... adjacent but disjoint
            tasks.add(() -> createBookingAs(farmer, start, start.plusDays(1)));
        }

        List<Object> results = runConcurrently(tasks);

        assertThat(results).allSatisfy(r -> assertThat(r).isNotInstanceOf(Exception.class));
        assertThat(reservingBookingsFor(day(40), day(60))).isEqualTo(6);
    }

    @Test
    void concurrentAcceptanceOfOverlappingPendingBookingsConfirmsExactlyOne() throws Exception {
        // Two overlapping PENDING bookings can only exist from data created before the creation lock existed
        markEquipmentAlreadyBooked();
        Booking first = insertBooking(newFarmer(), BookingStatus.PENDING, day(80), day(82));
        Booking second = insertBooking(newFarmer(), BookingStatus.PENDING, day(81), day(83));

        List<Object> results = runConcurrently(List.of(
                () -> bookingService.acceptBooking(first.getId(), partner.getId()),
                () -> bookingService.acceptBooking(second.getId(), partner.getId())));

        assertThat(successes(results)).isEqualTo(1);
        long confirmed = List.of(first, second).stream()
                .map(b -> bookingRepository.findById(b.getId()).orElseThrow().getStatus())
                .filter(s -> s == BookingStatus.CONFIRMED).count();
        assertThat(confirmed).isEqualTo(1);
    }

    @Test
    void adminCannotReactivateACancelledBookingOntoReservedDates() {
        Booking cancelled = insertBooking(newFarmer(), BookingStatus.CANCELLED, day(100), day(102));
        insertBooking(newFarmer(), BookingStatus.CONFIRMED, day(101), day(103));

        assertThatThrownBy(() -> bookingService.updateBookingStatus(cancelled.getId(),
                BookingStatusUpdateRequest.builder().status(BookingStatus.CONFIRMED).build()))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("already reserved");
        assertThat(bookingRepository.findById(cancelled.getId()).orElseThrow().getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    // ---- BIZ-14: rejection transitions ----------------------------------------------------------------

    @Test
    void pendingBookingCanBeRejected() {
        Booking booking = insertBooking(newFarmer(), BookingStatus.PENDING, day(120), day(121));

        bookingService.rejectBooking(booking.getId(), partner.getId(), "Machine under repair");

        Booking saved = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(BookingStatus.REJECTED);
        assertThat(saved.getRejectionReason()).isEqualTo("Machine under repair");
    }

    @ParameterizedTest
    @EnumSource(value = BookingStatus.class, names = "PENDING", mode = EnumSource.Mode.EXCLUDE)
    void rejectionIsRefusedFromEveryNonPendingStatus(BookingStatus status) {
        Booking booking = insertBooking(newFarmer(), status, day(140), day(141));

        assertThatThrownBy(() -> bookingService.rejectBooking(booking.getId(), partner.getId(), "Too late"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("Only pending bookings can be rejected");
        // The admin status override must not bypass the rule either
        assertThatThrownBy(() -> bookingService.updateBookingStatus(booking.getId(),
                BookingStatusUpdateRequest.builder().status(BookingStatus.REJECTED).rejectionReason("Too late").build()))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("Only pending bookings can be rejected");

        Booking unchanged = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(status);
        assertThat(unchanged.getRejectionReason()).isNull();
    }

    @Test
    void concurrentAcceptAndRejectLeaveOneConsistentOutcome() throws Exception {
        Booking booking = insertBooking(newFarmer(), BookingStatus.PENDING, day(160), day(161));

        List<Object> results = runConcurrently(List.of(
                () -> bookingService.acceptBooking(booking.getId(), partner.getId()),
                () -> bookingService.rejectBooking(booking.getId(), partner.getId(), "Declined")));

        assertThat(successes(results)).isEqualTo(1);
        BookingStatus finalStatus = bookingRepository.findById(booking.getId()).orElseThrow().getStatus();
        assertThat(finalStatus).isIn(BookingStatus.CONFIRMED, BookingStatus.REJECTED);
        Object loser = results.stream().filter(r -> r instanceof Exception).findFirst().orElseThrow();
        assertThat((Exception) loser).isInstanceOf(BadRequestException.class)
                .hasMessageContaining(finalStatus == BookingStatus.CONFIRMED ? "Only pending bookings can be rejected" : "Cannot accept");
    }

    @Test
    void concurrentRejectAndCancelLeaveOneConsistentOutcome() throws Exception {
        Farmer farmer = newFarmer();
        Booking booking = insertBooking(farmer, BookingStatus.PENDING, day(180), day(181));

        List<Object> results = runConcurrently(List.of(
                () -> bookingService.rejectBooking(booking.getId(), partner.getId(), "Declined"),
                () -> bookingService.cancelBooking(booking.getId(), farmer.getId())));

        assertThat(successes(results)).isEqualTo(1);
        BookingStatus finalStatus = bookingRepository.findById(booking.getId()).orElseThrow().getStatus();
        assertThat(finalStatus).isIn(BookingStatus.REJECTED, BookingStatus.CANCELLED);
    }

    @Test
    void acceptanceCannotMoveAnInProgressBookingBackwards() {
        Booking booking = insertBooking(newFarmer(), BookingStatus.ON_THE_WAY, day(200), day(201));

        assertThatThrownBy(() -> bookingService.acceptBooking(booking.getId(), partner.getId()))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("Cannot accept");
        assertThat(bookingRepository.findById(booking.getId()).orElseThrow().getStatus()).isEqualTo(BookingStatus.ON_THE_WAY);
    }

    @Test
    void rejectedBookingCannotBeCancelled() {
        Farmer farmer = newFarmer();
        Booking booking = insertBooking(farmer, BookingStatus.REJECTED, day(220), day(221));

        assertThatThrownBy(() -> bookingService.cancelBooking(booking.getId(), farmer.getId()))
                .isInstanceOf(BadRequestException.class);
        assertThat(bookingRepository.findById(booking.getId()).orElseThrow().getStatus()).isEqualTo(BookingStatus.REJECTED);
    }

    @Test
    void unpaidPendingBookingCanBeCancelledByItsFarmer() {
        Farmer farmer = newFarmer();
        Booking booking = insertBooking(farmer, BookingStatus.PENDING, day(230), day(231));

        bookingService.cancelBooking(booking.getId(), farmer.getId());

        assertThat(bookingRepository.findById(booking.getId()).orElseThrow().getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(paymentRepository.findByBookingId(booking.getId())).isEmpty();
    }

    // ---- BIZ-20: payment amount, duplicates, authorization ------------------------------------------------

    private PaymentRequest payment(Booking booking, Farmer farmer, String amount) {
        return PaymentRequest.builder().bookingId(booking.getId()).farmerId(farmer.getId())
                .amount(amount != null ? new BigDecimal(amount) : null).paymentMethod(PaymentMethod.UPI).build();
    }

    @Test
    void clientAmountBelowOrAboveTheBookingTotalIsRejected() {
        Farmer farmer = newFarmer();
        Booking booking = insertBooking(farmer, BookingStatus.CONFIRMED, day(260), day(261));   // total 5000.00
        actAsFarmer(farmer);

        assertThatThrownBy(() -> paymentService.processPayment(payment(booking, farmer, "1.00")))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("must equal the booking total");
        assertThatThrownBy(() -> paymentService.processPayment(payment(booking, farmer, "999999.00")))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("must equal the booking total");
        assertThat(paymentRepository.findByBookingId(booking.getId())).isEmpty();
    }

    @Test
    void exactPaymentRecordsTheServerSideTotalAndDuplicatesAreIdempotent() {
        Farmer farmer = newFarmer();
        Booking booking = insertBooking(farmer, BookingStatus.CONFIRMED, day(280), day(281));
        actAsFarmer(farmer);

        PaymentResponse first = paymentService.processPayment(payment(booking, farmer, "5000"));
        PaymentResponse second = paymentService.processPayment(payment(booking, farmer, "5000.00"));

        assertThat(first.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(first.getAmount()).isEqualByComparingTo("5000.00");
        assertThat(second.getTransactionId()).isEqualTo(first.getTransactionId());
        assertThat(paymentRepository.findAll().stream().filter(p -> p.getBookingId().equals(booking.getId())).count()).isEqualTo(1);
    }

    @Test
    void paymentWithoutAClientAmountIsChargedTheBookingTotal() {
        Farmer farmer = newFarmer();
        Booking booking = insertBooking(farmer, BookingStatus.CONFIRMED, day(300), day(302));   // total 7500.00
        actAsFarmer(farmer);

        PaymentResponse response = paymentService.createPayment(PaymentCreateRequest.builder()
                .bookingId(booking.getId()).farmerId(farmer.getId()).build());

        assertThat(response.getAmount()).isEqualByComparingTo("7500.00");
        assertThat(paymentRepository.findByBookingId(booking.getId()).orElseThrow().getAmount()).isEqualByComparingTo("7500.00");
    }

    @Test
    void anotherFarmerCannotPayForTheBooking() {
        Farmer owner = newFarmer();
        Farmer other = newFarmer();
        Booking booking = insertBooking(owner, BookingStatus.CONFIRMED, day(320), day(321));
        actAsFarmer(other);

        assertThatThrownBy(() -> paymentService.processPayment(payment(booking, owner, "5000.00")))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(paymentRepository.findByBookingId(booking.getId())).isEmpty();
    }

    @Test
    void concurrentDuplicatePaymentsCreateExactlyOnePaymentRow() throws Exception {
        Farmer farmer = newFarmer();
        Booking booking = insertBooking(farmer, BookingStatus.CONFIRMED, day(340), day(341));
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            tasks.add(() -> {
                actAsFarmer(farmer);
                return paymentService.processPayment(payment(booking, farmer, "5000.00"));
            });
        }

        List<Object> results = runConcurrently(tasks);

        assertThat(paymentRepository.findAll().stream().filter(p -> p.getBookingId().equals(booking.getId())).count()).isEqualTo(1);
        assertThat(results).filteredOn(r -> !(r instanceof Exception))
                .allSatisfy(r -> assertThat(((PaymentResponse) r).getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS));
    }

    // ---- BIZ-20: payment eligibility (CONFIRMED only) ---------------------------------------------------

    @ParameterizedTest
    @EnumSource(value = BookingStatus.class, names = "CONFIRMED", mode = EnumSource.Mode.EXCLUDE)
    void paymentIsRefusedForEveryNonConfirmedStatus(BookingStatus status) {
        Farmer farmer = newFarmer();
        Booking booking = insertBooking(farmer, status, day(360), day(361));
        actAsFarmer(farmer);

        assertThatThrownBy(() -> paymentService.processPayment(payment(booking, farmer, "5000.00")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("status: " + status)
                .hasMessageContaining("only for confirmed bookings");
        assertThat(paymentRepository.findByBookingId(booking.getId())).isEmpty();
    }

    @Test
    void invoiceSplitsTheGstInclusiveTotalAndReportsNoRefund() {
        Farmer farmer = newFarmer();
        Booking booking = insertBooking(farmer, BookingStatus.CONFIRMED, day(370), day(371));   // total 5000.00
        payFor(booking, farmer);
        actAsFarmer(farmer);

        InvoiceResponse invoice = paymentService.generateInvoice(booking.getId());

        assertThat(invoice.getTotalAmount()).isEqualByComparingTo("5000.00");
        assertThat(invoice.getSubtotal()).isEqualByComparingTo("4237.29");     // 5000 / 1.18, HALF_UP
        assertThat(invoice.getGstAmount()).isEqualByComparingTo("762.71");     // total - taxable value
        assertThat(invoice.getSubtotal().add(invoice.getGstAmount())).isEqualByComparingTo(invoice.getTotalAmount());
        assertThat(invoice.getRefundStatus()).isEqualTo("NONE");
        assertThat(paymentService.getPaymentByBookingId(booking.getId()).getRefundStatus()).isEqualTo("NONE");
    }

    // ---- BIZ-20: refund initiation -----------------------------------------------------------------------

    /** Pays the booking as its farmer through the real payment path (server-side total). */
    protected Payment payFor(Booking booking, Farmer farmer) {
        actAsFarmer(farmer);
        try {
            paymentService.processPayment(payment(booking, farmer, null));
        } finally {
            SecurityContextHolder.clearContext();
        }
        return paymentRepository.findByBookingId(booking.getId()).orElseThrow();
    }

    /** A payment row written directly, e.g. a booking paid while still PENDING before payment was restricted. */
    protected Payment insertPayment(Booking booking, PaymentStatus status) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return paymentRepository.save(Payment.builder().bookingId(booking.getId()).farmerId(booking.getFarmerId())
                .partnerId(partner.getId()).amount(booking.getTotalCost()).paymentMethod(PaymentMethod.UPI)
                .transactionId("TXN-LEGACY-" + suffix).invoiceReference("INV-LEGACY-" + suffix)
                .paymentStatus(status).paymentDate(LocalDateTime.now()).build());
    }

    protected static void actAsAdmin() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "ADMIN_9", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    protected static final CurrentCaller ADMIN = new CurrentCaller(CurrentCaller.Kind.ADMIN, null);

    protected List<PaymentRefund> refundsFor(Booking booking) {
        return paymentRefundRepository.findAll().stream().filter(r -> r.getBookingId().equals(booking.getId())).toList();
    }

    protected PaymentRefund singleRefund(Booking booking) {
        List<PaymentRefund> refunds = refundsFor(booking);
        assertThat(refunds).hasSize(1);
        return refunds.get(0);
    }

    /** A CONFIRMED booking paid by its farmer and then cancelled by that farmer: one REFUND_PENDING obligation. */
    protected PaymentRefund paidAndCancelled(int dayOffset) {
        Farmer farmer = newFarmer();
        Booking booking = insertBooking(farmer, BookingStatus.CONFIRMED, day(dayOffset), day(dayOffset + 1));
        payFor(booking, farmer);
        bookingService.cancelBooking(booking.getId(), farmer.getId());
        return singleRefund(booking);
    }

    private static String reference() {
        return "UTR" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    @Test
    void paidBookingCancelledByItsFarmerGetsExactlyOneFullPendingRefund() {
        Farmer farmer = newFarmer();
        Booking booking = insertBooking(farmer, BookingStatus.CONFIRMED, day(380), day(381));   // total 5000.00
        Payment paid = payFor(booking, farmer);

        bookingService.cancelBooking(booking.getId(), farmer.getId());

        PaymentRefund refund = singleRefund(booking);
        assertThat(refund.getStatus()).isEqualTo(RefundStatus.REFUND_PENDING);
        assertThat(refund.getAmount()).isEqualByComparingTo(paid.getAmount()).isEqualByComparingTo("5000.00");
        assertThat(refund.getPayment().getId()).isEqualTo(paid.getId());
        assertThat(refund.getFarmerId()).isEqualTo(farmer.getId());
        assertThat(refund.getPartnerId()).isEqualTo(partner.getId());
        assertThat(refund.getInitiatedBy()).isEqualTo("FARMER_" + farmer.getId());
        assertThat(refund.getInitiatedAt()).isNotNull();
        assertThat(refund.getAttemptCount()).isEqualTo(1);
        assertThat(refund.getTransferReference()).isNull();
        assertThat(paymentRefundEventRepository.findByRefundIdOrderByOccurredAtAscIdAsc(refund.getId()))
                .singleElement().satisfies(e -> {
                    assertThat(e.getFromStatus()).isNull();
                    assertThat(e.getToStatus()).isEqualTo(RefundStatus.REFUND_PENDING);
                    assertThat(e.getActor()).isEqualTo("FARMER_" + farmer.getId());
                });

        // The original payment is preserved as collected
        Payment after = paymentRepository.findById(paid.getId()).orElseThrow();
        assertThat(after.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(after.getAmount()).isEqualByComparingTo("5000.00");
        assertThat(after.getTransactionId()).isEqualTo(paid.getTransactionId());
        assertThat(paymentService.getPaymentByBookingId(booking.getId()).getRefundStatus()).isEqualTo("REFUND_PENDING");
        assertThat(paymentService.getPaymentByBookingId(booking.getId()).getRefundAmount()).isEqualByComparingTo("5000.00");
    }

    @Test
    void adminCancellationAndAdminStatusOverrideOfPaidBookingsInitiateRefunds() {
        Farmer farmer = newFarmer();
        Booking viaCancel = insertBooking(farmer, BookingStatus.CONFIRMED, day(390), day(391));
        Booking viaOverride = insertBooking(farmer, BookingStatus.CONFIRMED, day(393), day(395));   // total 7500.00
        payFor(viaCancel, farmer);
        payFor(viaOverride, farmer);

        actAsAdmin();
        adminBookingService.cancelBooking(viaCancel.getId());
        adminBookingService.updateBookingStatus(viaOverride.getId(),
                BookingStatusUpdateRequest.builder().status(BookingStatus.CANCELLED).build());

        assertThat(singleRefund(viaCancel).getInitiatedBy()).isEqualTo("ADMIN_9");
        assertThat(singleRefund(viaCancel).getAmount()).isEqualByComparingTo("5000.00");
        assertThat(singleRefund(viaOverride).getInitiatedBy()).isEqualTo("ADMIN_9");
        assertThat(singleRefund(viaOverride).getAmount()).isEqualByComparingTo("7500.00");
    }

    @Test
    void paidPendingBookingRejectedByPartnerOrAdminGetsAFullRefund() {
        // Pre-policy data: payments recorded while the booking was still PENDING
        Booking byPartner = insertBooking(newFarmer(), BookingStatus.PENDING, day(400), day(401));
        Booking byAdmin = insertBooking(newFarmer(), BookingStatus.PENDING, day(403), day(403));    // total 2500.00
        Payment partnerPaid = insertPayment(byPartner, PaymentStatus.SUCCESS);
        insertPayment(byAdmin, PaymentStatus.SUCCESS);

        bookingService.rejectBooking(byPartner.getId(), partner.getId(), "Machine under repair");
        actAsAdmin();
        bookingService.updateBookingStatus(byAdmin.getId(), BookingStatusUpdateRequest.builder()
                .status(BookingStatus.REJECTED).rejectionReason("Duplicate request").build());

        PaymentRefund partnerRefund = singleRefund(byPartner);
        assertThat(partnerRefund.getStatus()).isEqualTo(RefundStatus.REFUND_PENDING);
        assertThat(partnerRefund.getAmount()).isEqualByComparingTo(partnerPaid.getAmount());
        assertThat(partnerRefund.getInitiatedBy()).isEqualTo("PARTNER_" + partner.getId());
        assertThat(partnerRefund.getInitiationReason()).contains("Machine under repair");
        assertThat(singleRefund(byAdmin).getAmount()).isEqualByComparingTo("2500.00");
        assertThat(singleRefund(byAdmin).getInitiatedBy()).isEqualTo("ADMIN_9");
    }

    @Test
    void unpaidCancellationsAndRejectionsCreateNoRefund() {
        Farmer farmer = newFarmer();
        Booking pendingCancelled = insertBooking(farmer, BookingStatus.PENDING, day(410), day(411));
        Booking confirmedCancelled = insertBooking(farmer, BookingStatus.CONFIRMED, day(413), day(414));
        Booking pendingRejected = insertBooking(farmer, BookingStatus.PENDING, day(416), day(417));
        Booking failedPaymentCancelled = insertBooking(farmer, BookingStatus.CONFIRMED, day(419), day(420));
        insertPayment(failedPaymentCancelled, PaymentStatus.FAILED);

        bookingService.cancelBooking(pendingCancelled.getId(), farmer.getId());
        bookingService.cancelBooking(confirmedCancelled.getId(), farmer.getId());
        bookingService.rejectBooking(pendingRejected.getId(), partner.getId(), "Unavailable");
        bookingService.cancelBooking(failedPaymentCancelled.getId(), farmer.getId());

        for (Booking booking : List.of(pendingCancelled, confirmedCancelled, pendingRejected, failedPaymentCancelled)) {
            assertThat(refundsFor(booking)).as("refunds for booking %s", booking.getId()).isEmpty();
        }
    }

    @Test
    void refundInitiationIsIdempotentAndOnlyRunsInsideTheBookingTransaction() {
        PaymentRefund refund = paidAndCancelled(430);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        Optional<PaymentRefund> again = tx.execute(status ->
                refundService.initiateFullRefundIfPaid(refund.getBookingId(), "repeat", "TEST"));

        assertThat(again).isPresent();
        assertThat(again.get().getId()).isEqualTo(refund.getId());
        assertThat(paymentRefundRepository.findAll().stream().filter(r -> r.getBookingId().equals(refund.getBookingId()))).hasSize(1);
        assertThat(paymentRefundEventRepository.findByRefundIdOrderByOccurredAtAscIdAsc(refund.getId())).hasSize(1);
        assertThatThrownBy(() -> refundService.initiateFullRefundIfPaid(refund.getBookingId(), "outside", "TEST"))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void concurrentCancellationsOfAPaidBookingCreateExactlyOneRefund() throws Exception {
        Farmer farmer = newFarmer();
        Booking booking = insertBooking(farmer, BookingStatus.CONFIRMED, day(440), day(441));
        payFor(booking, farmer);

        List<Object> results = runConcurrently(List.of(
                () -> bookingService.cancelBooking(booking.getId(), farmer.getId()),
                () -> { actAsAdmin(); return adminBookingService.cancelBooking(booking.getId()); },
                () -> { actAsAdmin(); return bookingService.updateBookingStatus(booking.getId(),
                        BookingStatusUpdateRequest.builder().status(BookingStatus.CANCELLED).build()); }));

        assertThat(successes(results)).isGreaterThanOrEqualTo(1);
        assertThat(bookingRepository.findById(booking.getId()).orElseThrow().getStatus()).isEqualTo(BookingStatus.CANCELLED);
        PaymentRefund refund = singleRefund(booking);
        assertThat(refund.getAmount()).isEqualByComparingTo("5000.00");
        assertThat(paymentRefundEventRepository.findByRefundIdOrderByOccurredAtAscIdAsc(refund.getId())).hasSize(1);
    }

    @Test
    void paymentRacingCancellationEndsUnpaidOrPaidWithOneFullRefund() throws Exception {
        int paidOutcomes = 0;
        for (int i = 0; i < 4; i++) {
            Farmer farmer = newFarmer();
            Booking booking = insertBooking(farmer, BookingStatus.CONFIRMED, day(450 + i * 3), day(451 + i * 3));

            List<Object> results = runConcurrently(List.of(
                    () -> { actAsFarmer(farmer); return paymentService.processPayment(payment(booking, farmer, null)); },
                    () -> bookingService.cancelBooking(booking.getId(), farmer.getId())));

            assertThat(results.get(1)).isNotInstanceOf(Exception.class);   // CONFIRMED -> CANCELLED always allowed
            assertThat(bookingRepository.findById(booking.getId()).orElseThrow().getStatus()).isEqualTo(BookingStatus.CANCELLED);
            Optional<Payment> payment = paymentRepository.findByBookingId(booking.getId());
            if (payment.isPresent()) {
                paidOutcomes++;
                assertThat(results.get(0)).isNotInstanceOf(Exception.class);
                assertThat(payment.get().getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
                PaymentRefund refund = singleRefund(booking);
                assertThat(refund.getStatus()).isEqualTo(RefundStatus.REFUND_PENDING);
                assertThat(refund.getAmount()).isEqualByComparingTo(payment.get().getAmount());
            } else {
                assertThat(results.get(0)).isInstanceOf(BadRequestException.class);
                assertThat(((Exception) results.get(0)).getMessage()).contains("status: CANCELLED");
                assertThat(refundsFor(booking)).isEmpty();
            }
        }
        System.out.println("payment-vs-cancel races where the payment committed first: " + paidOutcomes + "/4");
    }

    /**
     * Forces the ordering that free-running races rarely produce: a payment transaction holds the booking row lock
     * (the first step of payment processing) and records a SUCCESS payment while the cancellation queues on the
     * same lock. Once the payment commits, the cancellation must see it and owe a full refund.
     */
    @Test
    void cancellationWaitingBehindAnInFlightPaymentRefundsTheCommittedPayment() throws Exception {
        Farmer farmer = newFarmer();
        Booking booking = insertBooking(farmer, BookingStatus.CONFIRMED, day(560), day(561));   // total 5000.00
        CountDownLatch lockHeld = new CountDownLatch(1);
        long[] paymentLockReleasedAt = new long[1];
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> payer = executor.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                bookingRepository.findByIdWithLock(booking.getId()).orElseThrow();
                insertPayment(booking, PaymentStatus.SUCCESS);
                lockHeld.countDown();
                try {
                    Thread.sleep(1500);   // keep the lock while the cancellation blocks on it
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                paymentLockReleasedAt[0] = System.nanoTime();   // commit follows immediately
            }));
            assertThat(lockHeld.await(10, TimeUnit.SECONDS)).isTrue();
            Future<Long> cancel = executor.submit(() -> {
                bookingService.cancelBooking(booking.getId(), farmer.getId());
                return System.nanoTime();
            });

            payer.get(30, TimeUnit.SECONDS);
            long cancelledAt = cancel.get(30, TimeUnit.SECONDS);
            assertThat(cancelledAt).as("cancellation must wait for the payment's booking lock").isGreaterThan(paymentLockReleasedAt[0]);
        } finally {
            executor.shutdownNow();
        }

        assertThat(bookingRepository.findById(booking.getId()).orElseThrow().getStatus()).isEqualTo(BookingStatus.CANCELLED);
        Payment payment = paymentRepository.findByBookingId(booking.getId()).orElseThrow();
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        PaymentRefund refund = singleRefund(booking);
        assertThat(refund.getStatus()).isEqualTo(RefundStatus.REFUND_PENDING);
        assertThat(refund.getAmount()).isEqualByComparingTo(payment.getAmount()).isEqualByComparingTo("5000.00");
    }

    @Test
    void bookingWithARefundCannotBeReactivatedOrCompleted() {
        PaymentRefund refund = paidAndCancelled(470);
        actAsAdmin();

        for (BookingStatus target : List.of(BookingStatus.CONFIRMED, BookingStatus.PENDING, BookingStatus.COMPLETED)) {
            assertThatThrownBy(() -> bookingService.updateBookingStatus(refund.getBookingId(),
                    BookingStatusUpdateRequest.builder().status(target).build()))
                    .isInstanceOf(BadRequestException.class).hasMessageContaining("refund on record");
        }
        assertThat(bookingRepository.findById(refund.getBookingId()).orElseThrow().getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    // ---- BIZ-20: admin refund lifecycle -------------------------------------------------------------------

    @Test
    void onlyAnAdminCanViewOrChangeRefunds() {
        PaymentRefund refund = paidAndCancelled(480);
        for (CurrentCaller caller : List.of(
                new CurrentCaller(CurrentCaller.Kind.FARMER, refund.getFarmerId()),
                new CurrentCaller(CurrentCaller.Kind.PARTNER, refund.getPartnerId()),
                new CurrentCaller(CurrentCaller.Kind.OPERATOR, 1L))) {
            assertThatThrownBy(() -> refundService.completeRefund(refund.getId(), reference(), null, caller, "X"))
                    .isInstanceOf(ForbiddenException.class);
            assertThatThrownBy(() -> refundService.markRefundFailed(refund.getId(), "bounced", caller, "X"))
                    .isInstanceOf(ForbiddenException.class);
            assertThatThrownBy(() -> refundService.retryRefund(refund.getId(), null, caller, "X"))
                    .isInstanceOf(ForbiddenException.class);
            assertThatThrownBy(() -> refundService.getRefund(refund.getId(), caller)).isInstanceOf(ForbiddenException.class);
            assertThatThrownBy(() -> refundService.listRefunds(null, caller)).isInstanceOf(ForbiddenException.class);
        }
        assertThatThrownBy(() -> refundService.completeRefund(refund.getId(), reference(), null, null, "X"))
                .isInstanceOf(ForbiddenException.class);
        assertThat(paymentRefundRepository.findById(refund.getId()).orElseThrow().getStatus()).isEqualTo(RefundStatus.REFUND_PENDING);
    }

    @Test
    void completionRequiresAValidVerifiedTransferReference() {
        PaymentRefund refund = paidAndCancelled(490);
        for (String invalid : new String[]{null, "", "   ", "abc12", "UTR 12345678", "-UTR12345", "UTR#123456", "X".repeat(65)}) {
            assertThatThrownBy(() -> refundService.completeRefund(refund.getId(), invalid, null, ADMIN, "ADMIN_9"))
                    .as("reference %s", invalid)
                    .isInstanceOf(BadRequestException.class).hasMessageContaining("transfer reference");
        }
        PaymentRefund unchanged = paymentRefundRepository.findById(refund.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(RefundStatus.REFUND_PENDING);
        assertThat(unchanged.getTransferReference()).isNull();

        String utr = reference();
        RefundResponse done = refundService.completeRefund(refund.getId(), "  " + utr + "  ", "NEFT confirmed by bank", ADMIN, "ADMIN_9");

        assertThat(done.getStatus()).isEqualTo(RefundStatus.REFUNDED);
        assertThat(done.getTransferReference()).isEqualTo(utr);
        assertThat(done.getCompletedBy()).isEqualTo("ADMIN_9");
        assertThat(done.getCompletedAt()).isNotNull();
        assertThat(done.getAmount()).isEqualByComparingTo("5000.00");
        assertThat(done.getHistory()).extracting(RefundResponse.Event::getToStatus)
                .containsExactly(RefundStatus.REFUND_PENDING, RefundStatus.REFUNDED);
        assertThat(done.getHistory().get(1).getActor()).isEqualTo("ADMIN_9");
        assertThat(done.getHistory().get(1).getTransferReference()).isEqualTo(utr);

        // REFUNDED is terminal
        assertThatThrownBy(() -> refundService.completeRefund(refund.getId(), reference(), null, ADMIN, "ADMIN_9"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("REFUNDED");
        assertThatThrownBy(() -> refundService.markRefundFailed(refund.getId(), "late bounce", ADMIN, "ADMIN_9"))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> refundService.retryRefund(refund.getId(), null, ADMIN, "ADMIN_9"))
                .isInstanceOf(BadRequestException.class);

        // The original payment is untouched; the refund is reported alongside it
        Payment payment = paymentRepository.findByBookingId(refund.getBookingId()).orElseThrow();
        assertThat(payment.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getAmount()).isEqualByComparingTo("5000.00");
        assertThat(paymentService.getPaymentByBookingId(refund.getBookingId()).getRefundStatus()).isEqualTo("REFUNDED");
    }

    @Test
    void failedRefundStaysVisibleAndRetryReopensTheSameObligation() {
        PaymentRefund refund = paidAndCancelled(500);

        assertThatThrownBy(() -> refundService.markRefundFailed(refund.getId(), " ", ADMIN, "ADMIN_9"))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> refundService.retryRefund(refund.getId(), null, ADMIN, "ADMIN_9"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("REFUND_PENDING");

        RefundResponse failed = refundService.markRefundFailed(refund.getId(), "Beneficiary account closed", ADMIN, "ADMIN_9");
        assertThat(failed.getStatus()).isEqualTo(RefundStatus.REFUND_FAILED);
        assertThat(failed.getLastFailureReason()).isEqualTo("Beneficiary account closed");
        assertThat(failed.getLastFailedBy()).isEqualTo("ADMIN_9");
        assertThat(refundService.listRefunds(RefundStatus.REFUND_FAILED, ADMIN)).extracting(RefundResponse::getId).contains(refund.getId());
        assertThatThrownBy(() -> refundService.completeRefund(refund.getId(), reference(), null, ADMIN, "ADMIN_9"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("REFUND_FAILED");

        RefundResponse retried = refundService.retryRefund(refund.getId(), "Farmer shared new UPI ID", ADMIN, "ADMIN_7");
        assertThat(retried.getStatus()).isEqualTo(RefundStatus.REFUND_PENDING);
        assertThat(retried.getAttemptCount()).isEqualTo(2);

        RefundResponse done = refundService.completeRefund(refund.getId(), reference(), null, ADMIN, "ADMIN_7");
        assertThat(done.getStatus()).isEqualTo(RefundStatus.REFUNDED);
        assertThat(done.getId()).isEqualTo(refund.getId());
        assertThat(done.getLastFailureReason()).isEqualTo("Beneficiary account closed");   // failure history kept
        assertThat(done.getHistory()).extracting(RefundResponse.Event::getToStatus).containsExactly(
                RefundStatus.REFUND_PENDING, RefundStatus.REFUND_FAILED, RefundStatus.REFUND_PENDING, RefundStatus.REFUNDED);
        assertThat(done.getHistory()).extracting(RefundResponse.Event::getActor).containsExactly(
                "FARMER_" + refund.getFarmerId(), "ADMIN_9", "ADMIN_7", "ADMIN_7");
        assertThat(paymentRefundRepository.findAll().stream().filter(r -> r.getBookingId().equals(refund.getBookingId()))).hasSize(1);
    }

    @Test
    void aTransferReferenceCanCompleteOnlyOneRefund() {
        PaymentRefund first = paidAndCancelled(510);
        PaymentRefund second = paidAndCancelled(513);
        String utr = reference();

        refundService.completeRefund(first.getId(), utr, null, ADMIN, "ADMIN_9");

        assertThatThrownBy(() -> refundService.completeRefund(second.getId(), utr.toLowerCase(), null, ADMIN, "ADMIN_9"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("already recorded");
        assertThat(paymentRefundRepository.findById(second.getId()).orElseThrow().getStatus()).isEqualTo(RefundStatus.REFUND_PENDING);
    }

    @Test
    void concurrentCompletionsOfOneRefundSucceedExactlyOnce() throws Exception {
        PaymentRefund refund = paidAndCancelled(520);
        List<String> references = List.of(reference(), reference(), reference(), reference());
        List<Callable<Object>> tasks = new ArrayList<>();
        for (String utr : references) {
            tasks.add(() -> refundService.completeRefund(refund.getId(), utr, null, ADMIN, "ADMIN_9"));
        }

        List<Object> results = runConcurrently(tasks);

        assertThat(successes(results)).isEqualTo(1);
        assertThat(results).filteredOn(r -> r instanceof Exception)
                .allSatisfy(r -> assertThat((Exception) r).isInstanceOf(BadRequestException.class).hasMessageContaining("REFUNDED"));
        PaymentRefund done = paymentRefundRepository.findById(refund.getId()).orElseThrow();
        assertThat(done.getStatus()).isEqualTo(RefundStatus.REFUNDED);
        assertThat(references).contains(done.getTransferReference());
        assertThat(paymentRefundEventRepository.findByRefundIdOrderByOccurredAtAscIdAsc(refund.getId()))
                .filteredOn(e -> e.getToStatus() == RefundStatus.REFUNDED).hasSize(1);
    }

    @Test
    void concurrentCompletionsOfTwoRefundsWithOneReferenceSucceedExactlyOnce() throws Exception {
        PaymentRefund first = paidAndCancelled(530);
        PaymentRefund second = paidAndCancelled(533);
        String utr = reference();

        List<Object> results = runConcurrently(List.of(
                () -> refundService.completeRefund(first.getId(), utr, null, ADMIN, "ADMIN_9"),
                () -> refundService.completeRefund(second.getId(), utr, null, ADMIN, "ADMIN_9")));

        assertThat(successes(results)).isEqualTo(1);
        assertThat(results).filteredOn(r -> r instanceof Exception).singleElement()
                .satisfies(r -> assertThat((Exception) r).isInstanceOf(BadRequestException.class).hasMessageContaining("already recorded"));
        long refundedWithUtr = List.of(first, second).stream()
                .map(r -> paymentRefundRepository.findById(r.getId()).orElseThrow())
                .filter(r -> r.getStatus() == RefundStatus.REFUNDED && utr.equals(r.getTransferReference())).count();
        assertThat(refundedWithUtr).isEqualTo(1);
    }

    // ---- BIZ-20: revenue definitions ------------------------------------------------------------------------

    @Test
    void revenueIsGrossMinusCompletedRefundsWithPendingAndFailedReportedSeparately() {
        AdminDashboardStatsResponse adminBefore = adminDashboardService.getDashboardStats();
        AdminBookingStatsResponse bookingStatsBefore = adminBookingService.getStats();

        Farmer kept = newFarmer();
        Farmer refunded = newFarmer();
        Farmer failed = newFarmer();
        Booking keptBooking = insertBooking(kept, BookingStatus.CONFIRMED, day(540), day(541));          // 5000.00
        Booking refundedBooking = insertBooking(refunded, BookingStatus.CONFIRMED, day(543), day(545));  // 7500.00
        Booking failedBooking = insertBooking(failed, BookingStatus.CONFIRMED, day(547), day(547));      // 2500.00
        payFor(keptBooking, kept);
        payFor(refundedBooking, refunded);
        payFor(failedBooking, failed);
        bookingService.cancelBooking(refundedBooking.getId(), refunded.getId());
        bookingService.cancelBooking(failedBooking.getId(), failed.getId());

        // Pending refunds are not subtracted: no money has left yet
        PartnerEarningsSummary pending = paymentService.getPartnerEarningsSummary(partner.getId());
        assertThat(pending.getGrossCollected()).isEqualByComparingTo("15000.00");
        assertThat(pending.getTotalRealizedEarnings()).isEqualByComparingTo("15000.00");
        assertThat(pending.getRefundsPendingAmount()).isEqualByComparingTo("10000.00");
        assertThat(pending.getRefundsPendingCount()).isEqualTo(2);

        refundService.completeRefund(singleRefund(refundedBooking).getId(), reference(), null, ADMIN, "ADMIN_9");
        refundService.markRefundFailed(singleRefund(failedBooking).getId(), "UPI handle invalid", ADMIN, "ADMIN_9");

        PartnerEarningsSummary summary = paymentService.getPartnerEarningsSummary(partner.getId());
        assertThat(summary.getGrossCollected()).isEqualByComparingTo("15000.00");
        assertThat(summary.getTotalRealizedEarnings()).isEqualByComparingTo("7500.00");      // 15000 - 7500 refunded
        assertThat(summary.getCompletedTransactionCount()).isEqualTo(3);
        assertThat(summary.getRefundsCompletedAmount()).isEqualByComparingTo("7500.00");
        assertThat(summary.getRefundsCompletedCount()).isEqualTo(1);
        assertThat(summary.getRefundsFailedAmount()).isEqualByComparingTo("2500.00");         // still owed, not subtracted
        assertThat(summary.getRefundsFailedCount()).isEqualTo(1);
        assertThat(summary.getRefundsPendingAmount()).isEqualByComparingTo("0");

        PartnerEarningsDetailResponse detail = paymentService.getPartnerEarningsDetail(partner.getId());
        assertThat(detail.getTotalRevenue()).isEqualByComparingTo("7500.00");
        assertThat(detail.getCompletedPaymentsAmount()).isEqualByComparingTo("15000.00");
        assertThat(detail.getDailyEarnings()).isEqualByComparingTo("7500.00");     // paid and refunded today
        assertThat(detail.getMonthlyEarnings()).isEqualByComparingTo("7500.00");
        assertThat(detail.getYearlyEarnings()).isEqualByComparingTo("7500.00");
        assertThat(detail.getRefundsFailedAmount()).isEqualByComparingTo("2500.00");

        List<PartnerBookingRevenueReportDto> rows = paymentService.getPartnerBookingRevenueReport(partner.getId());
        assertThat(rows).filteredOn(r -> r.getBookingId().equals(keptBooking.getId())).singleElement()
                .satisfies(r -> assertThat(r.getRefundStatus()).isEqualTo("NONE"));
        assertThat(rows).filteredOn(r -> r.getBookingId().equals(refundedBooking.getId())).singleElement()
                .satisfies(r -> {
                    assertThat(r.getRefundStatus()).isEqualTo("REFUNDED");
                    assertThat(r.getRefundedAmount()).isEqualByComparingTo("7500.00");
                    assertThat(r.getAmount()).isEqualByComparingTo("7500.00");   // original payment preserved
                });
        assertThat(rows).filteredOn(r -> r.getBookingId().equals(failedBooking.getId())).singleElement()
                .satisfies(r -> {
                    assertThat(r.getRefundStatus()).isEqualTo("REFUND_FAILED");
                    assertThat(r.getRefundedAmount()).isEqualByComparingTo("0");
                });
        assertThat(paymentService.getPartnerEquipmentRevenueReport(partner.getId())).singleElement()
                .satisfies(r -> assertThat(r.getTotalRevenue()).isEqualByComparingTo("7500.00"));
        assertThat(paymentService.getPartnerCustomerRevenueReport(partner.getId()))
                .filteredOn(r -> r.getFarmerId().equals(refunded.getId())).singleElement()
                .satisfies(r -> assertThat(r.getTotalRevenue()).isEqualByComparingTo("0"));

        PartnerDashboardResponse dashboard = partnerService.getPartnerDashboard(partner.getId()).orElseThrow();
        assertThat(dashboard.getMonthlyRevenue()).isEqualByComparingTo("7500.00");
        assertThat(dashboard.getMonthlyRevenueChart().get(dashboard.getMonthlyRevenueChart().size() - 1).getRevenue())
                .isEqualByComparingTo("7500.00");

        // Platform-wide admin figures use the same definitions (compared as deltas: other tests share the database)
        AdminDashboardStatsResponse adminAfter = adminDashboardService.getDashboardStats();
        assertThat(adminAfter.getGrossCollected().subtract(adminBefore.getGrossCollected())).isEqualByComparingTo("15000.00");
        assertThat(adminAfter.getTotalRevenue().subtract(adminBefore.getTotalRevenue())).isEqualByComparingTo("7500.00");
        assertThat(adminAfter.getRefundsCompletedAmount().subtract(adminBefore.getRefundsCompletedAmount())).isEqualByComparingTo("7500.00");
        assertThat(adminAfter.getRefundsFailedAmount().subtract(adminBefore.getRefundsFailedAmount())).isEqualByComparingTo("2500.00");
        AdminBookingStatsResponse bookingStatsAfter = adminBookingService.getStats();
        assertThat(bookingStatsAfter.getTotalRevenue().subtract(bookingStatsBefore.getTotalRevenue())).isEqualByComparingTo("7500.00");
        assertThat(bookingStatsAfter.getTotalRevenue()).isEqualByComparingTo(adminAfter.getTotalRevenue());
    }
}
