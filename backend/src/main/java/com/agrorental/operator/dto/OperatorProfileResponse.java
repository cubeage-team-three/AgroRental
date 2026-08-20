package com.agrorental.operator.dto;

import com.agrorental.operator.entity.OperatorStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Safe Response DTO representing an Operator's profile information.
 * Strictly excludes password, password hash, OTP, and raw unmasked government identifiers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorProfileResponse {

    private Long id;
    private String fullName;
    private String mobileNumber;
    private String email;
    private String address;
    private Integer experience;
    private String skills;
    private String profilePhoto;

    // Masked KYC Identifiers
    private String maskedAadhaarNumber;
    private String maskedDrivingLicenseNumber;

    // Status flags
    private OperatorStatus status;
    private boolean mobileVerified;
    private boolean active;

    // Associated partner details
    private Long partnerId;
    private String partnerName;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
