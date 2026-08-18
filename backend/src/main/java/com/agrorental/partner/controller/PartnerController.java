package com.agrorental.partner.controller;

import com.agrorental.common.dto.ApiResponse;
import com.agrorental.partner.dto.PartnerChangePasswordRequest;
import com.agrorental.partner.dto.PartnerDashboardResponse;
import com.agrorental.partner.dto.PartnerProfileResponse;
import com.agrorental.partner.dto.PartnerProfileUpdateRequest;
import com.agrorental.partner.dto.PartnerRegistrationRequest;
import com.agrorental.partner.entity.Partner;
import com.agrorental.partner.service.PartnerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/partners")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:5174", "http://localhost:5175", "http://localhost:5176"})
@RequiredArgsConstructor
public class PartnerController {

    private final PartnerService partnerService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<PartnerProfileResponse>> registerPartner(
            @Valid @RequestBody PartnerRegistrationRequest request) {
        log.info("Registering new partner with mobile: {}", request.getMobileNumber());
        Partner partner = partnerService.registerPartner(request);
        PartnerProfileResponse response = partnerService.toProfileResponse(partner);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Partner registered successfully", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PartnerProfileResponse>> getPartnerProfile(@PathVariable Long id) {
        log.info("Fetching partner profile for ID: {}", id);
        PartnerProfileResponse response = partnerService.getPartnerProfile(id);
        return ResponseEntity.ok(ApiResponse.success("Partner profile fetched successfully", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PartnerProfileResponse>> updatePartnerProfile(
            @PathVariable Long id,
            @Valid @RequestBody PartnerProfileUpdateRequest request) {
        log.info("Updating partner profile for ID: {}", id);
        PartnerProfileResponse response = partnerService.updatePartnerProfile(id, request);
        return ResponseEntity.ok(ApiResponse.success("Partner profile updated successfully", response));
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<ApiResponse<String>> changePartnerPassword(
            @PathVariable Long id,
            @Valid @RequestBody PartnerChangePasswordRequest request) {
        log.info("Updating password for partner ID: {}", id);
        partnerService.changePartnerPassword(id, request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", "Password has been updated."));
    }

    @GetMapping("/{id}/dashboard")
    public ResponseEntity<ApiResponse<PartnerDashboardResponse>> getPartnerDashboard(@PathVariable Long id) {
        log.info("Fetching partner dashboard metrics for ID: {}", id);
        return partnerService.getPartnerDashboard(id)
                .map(res -> ResponseEntity.ok(ApiResponse.success("Partner dashboard data fetched successfully", res)))
                .orElse(ResponseEntity.notFound().build());
    }
}
