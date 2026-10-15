package com.agrorental.payment.service;

import com.agrorental.booking.dto.BookingResponse;
import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.booking.repository.BookingRepository;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.payment.dto.PaymentRequest;
import com.agrorental.payment.entity.Payment;
import com.agrorental.payment.entity.PaymentMethod;
import com.agrorental.payment.entity.PaymentStatus;
import com.agrorental.payment.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Dedicated transactional helper to isolate payment database operations requiring
 * independent transaction boundaries (Propagation.REQUIRES_NEW).
 *
 * Prevents constraint violations (such as uk_payments_booking_id collisions) from marking
 * caller transactions as rollback-only, enabling graceful recovery and idempotent resolution.
 * Coordinates pessimistic booking row locking with payment persistence to serialize against
 * concurrent booking cancellation and rejection operations.
 */
@Component
public class PaymentTransactionHelper {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    @Autowired
    public PaymentTransactionHelper(PaymentRepository paymentRepository,
                                    @Autowired(required = false) BookingRepository bookingRepository) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
    }

    public PaymentTransactionHelper(PaymentRepository paymentRepository) {
        this(paymentRepository, null);
    }

    /**
     * Executes payment validation and persistence atomically while holding a pessimistic
     * write lock on the target Booking entity row.
     *
     * Serializes execution against concurrent cancelBooking and rejectBooking operations,
     * preventing race conditions where payments are committed for cancelled/rejected bookings.
     *
     * @param request Validated payment request payload
     * @param precheckBooking Booking response from initial pre-validation
     * @return Saved or existing winning Payment entity
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment processPaymentInTransaction(PaymentRequest request, BookingResponse precheckBooking) {
        if (bookingRepository != null) {
            Booking lockedBooking = bookingRepository.findByIdWithLock(request.getBookingId())
                    .or(() -> bookingRepository.findById(request.getBookingId()))
                    .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + request.getBookingId()));

            // Authoritative eligibility check under the booking row lock (serialized with cancel/reject)
            if (lockedBooking.getStatus() != BookingStatus.CONFIRMED) {
                throw new BadRequestException("Cannot process payment for a booking with status: " + lockedBooking.getStatus()
                        + ". Payment is accepted only for confirmed bookings.");
            }

            // Re-validate against the locked booking and always record the server-side total
            if (lockedBooking.getTotalCost() == null) {
                throw new BadRequestException("Booking total is not available; payment cannot be processed.");
            }
            if (request.getAmount() != null && request.getAmount().compareTo(lockedBooking.getTotalCost()) != 0) {
                throw new BadRequestException("Payment amount must equal the booking total of ₹" + lockedBooking.getTotalCost());
            }
            request.setAmount(lockedBooking.getTotalCost());
        }

        Optional<Payment> existingPayment = paymentRepository.findByBookingId(request.getBookingId());
        if (existingPayment.isPresent() && existingPayment.get().getPaymentStatus() == PaymentStatus.SUCCESS) {
            return existingPayment.get();
        }

        String transactionId = "TXN-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String invoiceRef = "INV-2026-" + String.format("%05d", System.currentTimeMillis() % 100000);

        Payment payment;
        if (existingPayment.isPresent()) {
            payment = existingPayment.get();
            payment.setAmount(request.getAmount());
            payment.setPaymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : PaymentMethod.UPI);
            payment.setPaymentReference(transactionId);
            payment.setTransactionId(transactionId);
            payment.setPaymentStatus(PaymentStatus.SUCCESS);
            payment.setPaymentDate(LocalDateTime.now());
            payment.setInvoiceReference(invoiceRef);
        } else {
            payment = Payment.builder()
                    .bookingId(request.getBookingId())
                    .farmerId(request.getFarmerId())
                    .partnerId(precheckBooking != null ? precheckBooking.getPartnerId() : null)
                    .amount(request.getAmount())
                    .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : PaymentMethod.UPI)
                    .paymentReference(transactionId)
                    .transactionId(transactionId)
                    .paymentStatus(PaymentStatus.SUCCESS)
                    .paymentDate(LocalDateTime.now())
                    .invoiceReference(invoiceRef)
                    .build();
        }

        Payment saved = paymentRepository.save(payment);
        paymentRepository.flush();
        return saved;
    }

    /**
     * Persists and flushes a payment record in an isolated transaction.
     *
     * @param payment Payment entity to persist
     * @return Saved payment entity
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Payment saveInNewTransaction(Payment payment) {
        Payment saved = paymentRepository.save(payment);
        paymentRepository.flush();
        return saved;
    }

    /**
     * Retrieves a payment by booking ID in a fresh, isolated read transaction.
     * Guarantees visibility of newly committed rows across concurrent threads regardless of outer MVCC snapshots.
     *
     * @param bookingId Target booking ID
     * @return Optional containing winning payment if present
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<Payment> findByBookingIdInNewTransaction(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId);
    }
}
