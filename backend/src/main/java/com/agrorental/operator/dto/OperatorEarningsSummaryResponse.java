package com.agrorental.operator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Aggregate summary of completed jobs, logged work hours, pause duration, and total gross earnings.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorEarningsSummaryResponse {

    private Long totalCompletedJobs;
    private Long totalWorkMinutes;
    private Double totalWorkHours;
    private Long totalPausedMinutes;
    private BigDecimal totalGrossEarnings;
    private BigDecimal averageEarningsPerJob;
    private BigDecimal hourlyRate;
    private String currency;
}
