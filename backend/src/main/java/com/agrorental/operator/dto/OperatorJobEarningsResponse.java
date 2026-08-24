package com.agrorental.operator.dto;

import com.agrorental.operator.enums.OperatorAssignmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO representing detailed work duration and earnings calculation for a specific operator job assignment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorJobEarningsResponse {

    private Long assignmentId;
    private Long bookingId;
    private OperatorAssignmentStatus assignmentStatus;
    private BigDecimal hourlyRate;
    private Long totalElapsedMinutes;
    private Long pausedMinutes;
    private Long netWorkMinutes;
    private Double netWorkHours;
    private BigDecimal grossEarnings;
    private String currency;
    private Boolean isFinalized;
    private Boolean isEstimated;
    private LocalDateTime workStartedAt;
    private LocalDateTime completedAt;
    private LocalDateTime pausedAt;
    private LocalDateTime resumedAt;
}
