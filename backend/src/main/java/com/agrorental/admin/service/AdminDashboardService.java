package com.agrorental.admin.service;

import com.agrorental.admin.dto.AdminDashboardStatsResponse;
import com.agrorental.booking.dto.BookingResponse;
import com.agrorental.booking.mapper.BookingMapper;
import com.agrorental.booking.repository.BookingRepository;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.payment.entity.PaymentStatus;
import com.agrorental.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
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

    @Transactional(readOnly = true)
    public AdminDashboardStatsResponse getDashboardStats() {
        return AdminDashboardStatsResponse.builder()
                .totalFarmers(farmerRepository.count())
                .activeOperators(operatorRepository.countByStatus(OperatorStatus.APPROVED))
                .totalRevenue(paymentRepository.sumAmountByStatus(PaymentStatus.SUCCESS))
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
