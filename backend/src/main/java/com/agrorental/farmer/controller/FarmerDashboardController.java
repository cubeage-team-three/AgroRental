package com.agrorental.farmer.controller;

import com.agrorental.common.dto.ApiResponse;
import com.agrorental.farmer.dto.FarmerDashboardResponse;
import com.agrorental.farmer.service.FarmerDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/farmers")
@RequiredArgsConstructor
public class FarmerDashboardController {

    private final FarmerDashboardService farmerDashboardService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<FarmerDashboardResponse>> getDashboard(@RequestParam(required = false, defaultValue = "1") Long farmerId) {
        log.info("REST request to fetch dashboard data for farmer ID: {}", farmerId);
        FarmerDashboardResponse dashboardData = farmerDashboardService.getDashboardData(farmerId);
        return ResponseEntity.ok(ApiResponse.success("Farmer dashboard summary retrieved successfully.", dashboardData));
    }

    @GetMapping("/dashboard/{farmerId}")
    public ResponseEntity<ApiResponse<FarmerDashboardResponse>> getDashboardById(@PathVariable Long farmerId) {
        log.info("REST request to fetch dashboard data for farmer ID: {}", farmerId);
        FarmerDashboardResponse dashboardData = farmerDashboardService.getDashboardData(farmerId);
        return ResponseEntity.ok(ApiResponse.success("Farmer dashboard summary retrieved successfully.", dashboardData));
    }
}
