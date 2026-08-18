package com.agrorental.farmer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FarmerDashboardResponse {

    private FarmerProfileResponse profileSummary;
    private long totalFarmsCount;
    private long activeBookingsCount;
    private long completedBookingsCount;
    private double totalSpentAmount;
    private String activeBookingStatusMessage;
    private List<RecentBookingSummary> recentBookings;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecentBookingSummary {
        private String bookingId;
        private String equipmentName;
        private String bookingDate;
        private String status;
        private String totalCost;
    }
}
