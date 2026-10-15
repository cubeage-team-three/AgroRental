package com.agrorental.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerEarningsDetailResponse {

    private Long partnerId;
    private BigDecimal dailyEarnings;
    private BigDecimal weeklyEarnings;
    private BigDecimal monthlyEarnings;
    private BigDecimal yearlyEarnings;
    private BigDecimal totalRevenue;
    private BigDecimal pendingPaymentsAmount;
    private long pendingPaymentsCount;
    private BigDecimal completedPaymentsAmount;
    private long completedPaymentsCount;

    // BIZ-20 refund breakdown (see RevenueCalculator). Headline revenue figures above are NET of completed refunds.
    private BigDecimal grossCollected;
    private BigDecimal refundsPendingAmount;
    private long refundsPendingCount;
    private BigDecimal refundsFailedAmount;
    private long refundsFailedCount;
    private BigDecimal refundsCompletedAmount;
    private long refundsCompletedCount;

    // Trend breakdown for daily (last 7 days), weekly (last 4 weeks), monthly (last 12 months)
    private List<TimeRevenueEntry> dailyTrend;
    private List<TimeRevenueEntry> weeklyTrend;
    private List<TimeRevenueEntry> monthlyTrend;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TimeRevenueEntry {
        private String periodLabel;
        private BigDecimal amount;
        private long transactionCount;
    }
}
