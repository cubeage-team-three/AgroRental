package com.agrorental.operator.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload submitted by an Operator when declining an assigned task.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorJobRejectionRequest {

    @NotBlank(message = "Rejection reason is required")
    @Size(min = 3, max = 500, message = "Rejection reason must be between 3 and 500 characters")
    private String rejectionReason;
}
