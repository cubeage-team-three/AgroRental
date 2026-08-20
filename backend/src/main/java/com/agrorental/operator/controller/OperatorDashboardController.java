package com.agrorental.operator.controller;

import com.agrorental.common.dto.ApiResponse;
import com.agrorental.common.exception.UnauthorizedException;
import com.agrorental.operator.dto.OperatorDashboardMetricsResponse;
import com.agrorental.operator.service.OperatorDashboardService;
import com.agrorental.security.principal.OperatorPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for Operator Dashboard metrics and real-time operational status.
 */
@Slf4j
@RestController
@RequestMapping("/api/operators/dashboard")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:5174",
        "http://localhost:5175",
        "http://localhost:5176"
})
@RequiredArgsConstructor
public class OperatorDashboardController {

    private final OperatorDashboardService dashboardService;

    private Long validatePrincipal(OperatorPrincipal principal) {
        if (principal == null) {
            log.warn("Unauthorized /api/operators/dashboard/metrics access attempt without principal");
            throw new UnauthorizedException("Full authentication is required to access this resource");
        }
        return principal.getId();
    }

    /**
     * Retrieves aggregated dashboard metrics, status counts, completion rates, and current active task.
     */
    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<OperatorDashboardMetricsResponse>> getDashboardMetrics(
            @AuthenticationPrincipal OperatorPrincipal principal) {

        Long operatorId = validatePrincipal(principal);
        log.info("REST request to fetch dashboard metrics for operator ID: {}", operatorId);

        OperatorDashboardMetricsResponse response = dashboardService.getDashboardMetrics(operatorId);
        return ResponseEntity.ok(ApiResponse.success("Operator dashboard metrics retrieved successfully", response));
    }
}
