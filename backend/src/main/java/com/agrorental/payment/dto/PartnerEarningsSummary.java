package com.agrorental.payment.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * Data Transfer Object carrying partner realized financial metrics.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerEarningsSummary {

    private Long partnerId;
    private BigDecimal totalRealizedEarnings;
    private long completedTransactionCount;

    // BIZ-20 refund breakdown (see RevenueCalculator). Headline revenue figures above are NET of completed refunds.
    private BigDecimal grossCollected;
    private BigDecimal refundsPendingAmount;
    private long refundsPendingCount;
    private BigDecimal refundsFailedAmount;
    private long refundsFailedCount;
    private BigDecimal refundsCompletedAmount;
    private long refundsCompletedCount;
}
