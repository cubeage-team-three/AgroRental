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

    // ---------------------------------------------------------
    // CREATE EQUIPMENT
    // ---------------------------------------------------------

    @PostMapping
    public ResponseEntity<ApiResponse<EquipmentResponse>> createEquipment(
            @Valid @RequestBody EquipmentCreateRequest request) {

        EquipmentResponse response =
                equipmentService.createEquipment(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Equipment created successfully",
                        response
                ));
    }

    // ---------------------------------------------------------
    // GET EQUIPMENT BY ID
    // ---------------------------------------------------------

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EquipmentResponse>> getEquipmentById(
            @PathVariable Long id) {

        EquipmentResponse response =
                equipmentService.getEquipmentById(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Equipment retrieved successfully",
                        response
                )
        );
    }

    // ---------------------------------------------------------
    // GET PARTNER EQUIPMENT
    // ---------------------------------------------------------

    @GetMapping("/partner/{partnerId}")
    public ResponseEntity<ApiResponse<List<EquipmentSummaryResponse>>>
    getEquipmentByPartner(@PathVariable Long partnerId) {

        List<EquipmentSummaryResponse> response =
                equipmentService.getEquipmentByPartner(partnerId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Partner equipment retrieved successfully",
                        response
                )
        );
    }

    // ---------------------------------------------------------
    // GET AVAILABLE EQUIPMENT
    // ---------------------------------------------------------

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<EquipmentSummaryResponse>>>
    getDiscoverableEquipment() {

        List<EquipmentSummaryResponse> response =
                equipmentService.getDiscoverableEquipment();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Discoverable equipment retrieved successfully",
                        response
                )
        );
    }

    // ---------------------------------------------------------
    // GET AVAILABLE EQUIPMENT - PAGINATED
    // ---------------------------------------------------------

    @GetMapping("/available/page")
    public ResponseEntity<ApiResponse<Page<EquipmentSummaryResponse>>>
    getDiscoverableEquipmentPaginated(
            @PageableDefault(size = 20) Pageable pageable) {

        Page<EquipmentSummaryResponse> response =
                equipmentService.getDiscoverableEquipment(pageable);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Discoverable equipment retrieved successfully",
                        response
                )
        );
    }

    // ---------------------------------------------------------
    // SEARCH EQUIPMENT
    // ---------------------------------------------------------

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<EquipmentSummaryResponse>>>
    searchEquipment(@Valid EquipmentSearchRequest request) {

        List<EquipmentSummaryResponse> response =
                equipmentService.searchEquipment(request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Equipment search completed successfully",
                        response
                )
        );
    }

    // ---------------------------------------------------------
    // SEARCH EQUIPMENT - PAGINATED
    // ---------------------------------------------------------

    @GetMapping("/search/page")
    public ResponseEntity<ApiResponse<Page<EquipmentSummaryResponse>>>
    searchEquipmentPaginated(
            @Valid EquipmentSearchRequest request,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<EquipmentSummaryResponse> response =
                equipmentService.searchEquipment(request, pageable);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Equipment search completed successfully",
                        response
                )
        );
    }

    // ---------------------------------------------------------
    // UPDATE EQUIPMENT
    // ---------------------------------------------------------

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EquipmentResponse>> updateEquipment(
            @PathVariable Long id,
            @RequestHeader(
                    value = "X-Partner-Id",
                    required = false
            ) Long requestingPartnerId,
            @Valid @RequestBody EquipmentUpdateRequest request) {

        EquipmentResponse response;

        if (requestingPartnerId != null) {
            response = equipmentService.updateEquipment(
                    id,
                    requestingPartnerId,
                    request
            );
        } else {
            response = equipmentService.updateEquipment(
                    id,
                    request
            );
        }

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Equipment updated successfully",
                        response
                )
        );
    }

    // ---------------------------------------------------------
    // ENABLE EQUIPMENT
    // ---------------------------------------------------------

    @PatchMapping("/{id}/enable")
    public ResponseEntity<ApiResponse<EquipmentResponse>> enableEquipment(
            @PathVariable Long id,
            @RequestHeader(
                    value = "X-Partner-Id",
                    required = false
            ) Long requestingPartnerId) {

        EquipmentResponse response;

        if (requestingPartnerId != null) {
            response = equipmentService.enableEquipment(
                    id,
                    requestingPartnerId
            );
        } else {
            response = equipmentService.enableEquipment(id);
        }

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Equipment enabled successfully",
                        response
                )
        );
    }

    // ---------------------------------------------------------
    // DISABLE EQUIPMENT
    // ---------------------------------------------------------

    @PatchMapping("/{id}/disable")
    public ResponseEntity<ApiResponse<EquipmentResponse>> disableEquipment(
            @PathVariable Long id,
            @RequestHeader(
                    value = "X-Partner-Id",
                    required = false
            ) Long requestingPartnerId) {

        EquipmentResponse response;

        if (requestingPartnerId != null) {
            response = equipmentService.disableEquipment(
                    id,
                    requestingPartnerId
            );
        } else {
            response = equipmentService.disableEquipment(id);
        }

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Equipment disabled successfully",
                        response
                )
        );
    }

    // ---------------------------------------------------------
    // DELETE EQUIPMENT
    // ---------------------------------------------------------

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEquipment(
            @PathVariable Long id,
            @RequestHeader(
                    value = "X-Partner-Id",
                    required = false
            ) Long requestingPartnerId) {

        if (requestingPartnerId != null) {
            equipmentService.deleteEquipment(
                    id,
                    requestingPartnerId
            );
        } else {
            equipmentService.deleteEquipment(id);
        }

        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------
    // DELETE EQUIPMENT IMAGE
    // ---------------------------------------------------------

    @DeleteMapping("/{equipmentId}/images/{imageId}")
    public ResponseEntity<ApiResponse<EquipmentResponse>>
    deleteEquipmentImage(
            @PathVariable Long equipmentId,
            @PathVariable Long imageId,
            @RequestHeader(
                    value = "X-Partner-Id",
                    required = false
            ) Long requestingPartnerId) {

        EquipmentResponse response =
                equipmentService.deleteEquipmentImage(
                        equipmentId,
                        imageId,
                        requestingPartnerId
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Equipment image deleted successfully",
                        response
                )
        );
    }
}