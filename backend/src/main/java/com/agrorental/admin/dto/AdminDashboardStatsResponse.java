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

    // BIZ-20 refund breakdown (see RevenueCalculator). Headline revenue figures above are NET of completed refunds.
    private BigDecimal grossCollected;
    private BigDecimal refundsPendingAmount;
    private long refundsPendingCount;
    private BigDecimal refundsFailedAmount;
    private long refundsFailedCount;
    private BigDecimal refundsCompletedAmount;
    private long refundsCompletedCount;
    private long pendingApprovals;
}
