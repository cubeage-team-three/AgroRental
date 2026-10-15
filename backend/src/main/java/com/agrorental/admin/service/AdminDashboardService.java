package com.agrorental.admin.service;

import com.agrorental.admin.dto.AdminDashboardStatsResponse;
import com.agrorental.booking.dto.BookingResponse;
import com.agrorental.booking.mapper.BookingMapper;
import com.agrorental.booking.repository.BookingRepository;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.payment.entity.PaymentStatus;
import com.agrorental.payment.repository.PaymentRefundRepository;
import com.agrorental.payment.repository.PaymentRepository;
import com.agrorental.payment.service.RevenueCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Aggregates platform-wide metrics and recent activity for the Admin Overview panel.
 */
@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final FarmerRepository farmerRepository;
    private final OperatorRepository operatorRepository;
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;

    /** Refund records (BIZ-20); headline revenue is net of completed refunds (see RevenueCalculator). */
    @Autowired(required = false)
    private PaymentRefundRepository paymentRefundRepository;

    @Transactional(readOnly = true)
    public AdminDashboardStatsResponse getDashboardStats() {
        RevenueCalculator.Totals totals = RevenueCalculator.platformTotals(
                paymentRepository.sumAmountByStatus(PaymentStatus.SUCCESS), paymentRefundRepository);
        return AdminDashboardStatsResponse.builder()
                .totalFarmers(farmerRepository.count())
                .activeOperators(operatorRepository.countByStatus(OperatorStatus.APPROVED))
                .totalRevenue(totals.netRevenue())
                .grossCollected(totals.grossCollected())
                .refundsPendingAmount(totals.refundsPendingAmount())
                .refundsPendingCount(totals.refundsPendingCount())
                .refundsFailedAmount(totals.refundsFailedAmount())
                .refundsFailedCount(totals.refundsFailedCount())
                .refundsCompletedAmount(totals.refundsCompletedAmount())
                .refundsCompletedCount(totals.refundsCompletedCount())
                .pendingApprovals(operatorRepository.countByStatus(OperatorStatus.PENDING))
                .build();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getRecentBookings() {
        return bookingRepository.findTop10ByOrderByCreatedAtDesc().stream()
                .map(bookingMapper::toResponse)
                .toList();
    }
}
