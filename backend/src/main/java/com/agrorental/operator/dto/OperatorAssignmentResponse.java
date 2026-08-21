package com.agrorental.operator.dto;

import com.agrorental.operator.enums.OperatorAssignmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response payload returned after an Operator has been assigned to a Booking.
 * Strictly excludes sensitive KYC and credentials.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorAssignmentResponse {

    private Long assignmentId;
    private Long bookingId;
    private Long operatorId;
    private String operatorName;
    private String operatorMobile;
    private Long equipmentId;
    private String equipmentName;
    private OperatorAssignmentStatus assignmentStatus;
    private LocalDateTime assignedAt;
    private String assignedBy;
    private String notes;
}
