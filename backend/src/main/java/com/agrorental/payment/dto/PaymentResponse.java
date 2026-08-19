package com.agrorental.payment.dto;

import com.agrorental.payment.enums.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object carrying rental payment transaction details.
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
    private PaymentStatus paymentStatus;
    private String paymentReference;
    private String paymentMethod;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
