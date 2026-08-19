package com.agrorental.payment.dto;

import com.agrorental.payment.entity.PaymentMethod;
import com.agrorental.payment.entity.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    private Long id;
    private Long bookingId;
    private Long farmerId;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private String transactionId;
    private PaymentStatus paymentStatus;
    private LocalDateTime paymentDate;
    private String invoiceReference;
}
