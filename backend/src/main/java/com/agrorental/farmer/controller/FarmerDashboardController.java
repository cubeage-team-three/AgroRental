package com.agrorental.farmer.controller;

import com.agrorental.common.dto.ApiResponse;
import com.agrorental.farmer.dto.FarmerDashboardResponse;
import com.agrorental.farmer.service.FarmerDashboardService;
import com.agrorental.security.principal.FarmerPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/farmers")
@RequiredArgsConstructor
public class FarmerDashboardController {

    private final FarmerDashboardService farmerDashboardService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<FarmerDashboardResponse>> getDashboard(@AuthenticationPrincipal FarmerPrincipal principal) {
        log.info("REST request to fetch own dashboard data for farmer ID: {}", principal.getId());
        FarmerDashboardResponse dashboardData = farmerDashboardService.getDashboardData(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Farmer dashboard summary retrieved successfully.", dashboardData));
    }

    @GetMapping("/dashboard/{farmerId}")
    public ResponseEntity<ApiResponse<FarmerDashboardResponse>> getDashboardById(@PathVariable Long farmerId,
                                                                                 @AuthenticationPrincipal FarmerPrincipal principal) {
        // The path ID is never trusted: a farmer may only load its own dashboard
        if (principal == null || !principal.getId().equals(farmerId)) {
            throw new AccessDeniedException("Access is denied. You can only view your own dashboard.");
        }
        log.info("REST request to fetch dashboard data for farmer ID: {}", principal.getId());
        FarmerDashboardResponse dashboardData = farmerDashboardService.getDashboardData(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Farmer dashboard summary retrieved successfully.", dashboardData));
    }
}
