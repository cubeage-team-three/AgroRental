package com.agrorental.payment.service;

import com.agrorental.booking.dto.BookingResponse;
import com.agrorental.booking.service.BookingService;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.farmer.dto.FarmerProfileResponse;
import com.agrorental.farmer.service.FarmerProfileService;
import com.agrorental.notification.service.NotificationService;
import com.agrorental.payment.dto.*;
import com.agrorental.payment.entity.Payment;
import com.agrorental.payment.entity.PaymentMethod;
import com.agrorental.payment.entity.PaymentStatus;
import com.agrorental.payment.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingService bookingService;
    private final FarmerProfileService farmerProfileService;
    
    @Autowired(required = false)
    private NotificationService notificationService;

    public PaymentService(PaymentRepository paymentRepository,
                          BookingService bookingService,
                          FarmerProfileService farmerProfileService) {
        this.paymentRepository = paymentRepository;
        this.bookingService = bookingService;
        this.farmerProfileService = farmerProfileService;
    }

    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        BookingResponse booking = bookingService.getBookingById(request.getBookingId());

        String transactionId = "TXN-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String invoiceRef = "INV-2026-" + String.format("%05d", System.currentTimeMillis() % 100000);

        Payment payment = Payment.builder()
                .bookingId(request.getBookingId())
                .farmerId(request.getFarmerId())
                .partnerId(booking != null ? booking.getPartnerId() : null)
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : PaymentMethod.UPI)
                .paymentReference(transactionId)
                .transactionId(transactionId)
                .paymentStatus(PaymentStatus.SUCCESS)
                .paymentDate(LocalDateTime.now())
                .invoiceReference(invoiceRef)
                .build();

        Payment saved = paymentRepository.save(payment);

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

    @Transactional
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
        BigDecimal total = paymentRepository.sumAmountByPartnerIdAndStatus(partnerId, PaymentStatus.SUCCESS);
        long count = paymentRepository.countByPartnerIdAndStatus(partnerId, PaymentStatus.SUCCESS);
        return PartnerEarningsSummary.builder()
                .partnerId(partnerId)
                .totalRealizedEarnings(total != null ? total : BigDecimal.ZERO)
                .completedTransactionCount(count)
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

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfToday = now.toLocalDate().atStartOfDay();
        LocalDateTime startOfWeek = now.toLocalDate().minusDays(now.getDayOfWeek().getValue() - 1).atStartOfDay();
        LocalDateTime startOfMonth = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
        LocalDateTime startOfYear = now.toLocalDate().withDayOfYear(1).atStartOfDay();

        BigDecimal daily = successfulPayments.stream()
                .filter(p -> p.getPaymentDate() != null && !p.getPaymentDate().isBefore(startOfToday))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal weekly = successfulPayments.stream()
                .filter(p -> p.getPaymentDate() != null && !p.getPaymentDate().isBefore(startOfWeek))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal monthly = successfulPayments.stream()
                .filter(p -> p.getPaymentDate() != null && !p.getPaymentDate().isBefore(startOfMonth))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal yearly = successfulPayments.stream()
                .filter(p -> p.getPaymentDate() != null && !p.getPaymentDate().isBefore(startOfYear))
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal total = successfulPayments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Daily trend: last 7 days
        List<PartnerEarningsDetailResponse.TimeRevenueEntry> dailyTrend = new java.util.ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            java.time.LocalDate d = now.toLocalDate().minusDays(i);
            LocalDateTime dayStart = d.atStartOfDay();
            LocalDateTime dayEnd = d.plusDays(1).atStartOfDay();
            List<Payment> dayPayments = successfulPayments.stream()
                    .filter(p -> p.getPaymentDate() != null && !p.getPaymentDate().isBefore(dayStart) && p.getPaymentDate().isBefore(dayEnd))
                    .toList();
            BigDecimal dayAmt = dayPayments.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
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
            BigDecimal wAmt = weekPayments.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            weeklyTrend.add(new PartnerEarningsDetailResponse.TimeRevenueEntry("Wk " + (4 - i), wAmt, weekPayments.size()));
        }

        // Monthly trend: last 6 months
        List<PartnerEarningsDetailResponse.TimeRevenueEntry> monthlyTrend = new java.util.ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            java.time.YearMonth ym = java.time.YearMonth.from(now.toLocalDate().minusMonths(i));
            List<Payment> monthPayments = successfulPayments.stream()
                    .filter(p -> p.getPaymentDate() != null && java.time.YearMonth.from(p.getPaymentDate().toLocalDate()).equals(ym))
                    .toList();
            BigDecimal mAmt = monthPayments.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
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
                .completedPaymentsAmount(total)
                .completedPaymentsCount(successfulPayments.size())
                .pendingPaymentsAmount(pendingAmount)
                .pendingPaymentsCount(pendingPayments.size())
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
        return payments.stream().map(p -> {
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
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * FR-19 Report 2: Equipment-wise Revenue Report
     */
    @Transactional(readOnly = true)
    public List<PartnerEquipmentRevenueReportDto> getPartnerEquipmentRevenueReport(Long partnerId) {
        List<Payment> payments = paymentRepository.findByPartnerIdOrderByCreatedAtDesc(partnerId);
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

            BigDecimal rev = pList.stream()
                    .filter(p -> p.getPaymentStatus() == PaymentStatus.SUCCESS)
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

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
        java.util.Map<Long, java.util.List<Payment>> groupedByFarmer = new java.util.HashMap<>();

        for (Payment p : payments) {
            groupedByFarmer.computeIfAbsent(p.getFarmerId(), k -> new java.util.ArrayList<>()).add(p);
        }

        List<PartnerCustomerRevenueReportDto> result = new java.util.ArrayList<>();
        for (java.util.Map.Entry<Long, java.util.List<Payment>> entry : groupedByFarmer.entrySet()) {
            Long farmerId = entry.getKey();
            List<Payment> pList = entry.getValue();

            BigDecimal rev = pList.stream()
                    .filter(p -> p.getPaymentStatus() == PaymentStatus.SUCCESS)
                    .map(Payment::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

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
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseGet(() -> {
                    String txn = "TXN-" + System.currentTimeMillis();
                    String inv = "INV-2026-" + String.format("%05d", bookingId);
                    return Payment.builder()
                            .bookingId(bookingId)
                            .farmerId(booking.getFarmerId())
                            .partnerId(booking.getPartnerId() != null ? booking.getPartnerId() : null)
                            .amount(booking.getTotalCost() != null ? booking.getTotalCost() : BigDecimal.valueOf(1500))
                            .paymentMethod(PaymentMethod.UPI)
                            .transactionId(txn)
                            .paymentReference(txn)
                            .paymentStatus(PaymentStatus.SUCCESS)
                            .paymentDate(LocalDateTime.now())
                            .invoiceReference(inv)
                            .build();
                });

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

        BigDecimal total = payment.getAmount() != null ? payment.getAmount() : BigDecimal.valueOf(1500);
        BigDecimal subtotal = total.multiply(BigDecimal.valueOf(0.82)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal gst = total.subtract(subtotal);
        BigDecimal rate = subtotal.divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP);

        return InvoiceResponse.builder()
                .invoiceReference(payment.getInvoiceReference())
                .transactionId(payment.getTransactionId())
                .bookingId(booking.getId())
                .farmerId(booking.getFarmerId())
                .farmerName(farmer != null ? farmer.getFullName() : "Farmer #" + booking.getFarmerId())
                .farmerMobile(farmer != null ? farmer.getMobileNumber() : "N/A")
                .equipmentName(booking.getEquipmentName())
                .equipmentCategory(booking.getEquipmentCategory())
                .partnerName("Partner #" + (booking.getPartnerId() != null ? booking.getPartnerId() : 1))
                .bookingStartDate(booking.getStartDate())
                .bookingEndDate(booking.getEndDate())
                .rentalRatePerDay(rate)
                .rentalDays(days)
                .subtotal(subtotal)
                .gstAmount(gst)
                .totalAmount(total)
                .paymentMethod(payment.getPaymentMethod())
                .paymentDate(payment.getPaymentDate())
                .status(payment.getPaymentStatus().name())
                .build();
    }

    private PaymentResponse mapToResponse(Payment payment) {
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
                .build();
    }
}
