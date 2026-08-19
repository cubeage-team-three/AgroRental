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
                .partnerId(booking != null ? booking.getPartnerId() : 1L)
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
                            .partnerId(booking.getPartnerId() != null ? booking.getPartnerId() : 1L)
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
