package com.agrorental.operator.controller;

import com.agrorental.common.dto.ApiResponse;
import com.agrorental.operator.dto.OperatorDetailResponse;
import com.agrorental.operator.dto.OperatorSummaryResponse;
import com.agrorental.operator.dto.OperatorVerificationRequest;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.service.AdminOperatorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller exposing Administrator endpoints for Operator review, verification, and filtering.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/operators")
@RequiredArgsConstructor
public class AdminOperatorController {

    private final AdminOperatorService adminOperatorService;

    /**
     * Lists operators with optional status and verification filtering and pagination.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<OperatorSummaryResponse>>> getOperators(
            @RequestParam(required = false) OperatorStatus status,
            @RequestParam(required = false) Boolean mobileVerified,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Admin listing operators with status={}, mobileVerified={}, page={}", status, mobileVerified, pageable.getPageNumber());
        Page<OperatorSummaryResponse> page = adminOperatorService.getOperators(status, mobileVerified, pageable);
        return ResponseEntity.ok(ApiResponse.success("Operators retrieved successfully", page));
    }

    /**
     * Retrieves full operator verification profile and KYC documents.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OperatorDetailResponse>> getOperatorDetail(
            @PathVariable Long id) {
        log.info("Admin fetching operator verification details for ID: {}", id);
        OperatorDetailResponse response = adminOperatorService.getOperatorDetail(id);
        return ResponseEntity.ok(ApiResponse.success("Operator details retrieved successfully", response));
    }

    /**
     * Processes Admin verification (approval or rejection) for an operator.
     */
    @PatchMapping("/{id}/verify")
    public ResponseEntity<ApiResponse<OperatorDetailResponse>> verifyOperator(
            @PathVariable Long id,
            @Valid @RequestBody OperatorVerificationRequest request) {
        log.info("Admin verifying operator ID: {} with target status: {}", id, request.getStatus());
        OperatorDetailResponse response = adminOperatorService.verifyOperator(id, request);
        return ResponseEntity.ok(ApiResponse.success("Operator verification updated successfully", response));
    }
}
