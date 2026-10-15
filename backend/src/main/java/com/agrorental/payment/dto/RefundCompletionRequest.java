package com.agrorental.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Admin confirmation that the refund was actually transferred, with the verified bank/UPI reference. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundCompletionRequest {

    @NotBlank(message = "The verified transfer reference is required")
    @Size(max = 64, message = "Transfer reference cannot exceed 64 characters")
    private String transferReference;

    @Size(max = 500, message = "Note cannot exceed 500 characters")
    private String note;
}
