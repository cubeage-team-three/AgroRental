package com.agrorental.payment.service;

import com.agrorental.booking.dto.BookingResponse;
import com.agrorental.booking.service.BookingService;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.farmer.dto.FarmerProfileResponse;
import com.agrorental.farmer.service.FarmerProfileService;
import com.agrorental.payment.dto.InvoiceResponse;
import com.agrorental.payment.dto.PaymentRequest;
import com.agrorental.payment.dto.PaymentResponse;
import com.agrorental.payment.entity.Payment;
import com.agrorental.payment.entity.PaymentStatus;
import com.agrorental.payment.repository.PaymentRepository;
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
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingService bookingService;
    private final FarmerProfileService farmerProfileService;

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
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .transactionId(transactionId)
                .paymentStatus(PaymentStatus.SUCCESS)
                .paymentDate(LocalDateTime.now())
                .invoiceReference(invoiceRef)
                .build();

        Payment saved = paymentRepository.save(payment);

        return mapToResponse(saved);
    }

    public PaymentResponse getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + id));
        return mapToResponse(payment);
    }

    public List<PaymentResponse> getFarmerPayments(Long farmerId) {
        return paymentRepository.findByFarmerIdOrderByPaymentDateDesc(farmerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public InvoiceResponse generateInvoice(Long bookingId) {
        BookingResponse booking = bookingService.getBookingById(bookingId);
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseGet(() -> {
                    String txn = "TXN-" + System.currentTimeMillis();
                    String inv = "INV-2026-" + String.format("%05d", bookingId);
                    return Payment.builder()
                            .bookingId(bookingId)
                            .farmerId(booking.getFarmerId())
                            .amount(booking.getTotalCost() != null ? booking.getTotalCost() : BigDecimal.valueOf(1500))
                            .paymentMethod(com.agrorental.payment.entity.PaymentMethod.UPI)
                            .transactionId(txn)
                            .paymentStatus(PaymentStatus.SUCCESS)
                            .paymentDate(LocalDateTime.now())
                            .invoiceReference(inv)
                            .build();
                });

        FarmerProfileResponse farmer = null;
        try {
            farmer = farmerProfileService.getProfile(booking.getFarmerId());
        } catch (Exception e) {
            // Fallback if profile not found
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
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .transactionId(payment.getTransactionId())
                .paymentStatus(payment.getPaymentStatus())
                .paymentDate(payment.getPaymentDate())
                .invoiceReference(payment.getInvoiceReference())
                .build();
    }
}
