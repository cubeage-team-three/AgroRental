package com.agrorental.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Admin record of a confirmed unsuccessful refund transfer. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundFailureRequest {

    @NotBlank(message = "A failure reason is required")
    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;
}
