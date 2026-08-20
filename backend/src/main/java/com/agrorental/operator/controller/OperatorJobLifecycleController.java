package com.agrorental.operator.controller;

import com.agrorental.common.dto.ApiResponse;
import com.agrorental.common.exception.UnauthorizedException;
import com.agrorental.operator.dto.OperatorAssignedJobResponse;
import com.agrorental.operator.dto.OperatorJobCompletionRequest;
import com.agrorental.operator.dto.OperatorJobPauseRequest;
import com.agrorental.operator.dto.OperatorJobRejectionRequest;
import com.agrorental.operator.service.OperatorJobLifecycleService;
import com.agrorental.security.principal.OperatorPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller managing the Operator Active Work Lifecycle endpoints (Accept, Reject, Travel, Work, Pause, Complete).
 */
@Slf4j
@RestController
@RequestMapping("/api/operators/jobs")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:5174",
        "http://localhost:5175",
        "http://localhost:5176"
})
@RequiredArgsConstructor
public class OperatorJobLifecycleController {

    private final OperatorJobLifecycleService lifecycleService;

    private Long validatePrincipal(OperatorPrincipal principal) {
        if (principal == null) {
            log.warn("Unauthorized lifecycle modification attempt without principal");
            throw new UnauthorizedException("Full authentication is required to access this resource");
        }
        return principal.getId();
    }

    /**
     * Accepts an assigned job (ASSIGNED -> ACCEPTED).
     */
    @PatchMapping("/{assignmentId}/accept")
    public ResponseEntity<ApiResponse<OperatorAssignedJobResponse>> acceptJob(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal OperatorPrincipal principal) {

        Long operatorId = validatePrincipal(principal);
        log.info("REST request: Operator {} accepting assignment {}", operatorId, assignmentId);

        OperatorAssignedJobResponse response = lifecycleService.acceptJob(assignmentId, operatorId);
        return ResponseEntity.ok(ApiResponse.success("Job assignment accepted successfully", response));
    }

    /**
     * Rejects an assigned job with mandatory reason (ASSIGNED -> REJECTED).
     */
    @PatchMapping("/{assignmentId}/reject")
    public ResponseEntity<ApiResponse<OperatorAssignedJobResponse>> rejectJob(
            @PathVariable Long assignmentId,
            @Valid @RequestBody OperatorJobRejectionRequest request,
            @AuthenticationPrincipal OperatorPrincipal principal) {

        Long operatorId = validatePrincipal(principal);
        log.info("REST request: Operator {} rejecting assignment {}", operatorId, assignmentId);

        OperatorAssignedJobResponse response = lifecycleService.rejectJob(assignmentId, operatorId, request);
        return ResponseEntity.ok(ApiResponse.success("Job assignment declined successfully", response));
    }

    /**
     * Starts traveling to the fieldwork location (ACCEPTED -> TRAVELING).
     */
    @PatchMapping("/{assignmentId}/start-travel")
    public ResponseEntity<ApiResponse<OperatorAssignedJobResponse>> startTravel(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal OperatorPrincipal principal) {

        Long operatorId = validatePrincipal(principal);
        log.info("REST request: Operator {} starting travel for assignment {}", operatorId, assignmentId);

        OperatorAssignedJobResponse response = lifecycleService.startTravel(assignmentId, operatorId);
        return ResponseEntity.ok(ApiResponse.success("En route to service location", response));
    }

    /**
     * Marks arrival at the fieldwork destination (TRAVELING -> REACHED).
     */
    @PatchMapping("/{assignmentId}/reached")
    public ResponseEntity<ApiResponse<OperatorAssignedJobResponse>> markReached(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal OperatorPrincipal principal) {

        Long operatorId = validatePrincipal(principal);
        log.info("REST request: Operator {} marking reached for assignment {}", operatorId, assignmentId);

        OperatorAssignedJobResponse response = lifecycleService.markReached(assignmentId, operatorId);
        return ResponseEntity.ok(ApiResponse.success("Arrival at farm location confirmed", response));
    }

    /**
     * Starts field work / machinery operations (REACHED -> IN_PROGRESS).
     */
    @PatchMapping("/{assignmentId}/start-work")
    public ResponseEntity<ApiResponse<OperatorAssignedJobResponse>> startWork(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal OperatorPrincipal principal) {

        Long operatorId = validatePrincipal(principal);
        log.info("REST request: Operator {} starting work for assignment {}", operatorId, assignmentId);

        OperatorAssignedJobResponse response = lifecycleService.startWork(assignmentId, operatorId);
        return ResponseEntity.ok(ApiResponse.success("Fieldwork operations started", response));
    }

    /**
     * Pauses field work with mandatory reason (IN_PROGRESS -> PAUSED).
     */
    @PatchMapping("/{assignmentId}/pause")
    public ResponseEntity<ApiResponse<OperatorAssignedJobResponse>> pauseWork(
            @PathVariable Long assignmentId,
            @Valid @RequestBody OperatorJobPauseRequest request,
            @AuthenticationPrincipal OperatorPrincipal principal) {

        Long operatorId = validatePrincipal(principal);
        log.info("REST request: Operator {} pausing work for assignment {}", operatorId, assignmentId);

        OperatorAssignedJobResponse response = lifecycleService.pauseWork(assignmentId, operatorId, request);
        return ResponseEntity.ok(ApiResponse.success("Fieldwork operations paused", response));
    }

    /**
     * Resumes field work (PAUSED -> IN_PROGRESS).
     */
    @PatchMapping("/{assignmentId}/resume")
    public ResponseEntity<ApiResponse<OperatorAssignedJobResponse>> resumeWork(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal OperatorPrincipal principal) {

        Long operatorId = validatePrincipal(principal);
        log.info("REST request: Operator {} resuming work for assignment {}", operatorId, assignmentId);

        OperatorAssignedJobResponse response = lifecycleService.resumeWork(assignmentId, operatorId);
        return ResponseEntity.ok(ApiResponse.success("Fieldwork operations resumed", response));
    }

    /**
     * Completes field work (IN_PROGRESS -> COMPLETED).
     */
    @PatchMapping("/{assignmentId}/complete")
    public ResponseEntity<ApiResponse<OperatorAssignedJobResponse>> completeWork(
            @PathVariable Long assignmentId,
            @Valid @RequestBody(required = false) OperatorJobCompletionRequest request,
            @AuthenticationPrincipal OperatorPrincipal principal) {

        Long operatorId = validatePrincipal(principal);
        log.info("REST request: Operator {} completing work for assignment {}", operatorId, assignmentId);

        OperatorAssignedJobResponse response = lifecycleService.completeWork(assignmentId, operatorId, request);
        return ResponseEntity.ok(ApiResponse.success("Fieldwork operations marked completed", response));
    }
}
