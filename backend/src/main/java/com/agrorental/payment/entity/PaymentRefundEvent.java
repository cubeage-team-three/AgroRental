package com.agrorental.payment.entity;

import com.agrorental.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Append-only audit record of a single refund state transition. */
@Entity
@Table(
        name = "payment_refund_events",
        indexes = @Index(name = "idx_payment_refund_events_refund", columnList = "refund_id, occurred_at")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRefundEvent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "refund_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_payment_refund_events_refund"))
    private PaymentRefund refund;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status")
    private RefundStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false)
    private RefundStatus toStatus;

    @Column(name = "actor", nullable = false, length = 100)
    private String actor;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "transfer_reference", length = 64)
    private String transferReference;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;
}
