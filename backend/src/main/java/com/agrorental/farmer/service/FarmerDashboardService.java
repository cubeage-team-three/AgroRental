package com.agrorental.farmer.service;

import com.agrorental.farmer.dto.FarmerDashboardResponse;
import com.agrorental.farmer.dto.FarmerProfileResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FarmerDashboardService {

    private final FarmerProfileService farmerProfileService;

    public FarmerDashboardResponse getDashboardData(Long farmerId) {
        log.info("Generating dashboard summary for farmer ID: {}", farmerId);

        FarmerProfileResponse profile = farmerProfileService.getProfile(farmerId);

        // Dummy aggregate metrics for dashboard view
        List<FarmerDashboardResponse.RecentBookingSummary> recentBookings = new ArrayList<>();
        recentBookings.add(FarmerDashboardResponse.RecentBookingSummary.builder()
                .bookingId("BK-2026-0891")
                .equipmentName("Mahindra 575 DI Tractor")
                .bookingDate("18 Aug 2026")
                .status("ACCEPTED")
                .totalCost("₹3,200")
                .build());

        recentBookings.add(FarmerDashboardResponse.RecentBookingSummary.builder()
                .bookingId("BK-2026-0744")
                .equipmentName("John Deere Rotavator 6ft")
                .bookingDate("12 Aug 2026")
                .status("COMPLETED")
                .totalCost("₹1,800")
                .build());

        return FarmerDashboardResponse.builder()
                .profileSummary(profile)
                .totalFarmsCount(2)
                .activeBookingsCount(1)
                .completedBookingsCount(4)
                .totalSpentAmount(12500.00)
                .activeBookingStatusMessage("Tractor #BK-2026-0891 is confirmed for tomorrow 9:00 AM.")
                .recentBookings(recentBookings)
                .build();
    }
}
