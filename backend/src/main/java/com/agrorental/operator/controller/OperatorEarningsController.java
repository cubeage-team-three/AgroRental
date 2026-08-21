package com.agrorental.operator.controller;

import com.agrorental.common.dto.ApiResponse;
import com.agrorental.common.exception.UnauthorizedException;
import com.agrorental.operator.dto.OperatorEarningsHistoryResponse;
import com.agrorental.operator.dto.OperatorEarningsSummaryResponse;
import com.agrorental.operator.dto.OperatorJobEarningsResponse;
import com.agrorental.operator.service.OperatorEarningsService;
import com.agrorental.security.principal.OperatorPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller exposing Operator work duration, pause intervals, and gross earnings endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/api/operators")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:5174",
        "http://localhost:5175",
        "http://localhost:5176"
})
@RequiredArgsConstructor
public class OperatorEarningsController {

    private final OperatorEarningsService earningsService;

    private Long validatePrincipal(OperatorPrincipal principal) {
        if (principal == null) {
            log.warn("Unauthorized earnings query attempt without principal");
            throw new UnauthorizedException("Full authentication is required to access this resource");
        }
        return principal.getId();
    }

    /**
     * Retrieves work duration and calculated earnings for a specific job assignment.
     */
    @GetMapping("/jobs/{assignmentId}/earnings")
    public ResponseEntity<ApiResponse<OperatorJobEarningsResponse>> getJobEarnings(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal OperatorPrincipal principal) {

        Long operatorId = validatePrincipal(principal);
        log.info("REST request: Operator {} fetching earnings for assignment {}", operatorId, assignmentId);

        OperatorJobEarningsResponse response = earningsService.getJobEarnings(assignmentId, operatorId);
        return ResponseEntity.ok(ApiResponse.success("Job earnings calculated successfully", response));
    }

    /**
     * Retrieves aggregate earnings summary and total logged work hours for the authenticated operator.
     */
    @GetMapping("/earnings/summary")
    public ResponseEntity<ApiResponse<OperatorEarningsSummaryResponse>> getEarningsSummary(
            @AuthenticationPrincipal OperatorPrincipal principal) {

        Long operatorId = validatePrincipal(principal);
        log.info("REST request: Operator {} fetching aggregate earnings summary", operatorId);

        OperatorEarningsSummaryResponse response = earningsService.getEarningsSummary(operatorId);
        return ResponseEntity.ok(ApiResponse.success("Earnings summary retrieved successfully", response));
    }

    /**
     * Retrieves paginated completed jobs earnings history for the authenticated operator.
     */
    @GetMapping("/earnings/history")
    public ResponseEntity<ApiResponse<Page<OperatorEarningsHistoryResponse>>> getEarningsHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal OperatorPrincipal principal) {

        Long operatorId = validatePrincipal(principal);
        log.info("REST request: Operator {} fetching earnings history page {} size {}", operatorId, page, size);

        int validPage = Math.max(0, page);
        int validSize = (size <= 0 || size > 50) ? 10 : size;
        Pageable pageable = PageRequest.of(validPage, validSize, Sort.by(Sort.Direction.DESC, "completedAt"));

        Page<OperatorEarningsHistoryResponse> response = earningsService.getEarningsHistory(operatorId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Earnings history retrieved successfully", response));
    }
}
