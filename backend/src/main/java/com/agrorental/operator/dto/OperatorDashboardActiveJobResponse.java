package com.agrorental.operator.dto;

import com.agrorental.operator.enums.OperatorAssignmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response DTO exposing concise active job details for the Operator Dashboard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorDashboardActiveJobResponse {

    private Long assignmentId;
    private Long bookingId;
    private OperatorAssignmentStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private String deliveryAddress;

    // Machinery details
    private Long equipmentId;
    private String equipmentName;
    private String equipmentCategory;
    private String primaryImageUrl;

    // Customer & Financial details
    private Long farmerId;
    private BigDecimal totalCost;
}
