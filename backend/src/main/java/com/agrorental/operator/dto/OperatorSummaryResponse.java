package com.agrorental.operator.dto;

import com.agrorental.operator.entity.OperatorStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Summary DTO for displaying Operators in administrative tables and lists.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperatorSummaryResponse {

    private Long id;
    private String fullName;
    private String mobileNumber;
    private String email;
    private Integer experience;
    private String skills;
    private String profilePhoto;
    private OperatorStatus status;
    private boolean mobileVerified;
    private int documentsCount;
    private Long partnerId;
    private boolean active;
    private LocalDateTime createdAt;
}
