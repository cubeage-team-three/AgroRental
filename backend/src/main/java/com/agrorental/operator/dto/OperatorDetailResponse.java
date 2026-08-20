package com.agrorental.operator.dto;

import com.agrorental.operator.entity.OperatorStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Detailed DTO for Operator review and administrative verification.
 * Government IDs are strictly masked to prevent PII exposure.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OperatorDetailResponse {

    private Long id;
    private String fullName;
    private String mobileNumber;
    private String email;
    private String address;
    private String maskedAadhaarNumber;
    private String maskedDrivingLicenseNumber;
    private Integer experience;
    private String skills;
    private String profilePhoto;
    private OperatorStatus status;
    private boolean mobileVerified;
    private String rejectionReason;
    private Long partnerId;
    private String partnerName;
    private boolean active;
    private List<OperatorDocumentResponse> documents;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
