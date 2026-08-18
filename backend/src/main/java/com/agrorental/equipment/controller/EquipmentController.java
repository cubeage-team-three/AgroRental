package com.agrorental.equipment.controller;

import com.agrorental.common.dto.ApiResponse;
import com.agrorental.equipment.dto.EquipmentCreateRequest;
import com.agrorental.equipment.dto.EquipmentResponse;
import com.agrorental.equipment.dto.EquipmentSearchRequest;
import com.agrorental.equipment.dto.EquipmentSummaryResponse;
import com.agrorental.equipment.dto.EquipmentUpdateRequest;
import com.agrorental.equipment.service.EquipmentService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller exposing HTTP APIs for Machine Management (Equipment) module.
 * Provides endpoints for creating, retrieving, updating, enabling, disabling, and deleting machinery listings.
 */
@RestController
@RequestMapping("/api/equipment")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:5174",
        "http://localhost:5175",
        "http://localhost:5176"
})
public class EquipmentController {

    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    /**
     * Creates a new equipment listing owned by a partner.
     *
     * @param request Validated equipment creation payload
     * @return ResponseEntity containing HTTP 201 Created and ApiResponse wrapper with EquipmentResponse
     */
    @PostMapping
    public ResponseEntity<ApiResponse<EquipmentResponse>> createEquipment(
            @Valid @RequestBody EquipmentCreateRequest request) {

        EquipmentResponse response = equipmentService.createEquipment(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Equipment created successfully", response));
    }

    /**
     * Retrieves an equipment listing by its unique identifier.
     *
     * @param id Equipment primary key
     * @return ResponseEntity containing HTTP 200 OK and EquipmentResponse
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EquipmentResponse>> getEquipmentById(
            @PathVariable Long id) {

        EquipmentResponse response = equipmentService.getEquipmentById(id);

        return ResponseEntity.ok(
                ApiResponse.success("Equipment retrieved successfully", response));
    }

    /**
     * Retrieves all equipment listings owned by a specific partner.
     *
     * @param partnerId Owning partner ID
     * @return ResponseEntity containing HTTP 200 OK and List of EquipmentSummaryResponse
     */
    @GetMapping("/partner/{partnerId}")
    public ResponseEntity<ApiResponse<List<EquipmentSummaryResponse>>> getEquipmentByPartner(
            @PathVariable Long partnerId) {

        List<EquipmentSummaryResponse> response = equipmentService.getEquipmentByPartner(partnerId);

        return ResponseEntity.ok(
                ApiResponse.success("Partner equipment retrieved successfully", response));
    }

    /**
     * Retrieves currently discoverable equipment listings (AVAILABLE and non-disabled).
     *
     * @return ResponseEntity containing HTTP 200 OK and List of EquipmentSummaryResponse
     */
    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<EquipmentSummaryResponse>>> getDiscoverableEquipment() {

        List<EquipmentSummaryResponse> response = equipmentService.getDiscoverableEquipment();

        return ResponseEntity.ok(
                ApiResponse.success("Discoverable equipment retrieved successfully", response));
    }

    /**
     * Retrieves discoverable equipment listings with database-side pagination.
     *
     * @param pageable Spring Data Pageable pagination request
     * @return ResponseEntity containing HTTP 200 OK and Page of EquipmentSummaryResponse
     */
    @GetMapping("/available/page")
    public ResponseEntity<ApiResponse<Page<EquipmentSummaryResponse>>> getDiscoverableEquipmentPaginated(
            @PageableDefault(size = 20) Pageable pageable) {

        Page<EquipmentSummaryResponse> response = equipmentService.getDiscoverableEquipment(pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Discoverable equipment retrieved successfully", response));
    }

    /**
     * Dynamically searches for equipment matching multi-criteria filter parameters.
     *
     * @param request Search filter parameters (category, minPrice, maxPrice, availabilityStatus, locationAddress)
     * @return ResponseEntity containing HTTP 200 OK and List of EquipmentSummaryResponse
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<EquipmentSummaryResponse>>> searchEquipment(
            @Valid EquipmentSearchRequest request) {

        List<EquipmentSummaryResponse> response = equipmentService.searchEquipment(request);

        return ResponseEntity.ok(
                ApiResponse.success("Equipment search completed successfully", response));
    }

    /**
     * Dynamically searches for equipment matching multi-criteria filter parameters with database-side pagination.
     *
     * @param request Search filter parameters (category, minPrice, maxPrice, availabilityStatus, locationAddress)
     * @param pageable Spring Data Pageable pagination request
     * @return ResponseEntity containing HTTP 200 OK and Page of EquipmentSummaryResponse
     */
    @GetMapping("/search/page")
    public ResponseEntity<ApiResponse<Page<EquipmentSummaryResponse>>> searchEquipmentPaginated(
            @Valid EquipmentSearchRequest request,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<EquipmentSummaryResponse> response = equipmentService.searchEquipment(request, pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Equipment search completed successfully", response));
    }

    /**
     * Updates mutable fields of an existing equipment listing.
     *
     * @param id Equipment primary key
     * @param requestingPartnerId Optional X-Partner-Id header for partner ownership authorization
     * @param request Validated equipment update payload
     * @return ResponseEntity containing HTTP 200 OK and updated EquipmentResponse
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EquipmentResponse>> updateEquipment(
            @PathVariable Long id,
            @RequestHeader(value = "X-Partner-Id", required = false) Long requestingPartnerId,
            @Valid @RequestBody EquipmentUpdateRequest request) {

        EquipmentResponse response = (requestingPartnerId != null)
                ? equipmentService.updateEquipment(id, requestingPartnerId, request)
                : equipmentService.updateEquipment(id, request);

        return ResponseEntity.ok(
                ApiResponse.success("Equipment updated successfully", response));
    }

    /**
     * Enables a previously disabled equipment listing.
     *
     * @param id Equipment primary key
     * @param requestingPartnerId Optional X-Partner-Id header for partner ownership authorization
     * @return ResponseEntity containing HTTP 200 OK and updated EquipmentResponse
     */
    @PatchMapping("/{id}/enable")
    public ResponseEntity<ApiResponse<EquipmentResponse>> enableEquipment(
            @PathVariable Long id,
            @RequestHeader(value = "X-Partner-Id", required = false) Long requestingPartnerId) {

        EquipmentResponse response = (requestingPartnerId != null)
                ? equipmentService.enableEquipment(id, requestingPartnerId)
                : equipmentService.enableEquipment(id);

        return ResponseEntity.ok(
                ApiResponse.success("Equipment enabled successfully", response));
    }

    /**
     * Disables an equipment listing for maintenance or administrative lockout.
     *
     * @param id Equipment primary key
     * @param requestingPartnerId Optional X-Partner-Id header for partner ownership authorization
     * @return ResponseEntity containing HTTP 200 OK and updated EquipmentResponse
     */
    @PatchMapping("/{id}/disable")
    public ResponseEntity<ApiResponse<EquipmentResponse>> disableEquipment(
            @PathVariable Long id,
            @RequestHeader(value = "X-Partner-Id", required = false) Long requestingPartnerId) {

        EquipmentResponse response = (requestingPartnerId != null)
                ? equipmentService.disableEquipment(id, requestingPartnerId)
                : equipmentService.disableEquipment(id);

        return ResponseEntity.ok(
                ApiResponse.success("Equipment disabled successfully", response));
    }

    /**
     * Removes an equipment listing from the system.
     *
     * @param id Equipment primary key
     * @param requestingPartnerId Optional X-Partner-Id header for partner ownership authorization
     * @return ResponseEntity containing HTTP 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEquipment(
            @PathVariable Long id,
            @RequestHeader(value = "X-Partner-Id", required = false) Long requestingPartnerId) {

        if (requestingPartnerId != null) {
            equipmentService.deleteEquipment(id, requestingPartnerId);
        } else {
            equipmentService.deleteEquipment(id);
        }

        return ResponseEntity.noContent().build();
    }

    /**
     * Removes a specific image asset from an equipment listing.
     *
     * @param equipmentId Equipment primary key
     * @param imageId Image asset primary key
     * @param requestingPartnerId Optional X-Partner-Id header for partner ownership authorization
     * @return ResponseEntity containing HTTP 200 OK and updated EquipmentResponse
     */
    @DeleteMapping("/{equipmentId}/images/{imageId}")
    public ResponseEntity<ApiResponse<EquipmentResponse>> deleteEquipmentImage(
            @PathVariable Long equipmentId,
            @PathVariable Long imageId,
            @RequestHeader(value = "X-Partner-Id", required = false) Long requestingPartnerId) {

        EquipmentResponse response = equipmentService.deleteEquipmentImage(equipmentId, imageId, requestingPartnerId);

        return ResponseEntity.ok(
                ApiResponse.success("Equipment image deleted successfully", response));
    }
}
