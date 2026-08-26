package com.agrorental.farmer.controller;

import com.agrorental.common.dto.ApiResponse;
import com.agrorental.farmer.dto.ChangePasswordRequest;
import com.agrorental.farmer.dto.FarmerProfileResponse;
import com.agrorental.farmer.dto.UpdateFarmerProfileRequest;
import com.agrorental.farmer.service.FarmerProfileService;
import com.agrorental.security.principal.FarmerPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/farmers")
@PreAuthorize("hasAnyRole('FARMER', 'ADMIN')")
@RequiredArgsConstructor
public class FarmerProfileController {

    private final FarmerProfileService farmerProfileService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<FarmerProfileResponse>> getProfile(@AuthenticationPrincipal FarmerPrincipal principal) {
        log.info("REST request to fetch own farmer profile. farmerId: {}", principal.getId());
        FarmerProfileResponse profile = farmerProfileService.getProfile(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Farmer profile retrieved successfully.", profile));
    }

    @GetMapping("/profile/{farmerId}")
    public ResponseEntity<ApiResponse<FarmerProfileResponse>> getProfileById(@PathVariable Long farmerId) {
        log.info("REST request to fetch farmer profile by path ID: {}", farmerId);
        FarmerProfileResponse profile = farmerProfileService.getProfile(farmerId);
        return ResponseEntity.ok(ApiResponse.success("Farmer profile retrieved successfully.", profile));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<FarmerProfileResponse>> updateProfile(@AuthenticationPrincipal FarmerPrincipal principal,
                                                                            @Valid @RequestBody UpdateFarmerProfileRequest request) {
        log.info("REST request to update own farmer profile for ID: {}", principal.getId());
        FarmerProfileResponse response = farmerProfileService.updateProfile(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Farmer profile updated successfully.", response));
    }

    @PutMapping("/profile/{farmerId}")
    public ResponseEntity<ApiResponse<FarmerProfileResponse>> updateProfileById(@PathVariable Long farmerId,
                                                                                @Valid @RequestBody UpdateFarmerProfileRequest request) {
        log.info("REST request to update farmer profile for ID: {}", farmerId);
        FarmerProfileResponse response = farmerProfileService.updateProfile(farmerId, request);
        return ResponseEntity.ok(ApiResponse.success("Farmer profile updated successfully.", response));
    }

    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(@AuthenticationPrincipal FarmerPrincipal principal,
                                                               @Valid @RequestBody ChangePasswordRequest request) {
        log.info("REST request to change password for own farmer ID: {}", principal.getId());
        farmerProfileService.changePassword(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully.", "Password updated."));
    }

    @PutMapping("/change-password/{farmerId}")
    public ResponseEntity<ApiResponse<String>> changePasswordById(@PathVariable Long farmerId,
                                                                   @Valid @RequestBody ChangePasswordRequest request) {
        log.info("REST request to change password for farmer ID: {}", farmerId);
        farmerProfileService.changePassword(farmerId, request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully.", "Password updated."));
    }
}
