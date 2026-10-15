package com.agrorental.payment.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Admin request to re-open a failed refund for another transfer attempt. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRetryRequest {

    @Size(max = 500, message = "Note cannot exceed 500 characters")
    private String note;
}
