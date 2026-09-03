package com.agrorental.farmer.controller;

import com.agrorental.common.dto.ApiResponse;
import com.agrorental.farmer.dto.FarmCreateRequest;
import com.agrorental.farmer.dto.FarmResponse;
import com.agrorental.farmer.service.FarmService;
import com.agrorental.security.principal.FarmerPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller providing APIs for Farmer Farm Management.
 */
@RestController
@RequestMapping("/api/farmers/farms")
@PreAuthorize("hasAnyRole('FARMER', 'ADMIN')")
@CrossOrigin(origins = "http://localhost:5173")
public class FarmController {

    private final FarmService farmService;

    public FarmController(FarmService farmService) {
        this.farmService = farmService;
    }

    /**
     * Creates a new farm for the authenticated farmer.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<FarmResponse>> createFarm(
            @AuthenticationPrincipal FarmerPrincipal principal,
            @Valid @RequestBody FarmCreateRequest request) {

        if (principal == null || principal.getId() == null) {
            throw new AccessDeniedException("Authentication is required to create a farm.");
        }
        request.setFarmerId(principal.getId());

        FarmResponse response = farmService.createFarm(principal.getId(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Farm created successfully", response));
    }

    /**
     * Lists all registered farms for the authenticated farmer.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<FarmResponse>>> getFarms(
            @AuthenticationPrincipal FarmerPrincipal principal,
            @RequestParam(required = false) Long farmerId) {

        Long targetFarmerId = (principal != null && principal.getId() != null) ? principal.getId() : farmerId;
        if (targetFarmerId == null) {
            throw new AccessDeniedException("Authentication is required to list farms.");
        }
        List<FarmResponse> response = farmService.getFarmsByFarmerId(targetFarmerId);
        return ResponseEntity.ok(ApiResponse.success("Farms retrieved successfully", response));
    }

    /**
     * Retrieves farm details by farm ID for the authenticated farmer.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FarmResponse>> getFarmById(
            @AuthenticationPrincipal FarmerPrincipal principal,
            @PathVariable Long id) {

        if (principal == null || principal.getId() == null) {
            throw new AccessDeniedException("Authentication is required to view farm details.");
        }
        FarmResponse response = farmService.getFarmByIdAndFarmerId(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Farm retrieved successfully", response));
    }

    /**
     * Updates an existing farm by farm ID for the authenticated farmer.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<FarmResponse>> updateFarm(
            @PathVariable Long id,
            @AuthenticationPrincipal FarmerPrincipal principal,
            @Valid @RequestBody FarmCreateRequest request) {

        if (principal == null || principal.getId() == null) {
            throw new AccessDeniedException("Authentication is required to update a farm.");
        }
        request.setFarmerId(principal.getId());

        FarmResponse response = farmService.updateFarm(id, principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Farm updated successfully", response));
    }

    /**
     * Deletes a farm by farm ID for the authenticated farmer.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFarm(
            @AuthenticationPrincipal FarmerPrincipal principal,
            @PathVariable Long id) {

        if (principal == null || principal.getId() == null) {
            throw new AccessDeniedException("Authentication is required to delete a farm.");
        }
        farmService.deleteFarm(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Farm deleted successfully", null));
    }
}
