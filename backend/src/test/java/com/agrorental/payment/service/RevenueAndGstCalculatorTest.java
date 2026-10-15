package com.agrorental.payment.service;

import com.agrorental.payment.entity.Payment;
import com.agrorental.payment.entity.PaymentRefund;
import com.agrorental.payment.entity.PaymentStatus;
import com.agrorental.payment.entity.RefundStatus;
import com.agrorental.payment.repository.PaymentRefundRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("BIZ-20: GST split and revenue definitions")
class RevenueAndGstCalculatorTest {

    // ---- GST (18% included in the booking total) ---------------------------------------------------------

    @ParameterizedTest(name = "{0} = {1} + {2}")
    @CsvSource({
            "5000.00, 4237.29, 762.71",
            "7500.00, 6355.93, 1144.07",
            "2500.00, 2118.64, 381.36",
            "118.00, 100.00, 18.00",
            "1.00, 0.85, 0.15",
            "0.01, 0.01, 0.00",
            "0.00, 0.00, 0.00",
            "99999999.99, 84745762.70, 15254237.29",
    })
    void inclusiveTotalSplitsIntoTaxableValueAndGstThatAddUpExactly(String total, String taxable, String gst) {
        GstCalculator.Split split = GstCalculator.splitInclusiveTotal(new BigDecimal(total));

        assertThat(split.taxableValue()).isEqualByComparingTo(taxable);
        assertThat(split.gstAmount()).isEqualByComparingTo(gst);
        assertThat(split.taxableValue().add(split.gstAmount())).isEqualByComparingTo(total);
        assertThat(split.taxableValue().scale()).isEqualTo(2);
        assertThat(split.gstAmount().scale()).isEqualTo(2);
    }

    @Test
    void gstIsNeverTheOldEighteenPercentOfTheTotal() {
        // The previous split took 18% of the GST-inclusive total (900.00 for 5000), overstating GST
        assertThat(GstCalculator.splitInclusiveTotal(new BigDecimal("5000")).gstAmount()).isNotEqualByComparingTo("900.00");
    }

    // ---- revenue ---------------------------------------------------------------------------------------------

    private static final LocalDateTime JAN_10 = LocalDateTime.of(2031, 1, 10, 12, 0);
    private static final LocalDateTime FEB_10 = LocalDateTime.of(2031, 2, 10, 12, 0);

    private static Payment payment(long id, String amount, PaymentStatus status, LocalDateTime paidAt) {
        Payment p = Payment.builder().amount(new BigDecimal(amount)).paymentStatus(status).paymentDate(paidAt).build();
        p.setId(id);
        return p;
    }

    private static PaymentRefund refund(Payment payment, RefundStatus status, LocalDateTime completedAt) {
        return PaymentRefund.builder().payment(payment).amount(payment.getAmount()).status(status).completedAt(completedAt).build();
    }

    @Test
    void netRevenueSubtractsOnlyCompletedRefundsAndOnlyOnce() {
        Payment kept = payment(1, "5000.00", PaymentStatus.SUCCESS, JAN_10);
        Payment refunded = payment(2, "7500.00", PaymentStatus.SUCCESS, JAN_10);
        Payment pending = payment(3, "2500.00", PaymentStatus.SUCCESS, JAN_10);
        Payment failed = payment(4, "1000.00", PaymentStatus.SUCCESS, JAN_10);
        Payment notCollected = payment(5, "9999.00", PaymentStatus.FAILED, JAN_10);
        List<Payment> payments = List.of(kept, refunded, pending, failed, notCollected);
        List<PaymentRefund> refunds = List.of(
                refund(refunded, RefundStatus.REFUNDED, FEB_10),
                refund(pending, RefundStatus.REFUND_PENDING, null),
                refund(failed, RefundStatus.REFUND_FAILED, null));

        RevenueCalculator.Totals totals = RevenueCalculator.totals(payments, refunds);

        assertThat(totals.grossCollected()).isEqualByComparingTo("16000.00");
        assertThat(totals.refundsCompletedAmount()).isEqualByComparingTo("7500.00");
        assertThat(totals.refundsCompletedCount()).isEqualTo(1);
        assertThat(totals.refundsPendingAmount()).isEqualByComparingTo("2500.00");
        assertThat(totals.refundsPendingCount()).isEqualTo(1);
        assertThat(totals.refundsFailedAmount()).isEqualByComparingTo("1000.00");
        assertThat(totals.refundsFailedCount()).isEqualTo(1);
        assertThat(totals.netRevenue()).isEqualByComparingTo("8500.00");   // 16000 - 7500

        assertThat(RevenueCalculator.netForPayments(List.of(kept, refunded), refunds)).isEqualByComparingTo("5000.00");
        assertThat(RevenueCalculator.netForPayments(List.of(pending, failed), refunds)).isEqualByComparingTo("3500.00");
    }

    @Test
    void periodRevenueIsCashBasisCollectionsByPaymentDateRefundsByCompletionDate() {
        Payment refunded = payment(2, "7500.00", PaymentStatus.SUCCESS, JAN_10);
        List<PaymentRefund> refunds = List.of(refund(refunded, RefundStatus.REFUNDED, FEB_10));
        LocalDateTime jan = LocalDateTime.of(2031, 1, 1, 0, 0);
        LocalDateTime feb = LocalDateTime.of(2031, 2, 1, 0, 0);
        LocalDateTime mar = LocalDateTime.of(2031, 3, 1, 0, 0);

        assertThat(RevenueCalculator.netInPeriod(List.of(refunded), refunds, jan, feb)).isEqualByComparingTo("7500.00");
        assertThat(RevenueCalculator.netInPeriod(List.of(refunded), refunds, feb, mar)).isEqualByComparingTo("-7500.00");
        assertThat(RevenueCalculator.netInPeriod(List.of(refunded), refunds, jan, mar)).isEqualByComparingTo("0.00");
    }

    @Test
    void aggregateTotalsUseTheSameDefinitionsAndTreatMissingRefundsAsZero() {
        PaymentRefundRepository repo = mock(PaymentRefundRepository.class);
        when(repo.sumAmountByStatus(RefundStatus.REFUNDED)).thenReturn(new BigDecimal("7500.00"));
        when(repo.countByStatus(RefundStatus.REFUNDED)).thenReturn(1L);
        when(repo.sumAmountByStatus(RefundStatus.REFUND_FAILED)).thenReturn(new BigDecimal("1000.00"));
        when(repo.countByStatus(RefundStatus.REFUND_FAILED)).thenReturn(1L);
        when(repo.sumAmountByStatus(RefundStatus.REFUND_PENDING)).thenReturn(null);

        RevenueCalculator.Totals platform = RevenueCalculator.platformTotals(new BigDecimal("16000.00"), repo);
        assertThat(platform.netRevenue()).isEqualByComparingTo("8500.00");
        assertThat(platform.refundsFailedAmount()).isEqualByComparingTo("1000.00");
        assertThat(platform.refundsPendingAmount()).isEqualByComparingTo("0");

        when(repo.sumAmountByPartnerIdAndStatus(7L, RefundStatus.REFUNDED)).thenReturn(new BigDecimal("2500.00"));
        assertThat(RevenueCalculator.partnerTotals(new BigDecimal("5000.00"), 7L, repo).netRevenue()).isEqualByComparingTo("2500.00");

        assertThat(RevenueCalculator.platformTotals(null, null).netRevenue()).isEqualByComparingTo("0");
        assertThat(RevenueCalculator.partnerTotals(new BigDecimal("5000.00"), 7L, null).netRevenue()).isEqualByComparingTo("5000.00");
    }
}
