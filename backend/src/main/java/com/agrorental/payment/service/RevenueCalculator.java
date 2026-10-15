package com.agrorental.payment.service;

import com.agrorental.payment.entity.Payment;
import com.agrorental.payment.entity.PaymentRefund;
import com.agrorental.payment.entity.PaymentStatus;
import com.agrorental.payment.entity.RefundStatus;
import com.agrorental.payment.repository.PaymentRefundRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Single definition of revenue figures used by partner and admin aggregates (BIZ-20):
 * <ul>
 *   <li><b>Gross collected</b>: sum of SUCCESS payments (original collections, never rewritten).</li>
 *   <li><b>Refunds completed</b>: REFUNDED refunds (verified transfer recorded by an admin).</li>
 *   <li><b>Net revenue</b> = gross collected - refunds completed (each completed refund subtracted exactly once).</li>
 *   <li><b>Refunds pending</b> (REFUND_PENDING) and <b>refunds failed</b> (REFUND_FAILED) are reported separately
 *       and NOT subtracted: no money has left yet; a failed refund is still owed to the farmer.</li>
 * </ul>
 * Period figures are cash-basis: payments count in the period of their payment date, completed refunds are
 * subtracted in the period of their completion date.
 */
public final class RevenueCalculator {

    private RevenueCalculator() {
    }

    public record Totals(BigDecimal grossCollected,
                         BigDecimal refundsPendingAmount, long refundsPendingCount,
                         BigDecimal refundsFailedAmount, long refundsFailedCount,
                         BigDecimal refundsCompletedAmount, long refundsCompletedCount) {

        public BigDecimal netRevenue() {
            return grossCollected.subtract(refundsCompletedAmount);
        }
    }

    public static Totals totals(Collection<Payment> payments, Collection<PaymentRefund> refunds) {
        BigDecimal gross = sum(payments.stream().filter(RevenueCalculator::isCollected).map(Payment::getAmount).toList());
        return new Totals(gross,
                refundSum(refunds, RefundStatus.REFUND_PENDING), refundCount(refunds, RefundStatus.REFUND_PENDING),
                refundSum(refunds, RefundStatus.REFUND_FAILED), refundCount(refunds, RefundStatus.REFUND_FAILED),
                refundSum(refunds, RefundStatus.REFUNDED), refundCount(refunds, RefundStatus.REFUNDED));
    }

    /** Platform-wide totals from aggregate queries (admin dashboards). A null refund repository means no refunds. */
    public static Totals platformTotals(BigDecimal grossCollected, PaymentRefundRepository refunds) {
        BigDecimal gross = grossCollected != null ? grossCollected : BigDecimal.ZERO;
        if (refunds == null) {
            return new Totals(gross, BigDecimal.ZERO, 0, BigDecimal.ZERO, 0, BigDecimal.ZERO, 0);
        }
        return new Totals(gross,
                zeroIfNull(refunds.sumAmountByStatus(RefundStatus.REFUND_PENDING)), refunds.countByStatus(RefundStatus.REFUND_PENDING),
                zeroIfNull(refunds.sumAmountByStatus(RefundStatus.REFUND_FAILED)), refunds.countByStatus(RefundStatus.REFUND_FAILED),
                zeroIfNull(refunds.sumAmountByStatus(RefundStatus.REFUNDED)), refunds.countByStatus(RefundStatus.REFUNDED));
    }

    /** One partner's totals from aggregate queries. A null refund repository means no refunds. */
    public static Totals partnerTotals(BigDecimal grossCollected, Long partnerId, PaymentRefundRepository refunds) {
        BigDecimal gross = grossCollected != null ? grossCollected : BigDecimal.ZERO;
        if (refunds == null) {
            return new Totals(gross, BigDecimal.ZERO, 0, BigDecimal.ZERO, 0, BigDecimal.ZERO, 0);
        }
        return new Totals(gross,
                zeroIfNull(refunds.sumAmountByPartnerIdAndStatus(partnerId, RefundStatus.REFUND_PENDING)),
                refunds.countByPartnerIdAndStatus(partnerId, RefundStatus.REFUND_PENDING),
                zeroIfNull(refunds.sumAmountByPartnerIdAndStatus(partnerId, RefundStatus.REFUND_FAILED)),
                refunds.countByPartnerIdAndStatus(partnerId, RefundStatus.REFUND_FAILED),
                zeroIfNull(refunds.sumAmountByPartnerIdAndStatus(partnerId, RefundStatus.REFUNDED)),
                refunds.countByPartnerIdAndStatus(partnerId, RefundStatus.REFUNDED));
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    /** Net revenue in [from, toExclusive): collections by payment date minus refunds completed in the period. */
    public static BigDecimal netInPeriod(Collection<Payment> payments, Collection<PaymentRefund> refunds,
                                         LocalDateTime from, LocalDateTime toExclusive) {
        BigDecimal collected = sum(payments.stream()
                .filter(RevenueCalculator::isCollected)
                .filter(p -> within(p.getPaymentDate(), from, toExclusive))
                .map(Payment::getAmount).toList());
        BigDecimal refunded = sum(refunds.stream()
                .filter(r -> r.getStatus() == RefundStatus.REFUNDED && within(r.getCompletedAt(), from, toExclusive))
                .map(PaymentRefund::getAmount).toList());
        return collected.subtract(refunded);
    }

    /** Net revenue attributable to a set of payments (e.g. one equipment item): collections minus their completed refunds. */
    public static BigDecimal netForPayments(Collection<Payment> payments, Collection<PaymentRefund> refunds) {
        List<Long> paymentIds = payments.stream().filter(RevenueCalculator::isCollected).map(Payment::getId).toList();
        BigDecimal collected = sum(payments.stream().filter(RevenueCalculator::isCollected).map(Payment::getAmount).toList());
        BigDecimal refunded = sum(refunds.stream()
                .filter(r -> r.getStatus() == RefundStatus.REFUNDED && r.getPayment() != null
                        && paymentIds.contains(r.getPayment().getId()))
                .map(PaymentRefund::getAmount).toList());
        return collected.subtract(refunded);
    }

    private static boolean isCollected(Payment payment) {
        return payment.getPaymentStatus() == PaymentStatus.SUCCESS && payment.getAmount() != null;
    }

    private static boolean within(LocalDateTime at, LocalDateTime from, LocalDateTime toExclusive) {
        return at != null && !at.isBefore(from) && at.isBefore(toExclusive);
    }

    private static BigDecimal refundSum(Collection<PaymentRefund> refunds, RefundStatus status) {
        return sum(refunds.stream().filter(r -> r.getStatus() == status).map(PaymentRefund::getAmount).toList());
    }

    private static long refundCount(Collection<PaymentRefund> refunds, RefundStatus status) {
        return refunds.stream().filter(r -> r.getStatus() == status).count();
    }

    private static BigDecimal sum(List<BigDecimal> amounts) {
        return amounts.stream().filter(a -> a != null).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
