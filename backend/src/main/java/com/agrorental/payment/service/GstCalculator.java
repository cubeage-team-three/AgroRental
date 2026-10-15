package com.agrorental.payment.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * GST split for booking totals that already INCLUDE 18% GST (no tax is added on top).
 * Taxable value = total / 1.18, rounded half-up to 2 decimal places (paise); GST = total - taxable value,
 * so taxable value + GST always equals the charged total exactly.
 */
public final class GstCalculator {

    public static final BigDecimal GST_RATE = new BigDecimal("0.18");
    private static final BigDecimal INCLUSIVE_DIVISOR = BigDecimal.ONE.add(GST_RATE);

    private GstCalculator() {
    }

    public record Split(BigDecimal taxableValue, BigDecimal gstAmount) {
    }

    public static Split splitInclusiveTotal(BigDecimal inclusiveTotal) {
        BigDecimal total = (inclusiveTotal == null ? BigDecimal.ZERO : inclusiveTotal).setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxable = total.divide(INCLUSIVE_DIVISOR, 2, RoundingMode.HALF_UP);
        return new Split(taxable, total.subtract(taxable));
    }
}
