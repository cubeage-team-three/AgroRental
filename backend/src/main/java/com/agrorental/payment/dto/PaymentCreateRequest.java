package com.agrorental.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

/**
 * Payload for processing a simulated rental payment transaction.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCreateRequest {

    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    @NotNull(message = "Farmer ID is required")
    private Long farmerId;

    private BigDecimal amount;

    private String paymentMethod;

    private Boolean simulateFailure;
}
