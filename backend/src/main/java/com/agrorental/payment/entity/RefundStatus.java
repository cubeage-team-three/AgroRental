package com.agrorental.payment.entity;

/**
 * Lifecycle of a refund obligation for a paid booking. "No refund" (NONE) is represented by the absence
 * of a {@link PaymentRefund} row. REFUNDED is only reached when an admin records a verified transfer reference.
 */
public enum RefundStatus {
    REFUND_PENDING,
    REFUNDED,
    REFUND_FAILED
}
