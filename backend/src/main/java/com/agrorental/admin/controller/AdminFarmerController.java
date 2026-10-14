package com.agrorental.admin.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.agrorental.common.dto.ApiResponse;
import com.agrorental.farmer.dto.FarmerResponse;
import com.agrorental.farmer.service.FarmerService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/farmers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminFarmerController {

    private final FarmerService farmerService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<FarmerResponse>>> getAllFarmers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            Pageable pageable) {

        Page<FarmerResponse> farmers = farmerService.getFarmers(search, status, pageable);

        return ResponseEntity.ok(
                new ApiResponse<>(true, "Farmers retrieved successfully", farmers)
        );
    }

    @PutMapping("/{farmerId}/approve-kyc")
    public ResponseEntity<ApiResponse<FarmerResponse>> approveKyc(@PathVariable Long farmerId) {
        try {
            FarmerResponse approvedFarmer = farmerService.approveFarmerKyc(farmerId);

            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Farmer KYC approved successfully", approvedFarmer)
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (IllegalStateException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @PutMapping("/{farmerId}/reject-kyc")
    public ResponseEntity<ApiResponse<FarmerResponse>> rejectKyc(@PathVariable Long farmerId) {
        try {
            FarmerResponse rejectedFarmer = farmerService.rejectFarmerKyc(farmerId);

            return ResponseEntity.ok(
                    new ApiResponse<>(true, "Farmer KYC rejected successfully", rejectedFarmer)
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        } catch (IllegalStateException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
}