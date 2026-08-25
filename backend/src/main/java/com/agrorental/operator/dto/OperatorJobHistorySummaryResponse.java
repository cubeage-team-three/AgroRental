package com.agrorental.operator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO encapsulating aggregated historical field performance metrics for an Operator.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperatorJobHistorySummaryResponse {

    private Long operatorId;
    private long totalHistoricalJobs;
    private long completedJobs;
    private long rejectedJobs;
    private long cancelledJobs;
    private BigDecimal totalWorkHours;
    private long totalPausedMinutes;
    private BigDecimal totalGrossEarnings;
    private String currency;
    private Double averageRating;
    private long totalReviewsCount;
}
