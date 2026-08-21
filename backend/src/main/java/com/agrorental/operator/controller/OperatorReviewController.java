package com.agrorental.operator.controller;

import com.agrorental.common.dto.ApiResponse;
import com.agrorental.common.exception.ForbiddenException;
import com.agrorental.common.exception.UnauthorizedException;
import com.agrorental.operator.dto.OperatorRatingSummaryResponse;
import com.agrorental.operator.dto.OperatorReviewCreateRequest;
import com.agrorental.operator.dto.OperatorReviewResponse;
import com.agrorental.operator.service.OperatorReviewService;
import com.agrorental.security.principal.OperatorPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for operator ratings, star reviews, and customer feedback management.
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
public class OperatorReviewController {

    private final OperatorReviewService reviewService;

    /**
     * Helper to safely extract authenticated Farmer ID from Spring Security Authentication context.
     */
    private Long resolveFarmerId(Authentication auth) {
        Authentication activeAuth = (auth != null && auth.isAuthenticated())
                ? auth
                : org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        if (activeAuth == null || !activeAuth.isAuthenticated()) {
            throw new UnauthorizedException("Full authentication is required to access this resource");
        }
        if (activeAuth.getPrincipal() instanceof OperatorPrincipal) {
            log.warn("Self-review blocked: Authenticated operator attempted to submit assignment review");
            throw new ForbiddenException("Operators cannot submit reviews for assignments");
        }
        Object principal = activeAuth.getPrincipal();
        if (principal instanceof String pStr) {
            if (pStr.startsWith("FARMER_")) {
                try {
                    return Long.parseLong(pStr.substring(7));
                } catch (NumberFormatException ignored) {}
            }
            try {
                return Long.parseLong(pStr);
            } catch (NumberFormatException ignored) {}
        }
        if (principal instanceof Long l) {
            return l;
        }
        throw new ForbiddenException("Access denied: Only registered farmers can review operator assignments");
    }

    /**
     * Submits a star rating and feedback review for a completed assignment (Farmer only).
     */
    @PostMapping("/jobs/{assignmentId}/reviews")
    public ResponseEntity<ApiResponse<OperatorReviewResponse>> createReview(
            @PathVariable Long assignmentId,
            @Valid @RequestBody OperatorReviewCreateRequest request,
            Authentication authentication) {

        Long farmerId = resolveFarmerId(authentication);
        log.info("REST request: Farmer {} submitting review for assignment {}", farmerId, assignmentId);

        OperatorReviewResponse response = reviewService.createOperatorReview(assignmentId, farmerId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Operator review submitted successfully", response));
    }

    /**
     * Retrieves overall rating summary and star distribution for any operator.
     */
    @GetMapping("/{operatorId}/ratings/summary")
    public ResponseEntity<ApiResponse<OperatorRatingSummaryResponse>> getOperatorRatingSummary(
            @PathVariable Long operatorId) {

        log.info("REST request: Fetching rating summary for operator ID: {}", operatorId);
        OperatorRatingSummaryResponse response = reviewService.getOperatorRatingSummary(operatorId);
        return ResponseEntity.ok(ApiResponse.success("Operator rating summary retrieved successfully", response));
    }

    /**
     * Retrieves paginated reviews and feedback for any operator.
     */
    @GetMapping("/{operatorId}/reviews")
    public ResponseEntity<ApiResponse<Page<OperatorReviewResponse>>> getOperatorReviews(
            @PathVariable Long operatorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("REST request: Fetching reviews for operator ID: {}, page: {}, size: {}", operatorId, page, size);
        int validPage = Math.max(0, page);
        int validSize = (size <= 0 || size > 50) ? 10 : size;
        Pageable pageable = PageRequest.of(validPage, validSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<OperatorReviewResponse> response = reviewService.getOperatorReviews(operatorId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Operator reviews retrieved successfully", response));
    }

    /**
     * Retrieves rating summary for the currently authenticated operator.
     */
    @GetMapping("/me/ratings/summary")
    public ResponseEntity<ApiResponse<OperatorRatingSummaryResponse>> getMyRatingSummary(
            @AuthenticationPrincipal OperatorPrincipal principal) {

        if (principal == null) {
            throw new UnauthorizedException("Full authentication is required to access this resource");
        }
        log.info("REST request: Authenticated operator {} fetching rating summary", principal.getId());
        OperatorRatingSummaryResponse response = reviewService.getOperatorRatingSummary(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Your rating summary retrieved successfully", response));
    }

    /**
     * Retrieves paginated feedback reviews for the currently authenticated operator.
     */
    @GetMapping("/me/reviews")
    public ResponseEntity<ApiResponse<Page<OperatorReviewResponse>>> getMyReviews(
            @AuthenticationPrincipal OperatorPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (principal == null) {
            throw new UnauthorizedException("Full authentication is required to access this resource");
        }
        log.info("REST request: Authenticated operator {} fetching reviews ledger", principal.getId());
        int validPage = Math.max(0, page);
        int validSize = (size <= 0 || size > 50) ? 10 : size;
        Pageable pageable = PageRequest.of(validPage, validSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<OperatorReviewResponse> response = reviewService.getOperatorReviews(principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success("Your customer reviews retrieved successfully", response));
    }

    /**
     * Retrieves the specific review for an assignment.
     */
    @GetMapping("/jobs/{assignmentId}/review")
    public ResponseEntity<ApiResponse<OperatorReviewResponse>> getAssignmentReview(
            @PathVariable Long assignmentId) {

        log.info("REST request: Fetching review for assignment ID: {}", assignmentId);
        OperatorReviewResponse response = reviewService.getAssignmentReview(assignmentId);
        return ResponseEntity.ok(ApiResponse.success("Assignment review retrieved successfully", response));
    }
}
