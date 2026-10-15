package com.agrorental.payment.entity;

import com.agrorental.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * The single refund obligation for a payment (unique per payment). The original {@link Payment} row is never
 * modified; this record tracks the refund separately so gross collections remain auditable.
 */
@Entity
@Table(
        name = "payment_refunds",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payment_refunds_payment_id", columnNames = "payment_id"),
                @UniqueConstraint(name = "uk_payment_refunds_transfer_reference", columnNames = "transfer_reference")
        },
        indexes = {
                @Index(name = "idx_payment_refunds_status", columnList = "status"),
                @Index(name = "idx_payment_refunds_partner_status", columnList = "partner_id, status"),
                @Index(name = "idx_payment_refunds_booking", columnList = "booking_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRefund extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_payment_refunds_payment"))
    private Payment payment;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "farmer_id", nullable = false)
    private Long farmerId;

    @Column(name = "partner_id")
    private Long partnerId;

    /** Always the amount actually paid (copied from the payment), never a client value or a re-priced total. */
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RefundStatus status;

    @Column(name = "initiation_reason", nullable = false, length = 500)
    private String initiationReason;

    @Column(name = "initiated_at", nullable = false)
    private LocalDateTime initiatedAt;

    @Column(name = "initiated_by", nullable = false, length = 100)
    private String initiatedBy;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Column(name = "transfer_reference", length = 64)
    private String transferReference;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "completed_by", length = 100)
    private String completedBy;

    @Column(name = "last_failure_reason", length = 500)
    private String lastFailureReason;

    @Column(name = "last_failed_at")
    private LocalDateTime lastFailedAt;

    @Column(name = "last_failed_by", length = 100)
    private String lastFailedBy;
}
