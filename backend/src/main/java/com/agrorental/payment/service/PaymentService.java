package com.agrorental.payment.service;

import com.agrorental.booking.dto.BookingResponse;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.booking.service.BookingService;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.farmer.dto.FarmerProfileResponse;
import com.agrorental.farmer.service.FarmerProfileService;
import com.agrorental.notification.service.NotificationService;
import com.agrorental.payment.dto.*;
import com.agrorental.payment.entity.Payment;
import com.agrorental.payment.entity.PaymentMethod;
import com.agrorental.payment.entity.PaymentRefund;
import com.agrorental.payment.entity.PaymentStatus;
import com.agrorental.payment.entity.RefundStatus;
import com.agrorental.payment.repository.PaymentRefundRepository;
import com.agrorental.payment.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.agrorental.security.principal.FarmerPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingService bookingService;
    private final FarmerProfileService farmerProfileService;
    private final PaymentTransactionHelper paymentTransactionHelper;
    
    @Autowired(required = false)
    private NotificationService notificationService;

    /** Refund records (BIZ-20). Always injected by Spring; null only when constructed manually in unit tests. */
    @Autowired
    private PaymentRefundRepository paymentRefundRepository;

    private List<PaymentRefund> refundsForPartner(Long partnerId) {
        return paymentRefundRepository != null ? paymentRefundRepository.findByPartnerId(partnerId) : List.of();
    }

    private Optional<PaymentRefund> refundForPayment(Long paymentId) {
        return paymentRefundRepository != null && paymentId != null
                ? paymentRefundRepository.findByPaymentId(paymentId) : Optional.empty();
    }

    private static String refundStatusName(Optional<PaymentRefund> refund) {
        return refund.map(r -> r.getStatus().name()).orElse("NONE");
    }

    public PaymentService(PaymentRepository paymentRepository,
                          BookingService bookingService,
                          FarmerProfileService farmerProfileService) {
        this(paymentRepository, bookingService, farmerProfileService, new PaymentTransactionHelper(paymentRepository));
    }

    @Autowired
    public PaymentService(PaymentRepository paymentRepository,
                          BookingService bookingService,
                          FarmerProfileService farmerProfileService,
                          PaymentTransactionHelper paymentTransactionHelper) {
        this.paymentRepository = paymentRepository;
        this.bookingService = bookingService;
        this.farmerProfileService = farmerProfileService;
        this.paymentTransactionHelper = paymentTransactionHelper != null
                ? paymentTransactionHelper
                : new PaymentTransactionHelper(paymentRepository);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PaymentResponse processPayment(PaymentRequest request) {
        BookingResponse booking = bookingService.getBookingById(request.getBookingId());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            // An admin records the payment for the booking's own farmer, never a caller-supplied one
            request.setFarmerId(booking.getFarmerId());
        } else if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            // Any other authenticated caller must be the farmer who owns the booking (partners/operators never pass)
            if (!(auth.getPrincipal() instanceof FarmerPrincipal farmer) || !farmer.getId().equals(booking.getFarmerId())) {
                throw new AccessDeniedException("Access is denied. You are not authorized to pay for another farmer's booking.");
            }
            request.setFarmerId(farmer.getId());
        } else if (request.getFarmerId() == null || !request.getFarmerId().equals(booking.getFarmerId())) {
            // Internal (unauthenticated) callers must name the booking's own farmer; a missing ID no longer skips the check
            throw new AccessDeniedException("Access is denied. You are not authorized to pay for another farmer's booking.");
        }

        // Policy (checked after authorization, so non-owners learn nothing about the booking): payment is accepted only for a CONFIRMED booking (re-checked under the booking lock below)
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BadRequestException("Cannot process payment for a booking with status: " + booking.getStatus()
                    + ". Payment is accepted only for confirmed bookings.");
        }

        // The amount due is the server-side booking total; a client amount is only accepted if it matches exactly
        if (booking.getTotalCost() == null) {
            throw new BadRequestException("Booking total is not available; payment cannot be processed.");
        }
        if (request.getAmount() != null && request.getAmount().compareTo(booking.getTotalCost()) != 0) {
            throw new BadRequestException("Payment amount must equal the booking total of ₹" + booking.getTotalCost());
        }
        request.setAmount(booking.getTotalCost());

        Optional<Payment> existingPayment = paymentRepository.findByBookingId(request.getBookingId());
        if (existingPayment.isPresent() && existingPayment.get().getPaymentStatus() == PaymentStatus.SUCCESS) {
            return mapToResponse(existingPayment.get());
        }

        Payment saved;
        try {
            saved = paymentTransactionHelper.processPaymentInTransaction(request, booking);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Database constraint collision during payment creation for booking {}: {}",
                    request.getBookingId(), ex.getMessage());

            // Safely recover: check if another concurrent thread committed the winning payment
            Optional<Payment> winningPayment = paymentTransactionHelper.findByBookingIdInNewTransaction(request.getBookingId());
            if (winningPayment.isPresent() && winningPayment.get().getPaymentStatus() == PaymentStatus.SUCCESS) {
                log.info("Concurrent payment resolved idempotently for booking ID {}. Returning winning transaction: {}",
                        request.getBookingId(), winningPayment.get().getTransactionId());
                return mapToResponse(winningPayment.get());
            }

            // Unrelated constraint collision or winning payment is not SUCCESS - rethrow
            throw ex;
        }

        if (notificationService != null && booking != null) {
            try {
                notificationService.sendNotification(
                        "FARMER",
                        booking.getFarmerId(),
                        "Payment Successful",
                        "Payment of ₹" + saved.getAmount() + " for booking #" + booking.getId() + " was successful.",
                        "PAYMENT_SUCCESS",
                        booking.getId()
                );
                notificationService.sendNotification(
                        "PARTNER",
                        booking.getPartnerId(),
                        "Payment Received",
                        "Payment of ₹" + saved.getAmount() + " for booking #" + booking.getId() + " was successfully received.",
                        "PAYMENT_RECEIVED",
                        booking.getId()
                );
            } catch (Exception ignored) {}
        }

        return mapToResponse(saved);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PaymentResponse createPayment(PaymentCreateRequest request) {
        PaymentRequest req = PaymentRequest.builder()
                .bookingId(request.getBookingId())
                .farmerId(request.getFarmerId())
                .amount(request.getAmount())
                .paymentMethod(PaymentMethod.UPI)
                .build();
        return processPayment(req);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + id));
        return mapToResponse(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByBookingId(Long bookingId) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("No payment transaction found for booking ID: " + bookingId));
        return mapToResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getFarmerPayments(Long farmerId) {
        return paymentRepository.findByFarmerIdOrderByPaymentDateDesc(farmerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByFarmer(Long farmerId) {
        return getFarmerPayments(farmerId);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByPartner(Long partnerId) {
        return paymentRepository.findByPartnerIdOrderByCreatedAtDesc(partnerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PartnerEarningsSummary getPartnerEarningsSummary(Long partnerId) {
        BigDecimal gross = paymentRepository.sumAmountByPartnerIdAndStatus(partnerId, PaymentStatus.SUCCESS);
        long count = paymentRepository.countByPartnerIdAndStatus(partnerId, PaymentStatus.SUCCESS);
        RevenueCalculator.Totals totals = RevenueCalculator.partnerTotals(gross, partnerId, paymentRefundRepository);
        return PartnerEarningsSummary.builder()
                .partnerId(partnerId)
                // Realized earnings are NET of completed refunds; pending/failed refunds are reported separately
                .totalRealizedEarnings(totals.netRevenue())
                .completedTransactionCount(count)
                .grossCollected(totals.grossCollected())
                .refundsPendingAmount(totals.refundsPendingAmount())
                .refundsPendingCount(totals.refundsPendingCount())
                .refundsFailedAmount(totals.refundsFailedAmount())
                .refundsFailedCount(totals.refundsFailedCount())
                .refundsCompletedAmount(totals.refundsCompletedAmount())
                .refundsCompletedCount(totals.refundsCompletedCount())
                .build();
    }

    /**
     * FR-19: Computes detailed realized earnings breakdowns (Daily, Weekly, Monthly, Yearly, Total, Pending, Completed).
     */
    @Transactional(readOnly = true)
    public PartnerEarningsDetailResponse getPartnerEarningsDetail(Long partnerId) {
        List<Payment> allPartnerPayments = paymentRepository.findByPartnerIdOrderByCreatedAtDesc(partnerId);
        List<Payment> successfulPayments = allPartnerPayments.stream()
                .filter(p -> p.getPaymentStatus() == PaymentStatus.SUCCESS)
                .toList();
        // Period and trend figures are NET: collections by payment date minus refunds completed in the period
        List<PaymentRefund> partnerRefunds = refundsForPartner(partnerId);
        RevenueCalculator.Totals totals = RevenueCalculator.totals(allPartnerPayments, partnerRefunds);
        LocalDateTime openEnd = LocalDateTime.now().plusYears(100);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfToday = now.toLocalDate().atStartOfDay();
        LocalDateTime startOfWeek = now.toLocalDate().minusDays(now.getDayOfWeek().getValue() - 1).atStartOfDay();
        LocalDateTime startOfMonth = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
        LocalDateTime startOfYear = now.toLocalDate().withDayOfYear(1).atStartOfDay();

        BigDecimal daily = RevenueCalculator.netInPeriod(successfulPayments, partnerRefunds, startOfToday, openEnd);

        BigDecimal weekly = RevenueCalculator.netInPeriod(successfulPayments, partnerRefunds, startOfWeek, openEnd);

        BigDecimal monthly = RevenueCalculator.netInPeriod(successfulPayments, partnerRefunds, startOfMonth, openEnd);

        BigDecimal yearly = RevenueCalculator.netInPeriod(successfulPayments, partnerRefunds, startOfYear, openEnd);

        BigDecimal total = totals.netRevenue();

        // Daily trend: last 7 days
        List<PartnerEarningsDetailResponse.TimeRevenueEntry> dailyTrend = new java.util.ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            java.time.LocalDate d = now.toLocalDate().minusDays(i);
            LocalDateTime dayStart = d.atStartOfDay();
            LocalDateTime dayEnd = d.plusDays(1).atStartOfDay();
            List<Payment> dayPayments = successfulPayments.stream()
                    .filter(p -> p.getPaymentDate() != null && !p.getPaymentDate().isBefore(dayStart) && p.getPaymentDate().isBefore(dayEnd))
                    .toList();
            BigDecimal dayAmt = RevenueCalculator.netInPeriod(successfulPayments, partnerRefunds, dayStart, dayEnd);
            dailyTrend.add(new PartnerEarningsDetailResponse.TimeRevenueEntry(d.getDayOfWeek().name().substring(0, 3), dayAmt, dayPayments.size()));
        }

        // Weekly trend: last 4 weeks
        List<PartnerEarningsDetailResponse.TimeRevenueEntry> weeklyTrend = new java.util.ArrayList<>();
        for (int i = 3; i >= 0; i--) {
            java.time.LocalDate wStart = now.toLocalDate().minusWeeks(i).minusDays(now.getDayOfWeek().getValue() - 1);
            java.time.LocalDate wEnd = wStart.plusDays(7);
            List<Payment> weekPayments = successfulPayments.stream()
                    .filter(p -> p.getPaymentDate() != null && !p.getPaymentDate().toLocalDate().isBefore(wStart) && p.getPaymentDate().toLocalDate().isBefore(wEnd))
                    .toList();
            BigDecimal wAmt = RevenueCalculator.netInPeriod(successfulPayments, partnerRefunds, wStart.atStartOfDay(), wEnd.atStartOfDay());
            weeklyTrend.add(new PartnerEarningsDetailResponse.TimeRevenueEntry("Wk " + (4 - i), wAmt, weekPayments.size()));
        }

        // Monthly trend: last 6 months
        List<PartnerEarningsDetailResponse.TimeRevenueEntry> monthlyTrend = new java.util.ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            java.time.YearMonth ym = java.time.YearMonth.from(now.toLocalDate().minusMonths(i));
            List<Payment> monthPayments = successfulPayments.stream()
                    .filter(p -> p.getPaymentDate() != null && java.time.YearMonth.from(p.getPaymentDate().toLocalDate()).equals(ym))
                    .toList();
            BigDecimal mAmt = RevenueCalculator.netInPeriod(successfulPayments, partnerRefunds,
                    ym.atDay(1).atStartOfDay(), ym.plusMonths(1).atDay(1).atStartOfDay());
            monthlyTrend.add(new PartnerEarningsDetailResponse.TimeRevenueEntry(ym.getMonth().name().substring(0, 3), mAmt, monthPayments.size()));
        }

        List<Payment> pendingPayments = allPartnerPayments.stream()
                .filter(p -> p.getPaymentStatus() == PaymentStatus.PENDING)
                .toList();

        BigDecimal pendingAmount = pendingPayments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return PartnerEarningsDetailResponse.builder()
                .partnerId(partnerId)
                .dailyEarnings(daily)
                .weeklyEarnings(weekly)
                .monthlyEarnings(monthly)
                .yearlyEarnings(yearly)
                .totalRevenue(total)
                // Gross amount of completed payments (original collections)
                .completedPaymentsAmount(totals.grossCollected())
                .completedPaymentsCount(successfulPayments.size())
                .pendingPaymentsAmount(pendingAmount)
                .pendingPaymentsCount(pendingPayments.size())
                .grossCollected(totals.grossCollected())
                .refundsPendingAmount(totals.refundsPendingAmount())
                .refundsPendingCount(totals.refundsPendingCount())
                .refundsFailedAmount(totals.refundsFailedAmount())
                .refundsFailedCount(totals.refundsFailedCount())
                .refundsCompletedAmount(totals.refundsCompletedAmount())
                .refundsCompletedCount(totals.refundsCompletedCount())
                .dailyTrend(dailyTrend)
                .weeklyTrend(weeklyTrend)
                .monthlyTrend(monthlyTrend)
                .build();
    }

    /**
     * FR-19 Report 1: Itemized Booking Revenue Report
     */
    @Transactional(readOnly = true)
    public List<PartnerBookingRevenueReportDto> getPartnerBookingRevenueReport(Long partnerId) {
        List<Payment> payments = paymentRepository.findByPartnerIdOrderByCreatedAtDesc(partnerId);
        java.util.Map<Long, PaymentRefund> refundByPayment = refundsForPartner(partnerId).stream()
                .filter(r -> r.getPayment() != null)
                .collect(Collectors.toMap(r -> r.getPayment().getId(), r -> r, (a, b) -> a));
        return payments.stream().map(p -> {
            PaymentRefund refund = refundByPayment.get(p.getId());
            String equipName = "Agricultural Machinery";
            Long equipId = null;
            String farmerName = "Farmer #" + p.getFarmerId();
            String farmerMobile = "N/A";

            try {
                BookingResponse b = bookingService.getBookingById(p.getBookingId());
                if (b != null) {
                    if (b.getEquipmentName() != null) equipName = b.getEquipmentName();
                    equipId = b.getEquipmentId();
                    if (b.getFarmerName() != null) farmerName = b.getFarmerName();
                    if (b.getFarmerMobile() != null) farmerMobile = b.getFarmerMobile();
                }
            } catch (Exception ignored) {}

            return PartnerBookingRevenueReportDto.builder()
                    .bookingId(p.getBookingId())
                    .transactionId(p.getTransactionId())
                    .invoiceReference(p.getInvoiceReference())
                    .equipmentId(equipId)
                    .equipmentName(equipName)
                    .farmerId(p.getFarmerId())
                    .farmerName(farmerName)
                    .farmerMobile(farmerMobile)
                    .paymentDate(p.getPaymentDate())
                    .amount(p.getAmount())
                    .paymentMethod(p.getPaymentMethod() != null ? p.getPaymentMethod().name() : "UPI")
                    .paymentStatus(p.getPaymentStatus() != null ? p.getPaymentStatus().name() : "SUCCESS")
                    .refundStatus(refund != null ? refund.getStatus().name() : "NONE")
                    .refundedAmount(refund != null && refund.getStatus() == RefundStatus.REFUNDED ? refund.getAmount() : BigDecimal.ZERO)
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * FR-19 Report 2: Equipment-wise Revenue Report
     */
    @Transactional(readOnly = true)
    public List<PartnerEquipmentRevenueReportDto> getPartnerEquipmentRevenueReport(Long partnerId) {
        List<Payment> payments = paymentRepository.findByPartnerIdOrderByCreatedAtDesc(partnerId);
        List<PaymentRefund> partnerRefunds = refundsForPartner(partnerId);
        java.util.Map<Long, java.util.List<Payment>> groupedByEquip = new java.util.HashMap<>();
        java.util.Map<Long, BookingResponse> equipBookingMap = new java.util.HashMap<>();

        for (Payment p : payments) {
            try {
                BookingResponse b = bookingService.getBookingById(p.getBookingId());
                if (b != null && b.getEquipmentId() != null) {
                    groupedByEquip.computeIfAbsent(b.getEquipmentId(), k -> new java.util.ArrayList<>()).add(p);
                    equipBookingMap.putIfAbsent(b.getEquipmentId(), b);
                }
            } catch (Exception ignored) {}
        }

        List<PartnerEquipmentRevenueReportDto> result = new java.util.ArrayList<>();
        for (java.util.Map.Entry<Long, java.util.List<Payment>> entry : groupedByEquip.entrySet()) {
            Long eqId = entry.getKey();
            List<Payment> pList = entry.getValue();
            BookingResponse b = equipBookingMap.get(eqId);

            BigDecimal rev = RevenueCalculator.netForPayments(pList, partnerRefunds);

            result.add(PartnerEquipmentRevenueReportDto.builder()
                    .equipmentId(eqId)
                    .equipmentName(b != null ? b.getEquipmentName() : "Machinery #" + eqId)
                    .category(b != null ? b.getEquipmentCategory() : "TRACTOR")
                    .brand(b != null ? b.getEquipmentName() : "Generic")
                    .model("Fleet Unit")
                    .totalBookings(pList.size())
                    .totalRevenue(rev)
                    .dailyRentalPrice(BigDecimal.valueOf(1500))
                    .primaryImageUrl(b != null ? b.getPrimaryImageUrl() : null)
                    .build());
        }
        return result;
    }

    /**
     * FR-19 Report 3: Customer-wise Revenue Report
     */
    @Transactional(readOnly = true)
    public List<PartnerCustomerRevenueReportDto> getPartnerCustomerRevenueReport(Long partnerId) {
        List<Payment> payments = paymentRepository.findByPartnerIdOrderByCreatedAtDesc(partnerId);
        List<PaymentRefund> partnerRefunds = refundsForPartner(partnerId);
        java.util.Map<Long, java.util.List<Payment>> groupedByFarmer = new java.util.HashMap<>();

        for (Payment p : payments) {
            groupedByFarmer.computeIfAbsent(p.getFarmerId(), k -> new java.util.ArrayList<>()).add(p);
        }

        List<PartnerCustomerRevenueReportDto> result = new java.util.ArrayList<>();
        for (java.util.Map.Entry<Long, java.util.List<Payment>> entry : groupedByFarmer.entrySet()) {
            Long farmerId = entry.getKey();
            List<Payment> pList = entry.getValue();

            BigDecimal rev = RevenueCalculator.netForPayments(pList, partnerRefunds);

            String farmerName = "Farmer #" + farmerId;
            String mobile = "9876543211";
            String email = "farmer" + farmerId + "@agrorent.in";
            String loc = "Maharashtra, India";

            try {
                FarmerProfileResponse profile = farmerProfileService.getProfile(farmerId);
                if (profile != null) {
                    farmerName = profile.getFullName();
                    mobile = profile.getMobileNumber();
                    email = profile.getEmail();
                    loc = profile.getAddress();
                }
            } catch (Exception ignored) {}

            result.add(PartnerCustomerRevenueReportDto.builder()
                    .farmerId(farmerId)
                    .farmerName(farmerName)
                    .mobileNumber(mobile)
                    .email(email)
                    .location(loc)
                    .totalBookings(pList.size())
                    .totalRevenue(rev)
                    .build());
        }
        return result;
    }

    @Transactional(readOnly = true)
    public InvoiceResponse generateInvoice(Long bookingId) {
        BookingResponse booking = bookingService.getBookingById(bookingId);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (!isAdmin) {
                Object principal = auth.getPrincipal();
                if (principal instanceof FarmerPrincipal farmerPrincipal) {
                    if (booking.getFarmerId() != null && !booking.getFarmerId().equals(farmerPrincipal.getId())) {
                        throw new AccessDeniedException("Access is denied. You are not authorized to view another farmer's invoice.");
                    }
                }
            }
        }

        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("No payment transaction found for booking ID: " + bookingId));

        FarmerProfileResponse farmer = null;
        try {
            farmer = farmerProfileService.getProfile(booking.getFarmerId());
        } catch (Exception e) {
            // Fallback
        }

        long days = 1;
        if (booking.getStartDate() != null && booking.getEndDate() != null) {
            days = Math.max(1, ChronoUnit.DAYS.between(booking.getStartDate(), booking.getEndDate()) + 1);
        }

        BigDecimal total = payment.getAmount() != null ? payment.getAmount() : (booking.getTotalCost() != null ? booking.getTotalCost() : BigDecimal.ZERO);
        // Booking totals include 18% GST: taxable value = total / 1.18 (half-up to paise), GST = remainder
        GstCalculator.Split gstSplit = GstCalculator.splitInclusiveTotal(total);
        BigDecimal subtotal = gstSplit.taxableValue();
        BigDecimal gst = gstSplit.gstAmount();
        BigDecimal rate = days > 0 ? subtotal.divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP) : subtotal;

        String farmerName = farmer != null && farmer.getFullName() != null
                ? farmer.getFullName()
                : (booking.getFarmerName() != null ? booking.getFarmerName() : "Farmer #" + booking.getFarmerId());
        String farmerMobile = farmer != null && farmer.getMobileNumber() != null
                ? farmer.getMobileNumber()
                : (booking.getFarmerMobile() != null ? booking.getFarmerMobile() : "N/A");
        String partnerName = booking.getPartnerBusinessName() != null
                ? booking.getPartnerBusinessName()
                : (booking.getPartnerName() != null ? booking.getPartnerName() : "Partner #" + (booking.getPartnerId() != null ? booking.getPartnerId() : 1));

        Optional<PaymentRefund> refund = refundForPayment(payment.getId());
        return InvoiceResponse.builder()
                .invoiceReference(payment.getInvoiceReference())
                .transactionId(payment.getTransactionId())
                .bookingId(booking.getId())
                .farmerId(booking.getFarmerId())
                .farmerName(farmerName)
                .farmerMobile(farmerMobile)
                .equipmentName(booking.getEquipmentName() != null ? booking.getEquipmentName() : "Agricultural Equipment")
                .equipmentCategory(booking.getEquipmentCategory() != null ? booking.getEquipmentCategory() : "EQUIPMENT")
                .partnerName(partnerName)
                .bookingStartDate(booking.getStartDate())
                .bookingEndDate(booking.getEndDate())
                .rentalRatePerDay(rate)
                .rentalDays(days)
                .subtotal(subtotal)
                .gstAmount(gst)
                .totalAmount(total)
                .paymentMethod(payment.getPaymentMethod())
                .paymentDate(payment.getPaymentDate())
                .status(payment.getPaymentStatus() != null ? payment.getPaymentStatus().name() : "SUCCESS")
                .bookingStatus(booking.getStatus() != null ? booking.getStatus().name() : null)
                .refundStatus(refundStatusName(refund))
                .refundAmount(refund.map(PaymentRefund::getAmount).orElse(null))
                .build();
    }

    private PaymentResponse mapToResponse(Payment payment) {
        Optional<PaymentRefund> refund = refundForPayment(payment.getId());
        return PaymentResponse.builder()
                .id(payment.getId())
                .bookingId(payment.getBookingId())
                .farmerId(payment.getFarmerId())
                .partnerId(payment.getPartnerId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .paymentReference(payment.getPaymentReference() != null ? payment.getPaymentReference() : payment.getTransactionId())
                .transactionId(payment.getTransactionId())
                .paymentStatus(payment.getPaymentStatus())
                .paymentDate(payment.getPaymentDate())
                .createdAt(payment.getCreatedAt() != null ? payment.getCreatedAt() : payment.getPaymentDate())
                .updatedAt(payment.getUpdatedAt() != null ? payment.getUpdatedAt() : payment.getPaymentDate())
                .invoiceReference(payment.getInvoiceReference())
                .refundStatus(refundStatusName(refund))
                .refundAmount(refund.map(PaymentRefund::getAmount).orElse(null))
                .build();
    }
}
