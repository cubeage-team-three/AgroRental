package com.agrorental.partner.dto;

import com.agrorental.partner.entity.Partner;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerDashboardResponse {

    private Long id;
    private String fullName;
    private String businessName;
    private String mobileNumber;
    private String email;
    private String address;
    private String profilePhoto;
    private boolean otpVerified;
    private Partner.VerificationStatus verificationStatus;

    // 7 KPI Widgets (FR-22)
    private long totalMachines;
    private long activeMachines;
    private long pendingBookings;
    private long completedBookings;
    private BigDecimal monthlyRevenue;
    private long activeOperators;
    private double customerRatings;

    // 5 Analytical Chart Series (FR-22)
    private List<MonthlyChartEntry> monthlyRevenueChart;
    private List<MonthlyCountEntry> bookingTrendChart;
    private List<MachineUtilizationEntry> machineUtilization;
    private List<MonthlyCountEntry> customerGrowth;
    private Map<String, Long> bookingStatusDistribution;

    // Record compatibility accessors for test suites
    public Long id() { return id; }
    public String fullName() { return fullName; }
    public String businessName() { return businessName; }
    public String mobileNumber() { return mobileNumber; }
    public String email() { return email; }
    public String address() { return address; }
    public boolean otpVerified() { return otpVerified; }
    public Partner.VerificationStatus verificationStatus() { return verificationStatus; }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MonthlyChartEntry {
        private String month;
        private BigDecimal revenue;
        private long bookingsCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MonthlyCountEntry {
        private String month;
        private long count;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MachineUtilizationEntry {
        private Long equipmentId;
        private String machineName;
        private String category;
        private long totalBookings;
        private long totalRentalDays;
        private double utilizationRate;
    }
}