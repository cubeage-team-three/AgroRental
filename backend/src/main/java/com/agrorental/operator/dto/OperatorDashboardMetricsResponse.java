package com.agrorental.operator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO providing comprehensive metrics, KPIs, and active assignment info for an authenticated Operator.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperatorDashboardMetricsResponse {

    private Long totalJobs;

    // Status breakdown counts
    private Long assignedJobs;
    private Long acceptedJobs;
    private Long travelingJobs;
    private Long reachedJobs;
    private Long inProgressJobs;
    private Long pausedJobs;
    private Long completedJobs;
    private Long rejectedJobs;
    private Long cancelledJobs;

    // Schedule counts
    private Long todayJobs;
    private Long upcomingJobs;

    // Performance rates (percentages)
    private Double completionRate;
    private Double acceptanceRate;

    // Active assignment
    private OperatorDashboardActiveJobResponse activeJob;
}
