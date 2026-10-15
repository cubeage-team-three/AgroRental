package com.agrorental.operator.controller;

import com.agrorental.common.dto.ApiResponse;
import com.agrorental.common.exception.UnauthorizedException;
import com.agrorental.operator.dto.EligibleOperatorResponse;
import com.agrorental.operator.dto.OperatorAssignedJobResponse;
import com.agrorental.operator.dto.OperatorAssignmentRequest;
import com.agrorental.operator.dto.OperatorAssignmentResponse;
import com.agrorental.operator.dto.OperatorJobHistoryResponse;
import com.agrorental.operator.dto.OperatorJobHistorySummaryResponse;
import com.agrorental.operator.enums.OperatorAssignmentStatus;
import com.agrorental.operator.service.OperatorAssignmentService;
import com.agrorental.security.principal.CurrentCaller;
import com.agrorental.security.principal.OperatorPrincipal;
import com.agrorental.security.principal.PartnerPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * REST Controller managing Operator Job Assignments, partner/admin operator assignments,
 * eligible operator discovery, and authenticated operator job retrieval.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class OperatorAssignmentController {

    private final OperatorAssignmentService assignmentService;

    // ==========================================
    // PARTNER / ADMIN ASSIGNMENT ENDPOINTS
    // ==========================================

    /**
     * Assigns an eligible Operator to a confirmed Booking (Partner/Admin).
     *
     * @param bookingId Booking identifier
     * @param request Validated assignment payload
     * @param authentication Current security authentication context
     * @return ApiResponse containing OperatorAssignmentResponse
     */
    @PostMapping("/api/bookings/{bookingId}/operator")
    public ResponseEntity<ApiResponse<OperatorAssignmentResponse>> assignOperator(
            @PathVariable Long bookingId,
            @Valid @RequestBody OperatorAssignmentRequest request,
            Authentication authentication) {

        // Only an authenticated partner (for its own bookings) or an admin may assign; the acting
        // identity comes solely from the JWT principal, never from the request.
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required.");
        }
        String assignedBy = authentication.getName();
        OperatorAssignmentResponse response;
        if (authentication.getPrincipal() instanceof PartnerPrincipal partner) {
            log.info("REST request to assign operator {} to booking {} by partner {}",
                    request.getOperatorId(), bookingId, partner.getId());
            response = assignmentService.assignOperator(bookingId, request, assignedBy, partner.getId());
        } else if (authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
            log.info("REST request to assign operator {} to booking {} by admin {}",
                    request.getOperatorId(), bookingId, assignedBy);
            response = assignmentService.assignOperator(bookingId, request, assignedBy);
        } else {
            throw new AccessDeniedException("Only partners and administrators can assign operators.");
        }

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Operator assigned to booking successfully", response));
    }

    /**
     * Retrieves the active operator assignment for a booking (Partner/Admin).
     *
     * @param bookingId Booking identifier
     * @return ApiResponse containing OperatorAssignmentResponse
     */
    @GetMapping("/api/bookings/{bookingId}/operator")
    public ResponseEntity<ApiResponse<OperatorAssignmentResponse>> getBookingAssignment(
            @PathVariable Long bookingId,
            Authentication authentication) {

        log.info("REST request to fetch operator assignment for booking ID: {}", bookingId);
        // Owning partner or admin only (enforced in the service)
        OperatorAssignmentResponse response = assignmentService.getBookingAssignmentForCaller(bookingId,
                CurrentCaller.from(authentication));

        return ResponseEntity.ok(
                ApiResponse.success("Booking operator assignment retrieved successfully", response)
        );
    }

    /**
     * Searches and lists eligible, approved operators for assignment (Partner/Admin).
     *
     * @param search Optional keyword search query
     * @param pageable Pagination configuration
     * @return ApiResponse containing Page of EligibleOperatorResponse
     */
    @GetMapping("/api/operators/eligible")
    public ResponseEntity<ApiResponse<Page<EligibleOperatorResponse>>> getEligibleOperators(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
            Authentication authentication) {

        log.info("REST request to search eligible operators with query: '{}'", search);
        // Partners get a minimized view (masked mobile, no address); admins the full view; others are refused
        Page<EligibleOperatorResponse> response = assignmentService.findEligibleOperatorsForCaller(search, pageable,
                CurrentCaller.from(authentication));

        return ResponseEntity.ok(
                ApiResponse.success("Eligible operators retrieved successfully", response)
        );
    }

    // ==========================================
    // AUTHENTICATED OPERATOR JOB ENDPOINTS
    // ==========================================

    /**
     * Retrieves assigned field jobs for the authenticated Operator.
     *
     * @param principal Authenticated operator principal from JWT
     * @param pageable Pagination configuration
     * @return ApiResponse containing Page of OperatorAssignedJobResponse
     */
    @GetMapping("/api/operators/jobs/assigned")
    public ResponseEntity<ApiResponse<Page<OperatorAssignedJobResponse>>> getAssignedJobs(
            @AuthenticationPrincipal OperatorPrincipal principal,
            @PageableDefault(size = 10, sort = "assignedAt", direction = Sort.Direction.DESC) Pageable pageable) {

        if (principal == null) {
            log.warn("Unauthorized /api/operators/jobs/assigned access attempt without principal");
            throw new UnauthorizedException("Full authentication is required to access this resource");
        }

        log.info("REST request to fetch assigned jobs for operator ID: {}", principal.getId());
        Page<OperatorAssignedJobResponse> response = assignmentService.getAssignedJobs(principal.getId(), pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Assigned jobs retrieved successfully", response)
        );
    }

    /**
     * Retrieves specific assigned job details for the authenticated Operator.
     *
     * @param assignmentId Target assignment ID
     * @param principal Authenticated operator principal from JWT
     * @return ApiResponse containing OperatorAssignedJobResponse
     */
    @GetMapping("/api/operators/jobs/{assignmentId}")
    public ResponseEntity<ApiResponse<OperatorAssignedJobResponse>> getAssignedJob(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal OperatorPrincipal principal) {

        if (principal == null) {
            log.warn("Unauthorized /api/operators/jobs/{} access attempt without principal", assignmentId);
            throw new UnauthorizedException("Full authentication is required to access this resource");
        }

        log.info("REST request to fetch assignment ID {} for operator ID: {}", assignmentId, principal.getId());
        OperatorAssignedJobResponse response = assignmentService.getAssignedJob(principal.getId(), assignmentId);

        return ResponseEntity.ok(
                ApiResponse.success("Job assignment details retrieved successfully", response)
        );
    }

    // ==========================================
    // PHASE 10: JOB HISTORY & ANALYTICS ENDPOINTS
    // ==========================================

    /**
     * Retrieves a paginated, filterable archive of historical jobs for the authenticated Operator.
     */
    @GetMapping("/api/operators/jobs/history")
    public ResponseEntity<ApiResponse<Page<OperatorJobHistoryResponse>>> getJobHistory(
            @AuthenticationPrincipal OperatorPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) OperatorAssignmentStatus status,
            @RequestParam(required = false) String equipmentCategory,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "completedAt", direction = Sort.Direction.DESC) Pageable pageable) {

        if (principal == null) {
            log.warn("Unauthorized /api/operators/jobs/history access attempt without principal");
            throw new UnauthorizedException("Full authentication is required to access this resource");
        }

        log.info("REST request: Operator ID {} fetching job history archive", principal.getId());
        Page<OperatorJobHistoryResponse> response = assignmentService.getJobHistory(
                principal.getId(),
                startDate,
                endDate,
                status,
                equipmentCategory,
                search,
                pageable
        );

        return ResponseEntity.ok(
                ApiResponse.success("Job history retrieved successfully", response)
        );
    }

    /**
     * Retrieves aggregated historical performance analytics for the authenticated Operator.
     */
    @GetMapping("/api/operators/jobs/history/summary")
    public ResponseEntity<ApiResponse<OperatorJobHistorySummaryResponse>> getJobHistorySummary(
            @AuthenticationPrincipal OperatorPrincipal principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String equipmentCategory) {

        if (principal == null) {
            log.warn("Unauthorized /api/operators/jobs/history/summary access attempt without principal");
            throw new UnauthorizedException("Full authentication is required to access this resource");
        }

        log.info("REST request: Operator ID {} fetching job history summary", principal.getId());
        OperatorJobHistorySummaryResponse response = assignmentService.getJobHistorySummary(
                principal.getId(),
                startDate,
                endDate,
                equipmentCategory
        );

        return ResponseEntity.ok(
                ApiResponse.success("Job history summary retrieved successfully", response)
        );
    }
}
