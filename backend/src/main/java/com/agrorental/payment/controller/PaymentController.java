package com.agrorental.payment.controller;

import com.agrorental.common.dto.ApiResponse;
import com.agrorental.payment.dto.*;
import com.agrorental.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller exposing endpoints for rental payment processing, financial reports, and tax invoices.
 */
@RestController
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // --- Farmer Payment & Invoice Endpoints ---

    @PostMapping("/api/farmers/payments")
    public ResponseEntity<ApiResponse<PaymentResponse>> processFarmerPayment(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.agrorental.security.principal.FarmerPrincipal principal,
            @Valid @RequestBody PaymentRequest request) {

        if (principal != null) {
            request.setFarmerId(principal.getId());
        }
        PaymentResponse response = paymentService.processPayment(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment processed successfully", response));
    }

    @GetMapping("/api/farmers/payments/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getFarmerPaymentById(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.agrorental.security.principal.FarmerPrincipal principal,
            @PathVariable Long id) {
        PaymentResponse response = paymentService.getPaymentById(id);
        if (principal != null && response != null && response.getFarmerId() != null && !response.getFarmerId().equals(principal.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Access is denied. You are not authorized to view another farmer's payment details.");
        }
        return ResponseEntity.ok(ApiResponse.success("Payment details retrieved successfully", response));
    }

    @GetMapping("/api/farmers/payments/farmer/{farmerId}")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getFarmerPayments(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.agrorental.security.principal.FarmerPrincipal principal,
            @PathVariable Long farmerId) {
        validateFarmerOwnershipOrAdmin(farmerId, principal);
        List<PaymentResponse> response = paymentService.getFarmerPayments(farmerId);
        return ResponseEntity.ok(ApiResponse.success("Farmer payments retrieved successfully", response));
    }

    @GetMapping("/api/farmers/bookings/{bookingId}/invoice")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoice(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.agrorental.security.principal.FarmerPrincipal principal,
            @PathVariable Long bookingId) {
        InvoiceResponse response = paymentService.generateInvoice(bookingId);
        validateFarmerOwnershipOrAdmin(response.getFarmerId(), principal);
        return ResponseEntity.ok(ApiResponse.success("Invoice generated successfully", response));
    }

    // --- Partner & General Payment Endpoints ---

    @PostMapping("/api/payments")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.agrorental.security.principal.FarmerPrincipal principal,
            @Valid @RequestBody PaymentCreateRequest request) {
        if (principal != null) {
            request.setFarmerId(principal.getId());
        }
        PaymentResponse response = paymentService.createPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Payment created successfully", response));
    }

    @GetMapping("/api/payments/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.agrorental.security.principal.FarmerPrincipal principal,
            @PathVariable Long id) {
        PaymentResponse response = paymentService.getPaymentById(id);
        if (response != null) {
            validateFarmerOwnershipOrAdmin(response.getFarmerId(), principal);
        }
        return ResponseEntity.ok(ApiResponse.success("Payment retrieved successfully", response));
    }

    @GetMapping("/api/payments/booking/{bookingId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByBookingId(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.agrorental.security.principal.FarmerPrincipal principal,
            @PathVariable Long bookingId) {
        PaymentResponse response = paymentService.getPaymentByBookingId(bookingId);
        if (response != null) {
            validateFarmerOwnershipOrAdmin(response.getFarmerId(), principal);
        }
        return ResponseEntity.ok(ApiResponse.success("Payment retrieved successfully", response));
    }

    @GetMapping("/api/payments/farmer/{farmerId}")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPaymentsByFarmer(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.agrorental.security.principal.FarmerPrincipal principal,
            @PathVariable Long farmerId) {
        validateFarmerOwnershipOrAdmin(farmerId, principal);
        return ResponseEntity.ok(ApiResponse.success("Farmer payments retrieved successfully", paymentService.getPaymentsByFarmer(farmerId)));
    }

    private void validateFarmerOwnershipOrAdmin(Long targetFarmerId, com.agrorental.security.principal.FarmerPrincipal principal) {
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (isAdmin) {
                return;
            }
        }
        if (principal != null && !principal.getId().equals(targetFarmerId)) {
            throw new org.springframework.security.access.AccessDeniedException("Access is denied. You are not authorized to view another farmer's payment history.");
        }
    }

    private void validatePartnerOwnershipOrAdmin(Long targetPartnerId, com.agrorental.security.principal.PartnerPrincipal principal) {
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (isAdmin) {
                return;
            }
        }
        if (principal == null || !principal.getId().equals(targetPartnerId)) {
            throw new org.springframework.security.access.AccessDeniedException("Access is denied. You are not authorized to view another partner's details.");
        }
    }

    @GetMapping("/api/payments/partner/{partnerId}")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPaymentsByPartner(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.agrorental.security.principal.PartnerPrincipal principal,
            @PathVariable Long partnerId) {
        validatePartnerOwnershipOrAdmin(partnerId, principal);
        return ResponseEntity.ok(ApiResponse.success("Partner payments retrieved successfully", paymentService.getPaymentsByPartner(partnerId)));
    }

    @GetMapping("/api/payments/partner/{partnerId}/earnings")
    public ResponseEntity<ApiResponse<PartnerEarningsSummary>> getPartnerEarningsSummary(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.agrorental.security.principal.PartnerPrincipal principal,
            @PathVariable Long partnerId) {
        validatePartnerOwnershipOrAdmin(partnerId, principal);
        return ResponseEntity.ok(ApiResponse.success("Partner earnings summary retrieved successfully", paymentService.getPartnerEarningsSummary(partnerId)));
    }

    @GetMapping("/api/payments/partner/{partnerId}/earnings-detail")
    public ResponseEntity<ApiResponse<PartnerEarningsDetailResponse>> getPartnerEarningsDetail(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.agrorental.security.principal.PartnerPrincipal principal,
            @PathVariable Long partnerId) {
        validatePartnerOwnershipOrAdmin(partnerId, principal);
        return ResponseEntity.ok(ApiResponse.success("Partner earnings detail retrieved successfully", paymentService.getPartnerEarningsDetail(partnerId)));
    }

    @GetMapping("/api/payments/partner/{partnerId}/reports/booking-revenue")
    public ResponseEntity<ApiResponse<List<PartnerBookingRevenueReportDto>>> getPartnerBookingRevenueReport(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.agrorental.security.principal.PartnerPrincipal principal,
            @PathVariable Long partnerId) {
        validatePartnerOwnershipOrAdmin(partnerId, principal);
        return ResponseEntity.ok(ApiResponse.success("Booking revenue report generated successfully", paymentService.getPartnerBookingRevenueReport(partnerId)));
    }

    @GetMapping("/api/payments/partner/{partnerId}/reports/equipment-revenue")
    public ResponseEntity<ApiResponse<List<PartnerEquipmentRevenueReportDto>>> getPartnerEquipmentRevenueReport(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.agrorental.security.principal.PartnerPrincipal principal,
            @PathVariable Long partnerId) {
        validatePartnerOwnershipOrAdmin(partnerId, principal);
        return ResponseEntity.ok(ApiResponse.success("Equipment revenue report generated successfully", paymentService.getPartnerEquipmentRevenueReport(partnerId)));
    }

    @GetMapping("/api/payments/partner/{partnerId}/reports/customer-revenue")
    public ResponseEntity<ApiResponse<List<PartnerCustomerRevenueReportDto>>> getPartnerCustomerRevenueReport(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.agrorental.security.principal.PartnerPrincipal principal,
            @PathVariable Long partnerId) {
        validatePartnerOwnershipOrAdmin(partnerId, principal);
        return ResponseEntity.ok(ApiResponse.success("Customer revenue report generated successfully", paymentService.getPartnerCustomerRevenueReport(partnerId)));
    }
}
