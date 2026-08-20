package com.agrorental.operator.dto;

import com.agrorental.operator.entity.OperatorStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Summary DTO representing an eligible, approved operator available for job assignment.
 * Excludes sensitive KYC and credential fields.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EligibleOperatorResponse {

    private Long operatorId;
    private String fullName;
    private String mobileNumber;
    private String skills;
    private Integer experience;
    private String address;
    private String profilePhoto;
    private OperatorStatus status;
    private boolean mobileVerified;
    private boolean active;
}
