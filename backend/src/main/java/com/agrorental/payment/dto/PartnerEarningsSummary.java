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
}
