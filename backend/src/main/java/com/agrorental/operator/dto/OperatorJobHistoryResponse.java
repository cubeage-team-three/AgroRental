package com.agrorental.operator.dto;

import com.agrorental.operator.enums.OperatorAssignmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO encapsulating a historical job assignment record for an Operator,
 * containing machinery metadata, work duration, pause intervals, server-calculated earnings,
 * customer feedback rating, and GPS tracking indicators.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperatorJobHistoryResponse {

    private Long assignmentId;
    private Long bookingId;
    private Long operatorId;
    private String operatorName;

    // Equipment & Machinery Metadata
    private Long equipmentId;
    private String equipmentName;
    private String equipmentModel;
    private String equipmentCategory;
    private String equipmentRegistrationNumber;

    // Customer & Partner Metadata
    private Long farmerId;
    private String farmerName;
    private String deliveryAddress;
    private Long partnerId;
    private String partnerName;

    // Booking & Assignment Dates
    private LocalDate bookingStartDate;
    private LocalDate bookingEndDate;
    private OperatorAssignmentStatus assignmentStatus;
    private String notes;
    private String rejectionReason;
    private String pauseReason;

    // Audited Work Lifecycle Timestamps
    private LocalDateTime assignedAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime rejectedAt;
    private LocalDateTime travelingAt;
    private LocalDateTime reachedAt;
    private LocalDateTime workStartedAt;
    private LocalDateTime pausedAt;
    private LocalDateTime resumedAt;
    private LocalDateTime completedAt;

    // Server-Authoritative Duration & Financials
    private Long totalElapsedMinutes;
    private Long totalPausedMinutes;
    private BigDecimal netWorkHours;
    private BigDecimal hourlyRate;
    private BigDecimal grossEarnings;
    private String currency;
    private boolean isFinalized;

    // Customer Rating & Feedback
    private Integer customerRating;
    private String customerReview;
    private LocalDateTime reviewSubmittedAt;

    // Spatial & GPS Indicators
    private boolean hasGpsData;
    private Double latestLatitude;
    private Double latestLongitude;
}
