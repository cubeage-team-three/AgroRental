package com.agrorental.payment.dto;

import com.agrorental.payment.entity.PaymentMethod;
import com.agrorental.payment.entity.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object carrying rental payment transaction details for farmers and partners.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    private Long id;
    private Long bookingId;
    private Long farmerId;
    private Long partnerId;
    private String equipmentName;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private String paymentReference;
    private String transactionId;
    private PaymentStatus paymentStatus;
    private LocalDateTime paymentDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String invoiceReference;
}
