package com.agrorental.admin.controller;

import com.agrorental.admin.dto.AdminDashboardStatsResponse;
import com.agrorental.admin.service.AdminDashboardService;
import com.agrorental.booking.dto.BookingResponse;
import com.agrorental.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller exposing platform-wide metrics for the Admin Overview panel.
 * Protected by SecurityConfig's "/api/admin/**" -&gt; hasRole("ADMIN") rule.
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminDashboardStatsResponse>> getStats() {
        AdminDashboardStatsResponse stats = adminDashboardService.getDashboardStats();
        return ResponseEntity.ok(ApiResponse.success("Dashboard stats retrieved successfully", stats));
    }

    @GetMapping("/recent-bookings")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getRecentBookings() {
        List<BookingResponse> bookings = adminDashboardService.getRecentBookings();
        return ResponseEntity.ok(ApiResponse.success("Recent bookings retrieved successfully", bookings));
    }
}
