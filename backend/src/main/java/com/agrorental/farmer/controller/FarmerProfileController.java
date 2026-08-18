package com.agrorental.farmer.controller;

import com.agrorental.common.dto.ApiResponse;
import com.agrorental.farmer.dto.ChangePasswordRequest;
import com.agrorental.farmer.dto.FarmerProfileResponse;
import com.agrorental.farmer.dto.UpdateFarmerProfileRequest;
import com.agrorental.farmer.service.FarmerProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/farmers")
@RequiredArgsConstructor
public class FarmerProfileController {

    private final FarmerProfileService farmerProfileService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<FarmerProfileResponse>> getProfile(@RequestParam(required = false) Long farmerId,
                                                                         @RequestParam(required = false) String mobileNumber) {
        log.info("REST request to fetch farmer profile. farmerId: {}, mobileNumber: {}", farmerId, mobileNumber);
        FarmerProfileResponse profile;
        if (farmerId != null) {
            profile = farmerProfileService.getProfile(farmerId);
        } else if (mobileNumber != null && !mobileNumber.trim().isEmpty()) {
            profile = farmerProfileService.getProfileByMobile(mobileNumber.trim());
        } else {
            // Default fallback for demo / dev when query param is not passed
            profile = farmerProfileService.getProfile(1L);
        }
        return ResponseEntity.ok(ApiResponse.success("Farmer profile retrieved successfully.", profile));
    }

    @GetMapping("/profile/{farmerId}")
    public ResponseEntity<ApiResponse<FarmerProfileResponse>> getProfileById(@PathVariable Long farmerId) {
        log.info("REST request to fetch farmer profile by path ID: {}", farmerId);
        FarmerProfileResponse profile = farmerProfileService.getProfile(farmerId);
        return ResponseEntity.ok(ApiResponse.success("Farmer profile retrieved successfully.", profile));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<FarmerProfileResponse>> updateProfile(@RequestParam(required = false, defaultValue = "1") Long farmerId,
                                                                            @Valid @RequestBody UpdateFarmerProfileRequest request) {
        log.info("REST request to update farmer profile for ID: {}", farmerId);
        FarmerProfileResponse response = farmerProfileService.updateProfile(farmerId, request);
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
    public ResponseEntity<ApiResponse<String>> changePassword(@RequestParam(required = false, defaultValue = "1") Long farmerId,
                                                               @Valid @RequestBody ChangePasswordRequest request) {
        log.info("REST request to change password for farmer ID: {}", farmerId);
        farmerProfileService.changePassword(farmerId, request);
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
