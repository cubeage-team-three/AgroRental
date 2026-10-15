package com.agrorental.payment.service;

import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.ForbiddenException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.payment.dto.RefundResponse;
import com.agrorental.payment.entity.Payment;
import com.agrorental.payment.entity.PaymentRefund;
import com.agrorental.payment.entity.PaymentRefundEvent;
import com.agrorental.payment.entity.PaymentStatus;
import com.agrorental.payment.entity.RefundStatus;
import com.agrorental.payment.repository.PaymentRefundEventRepository;
import com.agrorental.payment.repository.PaymentRefundRepository;
import com.agrorental.payment.repository.PaymentRepository;
import com.agrorental.security.principal.CurrentCaller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * BIZ-20 refund lifecycle. A refund obligation is created only for a SUCCESS payment whose booking is cancelled
 * or rejected, always for the full amount actually paid, at most once per payment. There is no payment-provider
 * integration: a refund becomes REFUNDED only when an admin records the verified bank/UPI transfer reference.
 *
 * <pre>
 *   (none) --cancel/reject of a paid booking--> REFUND_PENDING
 *   REFUND_PENDING --admin, verified transfer reference--> REFUNDED   (terminal)
 *   REFUND_PENDING --admin, failure reason--> REFUND_FAILED
 *   REFUND_FAILED  --admin retry--> REFUND_PENDING                    (same obligation; attempt_count + 1)
 * </pre>
 * Every transition is appended to payment_refund_events with the actor and timestamp.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    /** Bank UTR / UPI reference: 6-64 chars, letters/digits plus . _ / - (no spaces). */
    private static final Pattern TRANSFER_REFERENCE = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._/-]{5,63}$");
    private static final int MAX_TEXT = 500;

    private final PaymentRepository paymentRepository;
    private final PaymentRefundRepository refundRepository;
    private final PaymentRefundEventRepository eventRepository;

    /**
     * Initiates the full refund for a paid booking that is being cancelled or rejected. Must run inside the
     * booking transaction that already holds the booking row lock (MANDATORY), so the status change and the
     * refund obligation commit atomically. The payment row is locked next (booking -> payment, the same order
     * as payment processing), so a concurrently committed payment is always seen.
     *
     * @return the refund obligation, or empty when the booking has no successful payment
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<PaymentRefund> initiateFullRefundIfPaid(Long bookingId, String reason, String actor) {
        Optional<Payment> paymentOpt = paymentRepository.findByBookingIdForUpdate(bookingId);
        if (paymentOpt.isEmpty() || paymentOpt.get().getPaymentStatus() != PaymentStatus.SUCCESS) {
            return Optional.empty();
        }
        Payment payment = paymentOpt.get();

        Optional<PaymentRefund> existing = refundRepository.findByPaymentId(payment.getId());
        if (existing.isPresent()) {
            // Never a second obligation for the same payment
            return existing;
        }

        LocalDateTime now = LocalDateTime.now();
        PaymentRefund refund = refundRepository.save(PaymentRefund.builder()
                .payment(payment)
                .bookingId(payment.getBookingId())
                .farmerId(payment.getFarmerId())
                .partnerId(payment.getPartnerId())
                .amount(payment.getAmount())
                .status(RefundStatus.REFUND_PENDING)
                .initiationReason(truncate(reason))
                .initiatedAt(now)
                .initiatedBy(actor)
                .attemptCount(1)
                .build());
        recordEvent(refund, null, RefundStatus.REFUND_PENDING, actor, reason, null, now);
        log.info("Refund {} initiated (REFUND_PENDING) for booking {}", refund.getId(), bookingId);
        return Optional.of(refund);
    }

    @Transactional(readOnly = true)
    public boolean hasRefundForBooking(Long bookingId) {
        return refundRepository.existsByBookingId(bookingId);
    }

    @Transactional(readOnly = true)
    public Optional<PaymentRefund> findForPayment(Long paymentId) {
        return paymentId == null ? Optional.empty() : refundRepository.findByPaymentId(paymentId);
    }

    @Transactional(readOnly = true)
    public List<RefundResponse> listRefunds(RefundStatus status, CurrentCaller caller) {
        requireAdmin(caller);
        List<PaymentRefund> refunds = status != null
                ? refundRepository.findByStatusOrderByInitiatedAtAsc(status)
                : refundRepository.findAllByOrderByInitiatedAtDesc();
        return refunds.stream().map(r -> toResponse(r, false)).toList();
    }

    @Transactional(readOnly = true)
    public RefundResponse getRefund(Long refundId, CurrentCaller caller) {
        requireAdmin(caller);
        PaymentRefund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found with ID: " + refundId));
        return toResponse(refund, true);
    }

    /**
     * Marks a refund REFUNDED. Requires the verified bank/UPI transfer reference of the actual transfer; a
     * reference may be used for only one refund.
     */
    @Transactional
    public RefundResponse completeRefund(Long refundId, String transferReference, String note, CurrentCaller caller, String actor) {
        requireAdmin(caller);
        String reference = transferReference == null ? "" : transferReference.trim();
        if (!TRANSFER_REFERENCE.matcher(reference).matches()) {
            throw new BadRequestException("A verified transfer reference (6-64 letters, digits, '.', '_', '/', '-') is required to complete a refund.");
        }
        PaymentRefund refund = lockRefund(refundId);
        requireStatus(refund, RefundStatus.REFUND_PENDING, "complete");
        if (refundRepository.existsByTransferReferenceIgnoreCase(reference)) {
            throw new BadRequestException("This transfer reference is already recorded for another refund.");
        }

        LocalDateTime now = LocalDateTime.now();
        refund.setStatus(RefundStatus.REFUNDED);
        refund.setTransferReference(reference);
        refund.setCompletedAt(now);
        refund.setCompletedBy(actor);
        try {
            // Flush now so a concurrent completion using the same reference hits uk_payment_refunds_transfer_reference here
            refundRepository.saveAndFlush(refund);
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException("This transfer reference is already recorded for another refund.");
        }
        recordEvent(refund, RefundStatus.REFUND_PENDING, RefundStatus.REFUNDED, actor, note, reference, now);
        log.info("Refund {} completed by {} (reference ending {})", refund.getId(), actor, maskReference(reference));
        return toResponse(refund, true);
    }

    /** Records a confirmed unsuccessful transfer. The obligation stays open (visible) until retried and completed. */
    @Transactional
    public RefundResponse markRefundFailed(Long refundId, String reason, CurrentCaller caller, String actor) {
        requireAdmin(caller);
        if (reason == null || reason.isBlank()) {
            throw new BadRequestException("A failure reason is required.");
        }
        PaymentRefund refund = lockRefund(refundId);
        requireStatus(refund, RefundStatus.REFUND_PENDING, "mark as failed");

        LocalDateTime now = LocalDateTime.now();
        refund.setStatus(RefundStatus.REFUND_FAILED);
        refund.setLastFailureReason(truncate(reason.trim()));
        refund.setLastFailedAt(now);
        refund.setLastFailedBy(actor);
        refundRepository.save(refund);
        recordEvent(refund, RefundStatus.REFUND_PENDING, RefundStatus.REFUND_FAILED, actor, reason.trim(), null, now);
        log.info("Refund {} marked REFUND_FAILED by {}", refund.getId(), actor);
        return toResponse(refund, true);
    }

    /** Re-opens a failed refund for another transfer attempt, on the same obligation (no new refund is created). */
    @Transactional
    public RefundResponse retryRefund(Long refundId, String note, CurrentCaller caller, String actor) {
        requireAdmin(caller);
        PaymentRefund refund = lockRefund(refundId);
        requireStatus(refund, RefundStatus.REFUND_FAILED, "retry");

        refund.setStatus(RefundStatus.REFUND_PENDING);
        refund.setAttemptCount(refund.getAttemptCount() + 1);
        refundRepository.save(refund);
        recordEvent(refund, RefundStatus.REFUND_FAILED, RefundStatus.REFUND_PENDING, actor, note, null, LocalDateTime.now());
        log.info("Refund {} retried by {} (attempt {})", refund.getId(), actor, refund.getAttemptCount());
        return toResponse(refund, true);
    }

    // ---- helpers ----------------------------------------------------------------------------------

    private PaymentRefund lockRefund(Long refundId) {
        return refundRepository.findByIdForUpdate(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found with ID: " + refundId));
    }

    private static void requireAdmin(CurrentCaller caller) {
        if (caller == null || !caller.isAdmin()) {
            throw new ForbiddenException("Access is denied. Only administrators can manage refunds.");
        }
    }

    private static void requireStatus(PaymentRefund refund, RefundStatus expected, String action) {
        if (refund.getStatus() != expected) {
            throw new BadRequestException("Cannot " + action + " a refund in status " + refund.getStatus()
                    + " (expected " + expected + ").");
        }
    }

    private void recordEvent(PaymentRefund refund, RefundStatus from, RefundStatus to, String actor, String note,
                             String reference, LocalDateTime at) {
        eventRepository.save(PaymentRefundEvent.builder()
                .refund(refund)
                .fromStatus(from)
                .toStatus(to)
                .actor(actor)
                .note(truncate(note))
                .transferReference(reference)
                .occurredAt(at)
                .build());
    }

    private static String truncate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() <= MAX_TEXT ? text : text.substring(0, MAX_TEXT);
    }

    private static String maskReference(String reference) {
        return reference.length() <= 4 ? "****" : reference.substring(reference.length() - 4);
    }

    RefundResponse toResponse(PaymentRefund refund, boolean withHistory) {
        RefundResponse.RefundResponseBuilder builder = RefundResponse.builder()
                .id(refund.getId())
                .paymentId(refund.getPayment() != null ? refund.getPayment().getId() : null)
                .bookingId(refund.getBookingId())
                .farmerId(refund.getFarmerId())
                .partnerId(refund.getPartnerId())
                .amount(refund.getAmount())
                .status(refund.getStatus())
                .initiationReason(refund.getInitiationReason())
                .initiatedAt(refund.getInitiatedAt())
                .initiatedBy(refund.getInitiatedBy())
                .attemptCount(refund.getAttemptCount())
                .transferReference(refund.getTransferReference())
                .completedAt(refund.getCompletedAt())
                .completedBy(refund.getCompletedBy())
                .lastFailureReason(refund.getLastFailureReason())
                .lastFailedAt(refund.getLastFailedAt())
                .lastFailedBy(refund.getLastFailedBy());
        if (withHistory && refund.getId() != null) {
            builder.history(eventRepository.findByRefundIdOrderByOccurredAtAscIdAsc(refund.getId()).stream()
                    .map(e -> RefundResponse.Event.builder()
                            .fromStatus(e.getFromStatus())
                            .toStatus(e.getToStatus())
                            .actor(e.getActor())
                            .note(e.getNote())
                            .transferReference(e.getTransferReference())
                            .occurredAt(e.getOccurredAt())
                            .build())
                    .toList());
        }
        return builder.build();
    }
}
