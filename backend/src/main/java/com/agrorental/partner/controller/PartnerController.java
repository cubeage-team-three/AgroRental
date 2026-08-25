package com.agrorental.partner.controller;

import com.agrorental.common.dto.ApiResponse;
import com.agrorental.partner.dto.PartnerChangePasswordRequest;
import com.agrorental.partner.dto.PartnerDashboardResponse;
import com.agrorental.partner.dto.PartnerOtpRequest;
import com.agrorental.partner.dto.PartnerProfileResponse;
import com.agrorental.partner.dto.PartnerProfileUpdateRequest;
import com.agrorental.partner.dto.PartnerRegistrationRequest;
import com.agrorental.partner.entity.Partner;
import com.agrorental.partner.service.PartnerService;

import java.util.List;

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
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:5174",
        "http://localhost:5175",
        "http://localhost:5176"
})
@RequiredArgsConstructor
public class PartnerController {

    private final PartnerService partnerService;

    // =========================
    // PARTNER REGISTRATION
    // =========================

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<PartnerProfileResponse>> registerPartner(
            @Valid @RequestBody PartnerRegistrationRequest request) {

        log.info("Registering new partner with mobile: {}", request.getMobileNumber());

        Partner partner = partnerService.registerPartner(request);

        PartnerProfileResponse response =
                partnerService.toProfileResponse(partner);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Partner registered successfully",
                        response
                ));
    }

    // =========================
    // GET ALL PARTNERS (ADMIN / KYC)
    // =========================

    @GetMapping
    public ResponseEntity<ApiResponse<List<PartnerProfileResponse>>> getAllPartners() {
        log.info("Fetching all registered partners");

        List<PartnerProfileResponse> responses = partnerService.getAllPartners();

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Partners fetched successfully",
                        responses
                )
        );
    }

    // =========================
    // GET PARTNER PROFILE
    // =========================

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PartnerProfileResponse>> getPartnerProfile(
            @PathVariable Long id) {

        log.info("Fetching partner profile for ID: {}", id);

        PartnerProfileResponse response =
                partnerService.getPartnerProfile(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Partner profile fetched successfully",
                        response
                )
        );
    }

    // =========================
    // UPDATE PARTNER PROFILE
    // =========================

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PartnerProfileResponse>> updatePartnerProfile(
            @PathVariable Long id,
            @Valid @RequestBody PartnerProfileUpdateRequest request) {

        log.info("Updating partner profile for ID: {}", id);

        PartnerProfileResponse response =
                partnerService.updatePartnerProfile(id, request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Partner profile updated successfully",
                        response
                )
        );
    }

    // =========================
    // CHANGE PASSWORD
    // =========================

    @PutMapping("/{id}/password")
    public ResponseEntity<ApiResponse<String>> changePartnerPassword(
            @PathVariable Long id,
            @Valid @RequestBody PartnerChangePasswordRequest request) {

        log.info("Updating password for partner ID: {}", id);

        partnerService.changePartnerPassword(id, request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Password changed successfully",
                        "Password has been updated."
                )
        );
    }

    // =========================
    // PARTNER DASHBOARD
    // =========================

    @GetMapping("/{id}/dashboard")
    public ResponseEntity<ApiResponse<PartnerDashboardResponse>> getPartnerDashboard(
            @PathVariable Long id) {

        log.info("Fetching partner dashboard metrics for ID: {}", id);

        return partnerService.getPartnerDashboard(id)
                .map(res -> ResponseEntity.ok(
                        ApiResponse.success(
                                "Partner dashboard data fetched successfully",
                                res
                        )
                ))
                .orElse(ResponseEntity.notFound().build());
    }

    // =========================
    // SEND OTP
    // =========================

    @PostMapping("/{id}/otp/send")
    public ResponseEntity<ApiResponse<String>> sendOtp(
            @PathVariable Long id) {

        log.info("Sending OTP for partner ID: {}", id);

        String otp = partnerService.sendOtp(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "OTP sent successfully",
                        otp
                )
        );
    }

    // =========================
    // VERIFY OTP
    // =========================

    @PostMapping("/{id}/otp/verify")
    public ResponseEntity<ApiResponse<PartnerProfileResponse>> verifyOtp(
            @PathVariable Long id,
            @Valid @RequestBody PartnerOtpRequest request) {

        log.info("Verifying OTP for partner ID: {}", id);

        PartnerProfileResponse response =
                partnerService.verifyOtp(id, request.getOtp());

        return ResponseEntity.ok(
                ApiResponse.success(
                        "OTP verified successfully",
                        response
                )
        );
    }

    // =========================
    // RESEND OTP
    // =========================

    @PostMapping("/{id}/otp/resend")
    public ResponseEntity<ApiResponse<String>> resendOtp(
            @PathVariable Long id) {

        log.info("Resending OTP for partner ID: {}", id);

        String otp = partnerService.resendOtp(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "OTP resent successfully",
                        otp
                )
        );
    }

    // =========================
    // KYC APPROVE
    // =========================

    @PutMapping("/{id}/kyc/approve")
    public ResponseEntity<ApiResponse<PartnerProfileResponse>> approvePartnerKyc(
            @PathVariable Long id) {

        log.info("Approving KYC for partner ID: {}", id);

        PartnerProfileResponse response =
                partnerService.approvePartnerKyc(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Partner KYC approved successfully",
                        response
                )
        );
    }

    // =========================
    // KYC REJECT
    // =========================

    @PutMapping("/{id}/kyc/reject")
    public ResponseEntity<ApiResponse<PartnerProfileResponse>> rejectPartnerKyc(
            @PathVariable Long id) {

        log.info("Rejecting KYC for partner ID: {}", id);

        PartnerProfileResponse response =
                partnerService.rejectPartnerKyc(id);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Partner KYC rejected successfully",
                        response
                )
        );
    }
}
