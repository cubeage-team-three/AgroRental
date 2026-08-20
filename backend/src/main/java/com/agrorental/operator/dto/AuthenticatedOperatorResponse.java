package com.agrorental.operator.dto;

import com.agrorental.operator.entity.OperatorStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Safe representation of an authenticated Operator identity.
 * Strictly excludes sensitive security fields, credentials, OTPs, and unmasked government IDs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticatedOperatorResponse {

    private Long id;
    private String fullName;
    private String mobileNumber;
    private String email;
    private OperatorStatus status;
    private boolean mobileVerified;
    private boolean active;
    private String role;
}
