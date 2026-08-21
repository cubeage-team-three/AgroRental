package com.agrorental.farmer.service;

import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.booking.repository.BookingRepository;
import com.agrorental.farmer.dto.FarmerDashboardResponse;
import com.agrorental.farmer.dto.FarmerProfileResponse;
import com.agrorental.farmer.repository.FarmRepository;
import com.agrorental.payment.entity.Payment;
import com.agrorental.payment.entity.PaymentStatus;
import com.agrorental.payment.repository.PaymentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FarmerDashboardService {

    private final FarmerProfileService farmerProfileService;
    private final FarmRepository farmRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;

    public FarmerDashboardService(
            FarmerProfileService farmerProfileService,
            FarmRepository farmRepository,
            BookingRepository bookingRepository,
            PaymentRepository paymentRepository) {
        this.farmerProfileService = farmerProfileService;
        this.farmRepository = farmRepository;
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional(readOnly = true)
    public FarmerDashboardResponse getDashboardData(Long farmerId) {
        log.info("Calculating dynamic dashboard summary for farmer ID: {}", farmerId);

        FarmerProfileResponse profile = farmerProfileService.getProfile(farmerId);

        // Calculate real DB metrics
        int totalFarmsCount = farmRepository.findByFarmerId(farmerId).size();

        List<Booking> allFarmerBookings = bookingRepository.findByFarmerId(farmerId);

        int activeBookingsCount = (int) allFarmerBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.PENDING || 
                             b.getStatus() == BookingStatus.CONFIRMED)
                .count();

        int completedBookingsCount = (int) allFarmerBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.COMPLETED)
                .count();

        List<Payment> payments = paymentRepository.findByFarmerIdOrderByCreatedAtDesc(farmerId);
        double totalSpentAmount = payments.stream()
                .filter(p -> p.getPaymentStatus() == PaymentStatus.SUCCESS)
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .doubleValue();

        // Generate status message from active booking if present
        String activeStatusMessage = allFarmerBookings.stream()
                .filter(b -> b.getStatus() != BookingStatus.CANCELLED && b.getStatus() != BookingStatus.COMPLETED)
                .findFirst()
                .map(b -> (b.getEquipment() != null ? b.getEquipment().getName() : "Equipment") + 
                          " Reservation #" + b.getId() + " is currently " + b.getStatus())
                .orElse("No active bookings currently in progress.");

        // Map recent 5 bookings
        List<FarmerDashboardResponse.RecentBookingSummary> recentBookings = allFarmerBookings.stream()
                .sorted((b1, b2) -> Long.compare(b2.getId(), b1.getId()))
                .limit(5)
                .map(b -> FarmerDashboardResponse.RecentBookingSummary.builder()
                        .bookingId("BK-" + b.getId())
                        .equipmentName(b.getEquipment() != null ? b.getEquipment().getName() : "Agricultural Equipment")
                        .bookingDate(b.getStartDate() != null ? b.getStartDate().toString() : "N/A")
                        .status(b.getStatus() != null ? b.getStatus().name() : "PENDING")
                        .totalCost("₹" + (b.getTotalCost() != null ? b.getTotalCost().toPlainString() : "0"))
                        .build())
                .collect(Collectors.toList());

        return FarmerDashboardResponse.builder()
                .profileSummary(profile)
                .totalFarmsCount(totalFarmsCount)
                .activeBookingsCount(activeBookingsCount)
                .completedBookingsCount(completedBookingsCount)
                .totalSpentAmount(totalSpentAmount)
                .activeBookingStatusMessage(activeStatusMessage)
                .recentBookings(recentBookings)
                .build();
    }
}
