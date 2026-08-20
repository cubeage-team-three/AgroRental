package com.agrorental.operator.dto;

import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.operator.enums.OperatorAssignmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO exposing assigned field task, machinery details, and complete lifecycle audit trail to an authenticated Operator.
 * Excludes sensitive farmer/partner payment details, PII, and credentials.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorAssignedJobResponse {

    private Long assignmentId;
    private Long bookingId;
    private Long operatorId;
    private OperatorAssignmentStatus assignmentStatus;
    private LocalDateTime assignedAt;
    private String assignedBy;
    private String notes;

    // Phase 5 Lifecycle Auditing Fields
    private LocalDateTime acceptedAt;
    private LocalDateTime rejectedAt;
    private String rejectionReason;
    private LocalDateTime travelingAt;
    private LocalDateTime reachedAt;
    private LocalDateTime workStartedAt;
    private LocalDateTime pausedAt;
    private String pauseReason;
    private LocalDateTime resumedAt;
    private LocalDateTime completedAt;
    private String completionNotes;

    // Machinery details
    private Long equipmentId;
    private String equipmentName;
    private String equipmentCategory;
    private String primaryImageUrl;

    // Service & Schedule details
    private Long farmerId;
    private String deliveryAddress;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalCost;
    private BookingStatus bookingStatus;
}
