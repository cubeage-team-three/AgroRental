package com.agrorental.operator.controller;

import com.agrorental.common.dto.ApiResponse;
import com.agrorental.common.exception.UnauthorizedException;
import com.agrorental.operator.dto.OperatorLocationResponse;
import com.agrorental.operator.dto.OperatorLocationUpdateRequest;
import com.agrorental.operator.service.OperatorLocationService;
import com.agrorental.security.principal.OperatorPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller exposing endpoints for Operator GPS Location Tracking (Start, Update, Latest, Stop).
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
public class OperatorLocationController {

    private final OperatorLocationService locationService;

    private Long validatePrincipal(OperatorPrincipal principal) {
        if (principal == null) {
            log.warn("Unauthorized location modification attempt without principal");
            throw new UnauthorizedException("Full authentication is required to access this resource");
        }
        return principal.getId();
    }

    /**
     * Starts real-time GPS location tracking for an active job assignment.
     */
    @PatchMapping("/{assignmentId}/location/start")
    public ResponseEntity<ApiResponse<OperatorLocationResponse>> startTracking(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal OperatorPrincipal principal) {

        Long operatorId = validatePrincipal(principal);
        log.info("REST request: Operator {} starting GPS tracking for assignment {}", operatorId, assignmentId);

        OperatorLocationResponse response = locationService.startTracking(assignmentId, operatorId);
        return ResponseEntity.ok(ApiResponse.success("GPS location tracking started successfully", response));
    }

    /**
     * Updates GPS coordinates for an active job assignment.
     */
    @PatchMapping("/{assignmentId}/location")
    public ResponseEntity<ApiResponse<OperatorLocationResponse>> updateLocation(
            @PathVariable Long assignmentId,
            @Valid @RequestBody OperatorLocationUpdateRequest request,
            @AuthenticationPrincipal OperatorPrincipal principal) {

        Long operatorId = validatePrincipal(principal);
        log.info("REST request: Operator {} sending GPS update for assignment {}", operatorId, assignmentId);

        OperatorLocationResponse response = locationService.updateLocation(assignmentId, operatorId, request);
        return ResponseEntity.ok(ApiResponse.success("GPS location updated successfully", response));
    }

    /**
     * Retrieves the latest recorded GPS location for an active job assignment.
     */
    @GetMapping("/{assignmentId}/location")
    public ResponseEntity<ApiResponse<OperatorLocationResponse>> getLatestLocation(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal OperatorPrincipal principal) {

        Long operatorId = validatePrincipal(principal);
        log.info("REST request: Operator {} fetching latest GPS location for assignment {}", operatorId, assignmentId);

        OperatorLocationResponse response = locationService.getLatestLocation(assignmentId, operatorId);
        return ResponseEntity.ok(ApiResponse.success("Latest GPS location retrieved successfully", response));
    }

    /**
     * Stops GPS location tracking for an active job assignment.
     */
    @PatchMapping("/{assignmentId}/location/stop")
    public ResponseEntity<ApiResponse<OperatorLocationResponse>> stopTracking(
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal OperatorPrincipal principal) {

        Long operatorId = validatePrincipal(principal);
        log.info("REST request: Operator {} stopping GPS tracking for assignment {}", operatorId, assignmentId);

        OperatorLocationResponse response = locationService.stopTracking(assignmentId, operatorId);
        return ResponseEntity.ok(ApiResponse.success("GPS location tracking stopped successfully", response));
    }
}
