package com.agrorental.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Aggregate platform-wide metrics for the Admin Overview bento cards.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardStatsResponse {

    private long totalFarmers;
    private long activeOperators;
    private BigDecimal totalRevenue;
    private long pendingApprovals;
}
