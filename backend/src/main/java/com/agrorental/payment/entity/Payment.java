package com.agrorental.payment.entity;

import com.agrorental.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseEntity {

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "farmer_id", nullable = false)
    private Long farmerId;

    @Column(name = "partner_id")
    private Long partnerId;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    @Column(name = "payment_reference")
    private String paymentReference;

    @Column(name = "transaction_id", nullable = false, unique = true)
    private String transactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus;

    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;

    @Column(name = "invoice_reference", nullable = false, unique = true)
    private String invoiceReference;
}
