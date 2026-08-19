package com.agrorental.payment.service;

import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.booking.repository.BookingRepository;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.notification.service.NotificationService;
import com.agrorental.payment.dto.PartnerEarningsSummary;
import com.agrorental.payment.dto.PaymentCreateRequest;
import com.agrorental.payment.dto.PaymentResponse;
import com.agrorental.payment.entity.PaymentTransaction;
import com.agrorental.payment.enums.PaymentStatus;
import com.agrorental.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Service managing financial transactions, amount verification, duplicate protection, and earnings reporting.
 */
@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;

    public PaymentService(
            PaymentRepository paymentRepository,
            BookingRepository bookingRepository,
            NotificationService notificationService) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.notificationService = notificationService;
    }

    /**
     * Processes a simulated payment transaction for a confirmed booking.
     */
    public PaymentResponse createPayment(PaymentCreateRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + request.getBookingId()));

        if (!booking.getFarmerId().equals(request.getFarmerId())) {
            throw new BadRequestException("Farmer is not authorized to pay for booking #" + request.getBookingId());
        }

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.REJECTED) {
            throw new BadRequestException("Cannot process payment for booking in state: " + booking.getStatus());
        }

        if (paymentRepository.existsByBookingIdAndPaymentStatus(booking.getId(), PaymentStatus.SUCCESS)) {
            throw new BadRequestException("Payment has already been completed for booking #" + booking.getId());
        }

        if (request.getAmount() != null && request.getAmount().compareTo(booking.getTotalCost()) != 0) {
            throw new BadRequestException("Payment amount mismatch. Expected: " + booking.getTotalCost() + ", Provided: " + request.getAmount());
        }

        Long partnerId = booking.getPartner() != null ? booking.getPartner().getId() : 1L;
        PaymentStatus status = Boolean.TRUE.equals(request.getSimulateFailure()) ? PaymentStatus.FAILED : PaymentStatus.SUCCESS;
        String reference = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String method = request.getPaymentMethod() != null ? request.getPaymentMethod() : "SIMULATED_UPI";

        PaymentTransaction transaction = PaymentTransaction.builder()
                .bookingId(booking.getId())
                .farmerId(booking.getFarmerId())
                .partnerId(partnerId)
                .amount(booking.getTotalCost())
                .paymentStatus(status)
                .paymentReference(reference)
                .paymentMethod(method)
                .build();

        PaymentTransaction saved = paymentRepository.save(transaction);
        String equipName = booking.getEquipment() != null ? booking.getEquipment().getName() : "Machinery";

        if (status == PaymentStatus.SUCCESS) {
            notificationService.sendNotification(
                    "FARMER",
                    booking.getFarmerId(),
                    "Payment Successful",
                    "Payment of ₹" + saved.getAmount() + " for booking #" + booking.getId() + " (" + equipName + ") was successful. Reference: " + reference,
                    "PAYMENT_SUCCESS",
                    booking.getId()
            );

            notificationService.sendNotification(
                    "PARTNER",
                    partnerId,
                    "Rental Revenue Credited",
                    "Rental payment of ₹" + saved.getAmount() + " for booking #" + booking.getId() + " (" + equipName + ") has been credited.",
                    "PAYMENT_RECEIVED",
                    booking.getId()
            );
        } else {
            notificationService.sendNotification(
                    "FARMER",
                    booking.getFarmerId(),
                    "Payment Failed",
                    "Payment attempt for booking #" + booking.getId() + " failed. Please try again.",
                    "PAYMENT_FAILED",
                    booking.getId()
            );
        }

        return mapToResponse(saved, equipName);
    }

    /**
     * Retrieves transaction by payment primary key.
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(Long id) {
        PaymentTransaction transaction = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment transaction not found with ID: " + id));
        return mapToResponse(transaction, getEquipmentName(transaction.getBookingId()));
    }

    /**
     * Retrieves transaction by booking ID.
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByBookingId(Long bookingId) {
        PaymentTransaction transaction = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("No payment transaction found for booking ID: " + bookingId));
        return mapToResponse(transaction, getEquipmentName(bookingId));
    }

    /**
     * Retrieves payment history for a specific farmer.
     */
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByFarmer(Long farmerId) {
        return paymentRepository.findByFarmerIdOrderByCreatedAtDesc(farmerId).stream()
                .map(tx -> mapToResponse(tx, getEquipmentName(tx.getBookingId())))
                .toList();
    }

    /**
     * Retrieves revenue transactions for a specific partner.
     */
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByPartner(Long partnerId) {
        return paymentRepository.findByPartnerIdOrderByCreatedAtDesc(partnerId).stream()
                .map(tx -> mapToResponse(tx, getEquipmentName(tx.getBookingId())))
                .toList();
    }

    /**
     * Calculates total realized earnings summary for a partner.
     */
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

    private String getEquipmentName(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .map(b -> b.getEquipment() != null ? b.getEquipment().getName() : "Machinery")
                .orElse("Machinery");
    }

    private PaymentResponse mapToResponse(PaymentTransaction tx, String equipmentName) {
        return PaymentResponse.builder()
                .id(tx.getId())
                .bookingId(tx.getBookingId())
                .farmerId(tx.getFarmerId())
                .partnerId(tx.getPartnerId())
                .equipmentName(equipmentName)
                .amount(tx.getAmount())
                .paymentStatus(tx.getPaymentStatus())
                .paymentReference(tx.getPaymentReference())
                .paymentMethod(tx.getPaymentMethod())
                .createdAt(tx.getCreatedAt())
                .updatedAt(tx.getUpdatedAt())
                .build();
    }
}
