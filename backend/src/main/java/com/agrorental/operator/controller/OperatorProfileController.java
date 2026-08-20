package com.agrorental.operator.controller;

import com.agrorental.common.dto.ApiResponse;
import com.agrorental.common.exception.UnauthorizedException;
import com.agrorental.operator.dto.OperatorChangePasswordRequest;
import com.agrorental.operator.dto.OperatorProfileResponse;
import com.agrorental.operator.dto.OperatorProfileUpdateRequest;
import com.agrorental.operator.service.OperatorProfileService;
import com.agrorental.security.principal.OperatorPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for authenticated Operator self-service Profile Management.
 * All operations derive the operator identity directly from the authenticated JWT OperatorPrincipal.
 */
@Slf4j
@RestController
@RequestMapping("/api/operators/profile")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:5174",
        "http://localhost:5175",
        "http://localhost:5176"
})
@RequiredArgsConstructor
public class OperatorProfileController {

    private final OperatorProfileService operatorProfileService;

    /**
     * Retrieves the profile details of the authenticated operator.
     *
     * @param principal Authenticated operator principal from JWT
     * @return ApiResponse containing OperatorProfileResponse
     */
    @GetMapping
    public ResponseEntity<ApiResponse<OperatorProfileResponse>> getProfile(
            @AuthenticationPrincipal OperatorPrincipal principal) {

        if (principal == null) {
            log.warn("Unauthorized /api/operators/profile GET attempt without principal");
            throw new UnauthorizedException("Full authentication is required to access this resource");
        }

        log.info("REST request to fetch profile for operator ID: {}", principal.getId());
        OperatorProfileResponse profile = operatorProfileService.getCurrentProfile(principal.getId());

        return ResponseEntity.ok(
                ApiResponse.success("Operator profile retrieved successfully", profile)
        );
    }

    /**
     * Updates the permitted editable profile fields of the authenticated operator.
     *
     * @param principal Authenticated operator principal from JWT
     * @param request Validated profile update request
     * @return ApiResponse containing updated OperatorProfileResponse
     */
    @PutMapping
    public ResponseEntity<ApiResponse<OperatorProfileResponse>> updateProfile(
            @AuthenticationPrincipal OperatorPrincipal principal,
            @Valid @RequestBody OperatorProfileUpdateRequest request) {

        if (principal == null) {
            log.warn("Unauthorized /api/operators/profile PUT attempt without principal");
            throw new UnauthorizedException("Full authentication is required to access this resource");
        }

        log.info("REST request to update profile for operator ID: {}", principal.getId());
        OperatorProfileResponse updatedProfile = operatorProfileService.updateCurrentProfile(principal.getId(), request);

        return ResponseEntity.ok(
                ApiResponse.success("Operator profile updated successfully", updatedProfile)
        );
    }

    /**
     * Changes the password of the authenticated operator.
     *
     * @param principal Authenticated operator principal from JWT
     * @param request Validated password change request
     * @return ApiResponse confirming successful password update
     */
    @PatchMapping("/password")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @AuthenticationPrincipal OperatorPrincipal principal,
            @Valid @RequestBody OperatorChangePasswordRequest request) {

        if (principal == null) {
            log.warn("Unauthorized /api/operators/profile/password PATCH attempt without principal");
            throw new UnauthorizedException("Full authentication is required to access this resource");
        }

        log.info("REST request to change password for operator ID: {}", principal.getId());
        operatorProfileService.changePassword(principal.getId(), request);

        return ResponseEntity.ok(
                ApiResponse.success("Password changed successfully", "Your password has been updated securely.")
        );
    }
}
