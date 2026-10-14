package com.agrorental.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Aggregate platform-wide booking metrics for the Admin Booking Management summary cards.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminBookingStatsResponse {

    private long totalBookings;
    private long pendingBookings;
    private long activeBookings;
    private long completedBookings;
    private long cancelledBookings;
    private BigDecimal totalRevenue;
    private BigDecimal totalAmount;
}
