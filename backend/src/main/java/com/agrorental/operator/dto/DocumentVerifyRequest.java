package com.agrorental.operator.dto;

import com.agrorental.operator.entity.DocumentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentVerifyRequest {

    @NotNull(message = "Verification status is required")
    private DocumentStatus status;

    private String rejectionReason;

    private String verifiedBy;
}
