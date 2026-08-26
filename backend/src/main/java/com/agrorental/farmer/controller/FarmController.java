package com.agrorental.farmer.controller;

import com.agrorental.common.dto.ApiResponse;
import com.agrorental.farmer.dto.FarmCreateRequest;
import com.agrorental.farmer.dto.FarmResponse;
import com.agrorental.farmer.service.FarmService;
import com.agrorental.security.principal.FarmerPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller providing APIs for Farmer Farm Management (Module 6).
 */
@RestController
@RequestMapping("/api/farmers/farms")
@CrossOrigin(origins = "http://localhost:5173")
public class FarmController {

    private final FarmService farmService;

    public FarmController(FarmService farmService) {
        this.farmService = farmService;
    }

    /**
     * Creates a new farm for the farmer.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<FarmResponse>> createFarm(
            @AuthenticationPrincipal FarmerPrincipal principal,
            @Valid @RequestBody FarmCreateRequest request) {

        if (principal != null && principal.getId() != null) {
            request.setFarmerId(principal.getId());
        }

        FarmResponse response = farmService.createFarm(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Farm created successfully", response));
    }

    /**
     * Lists all registered farms for a farmer.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<FarmResponse>>> getFarms(
            @AuthenticationPrincipal FarmerPrincipal principal,
            @RequestParam(required = false) Long farmerId) {

        Long targetFarmerId = (principal != null && principal.getId() != null) ? principal.getId() : farmerId;
        List<FarmResponse> response = farmService.getFarmsByFarmerId(targetFarmerId);
        return ResponseEntity.ok(ApiResponse.success("Farms retrieved successfully", response));
    }

    /**
     * Retrieves farm details by farm ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FarmResponse>> getFarmById(@PathVariable Long id) {
        FarmResponse response = farmService.getFarmById(id);
        return ResponseEntity.ok(ApiResponse.success("Farm retrieved successfully", response));
    }

    /**
     * Updates an existing farm by farm ID.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FarmResponse>> updateFarm(
            @PathVariable Long id,
            @AuthenticationPrincipal FarmerPrincipal principal,
            @Valid @RequestBody FarmCreateRequest request) {

        if (principal != null && principal.getId() != null) {
            request.setFarmerId(principal.getId());
        }

        FarmResponse response = farmService.updateFarm(id, request);
        return ResponseEntity.ok(ApiResponse.success("Farm updated successfully", response));
    }

    /**
     * Deletes a farm by farm ID.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFarm(@PathVariable Long id) {
        farmService.deleteFarm(id);
        return ResponseEntity.ok(ApiResponse.success("Farm deleted successfully", null));
    }
}
