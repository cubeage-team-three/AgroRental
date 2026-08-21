package com.agrorental.operator.dto;

import com.agrorental.operator.entity.OperatorStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload used by Admins to review and approve or reject an Operator account.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperatorVerificationRequest {

    @NotNull(message = "Verification status is required (APPROVED or REJECTED)")
    private OperatorStatus status;

    private String rejectionReason;
}
