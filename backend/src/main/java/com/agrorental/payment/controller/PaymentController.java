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
@CrossOrigin(origins = "http://localhost:5173")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // --- Farmer Payment & Invoice Endpoints ---

    @PostMapping("/api/farmers/payments")
    public ResponseEntity<ApiResponse<PaymentResponse>> processFarmerPayment(
            @Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.processPayment(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment processed successfully", response));
    }

    @GetMapping("/api/farmers/payments/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getFarmerPaymentById(
            @PathVariable Long id) {
        PaymentResponse response = paymentService.getPaymentById(id);
        return ResponseEntity.ok(ApiResponse.success("Payment details retrieved successfully", response));
    }

    @GetMapping("/api/farmers/payments/farmer/{farmerId}")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getFarmerPayments(
            @PathVariable Long farmerId) {
        List<PaymentResponse> response = paymentService.getFarmerPayments(farmerId);
        return ResponseEntity.ok(ApiResponse.success("Farmer payments retrieved successfully", response));
    }

    @GetMapping("/api/farmers/bookings/{bookingId}/invoice")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoice(
            @PathVariable Long bookingId) {
        InvoiceResponse response = paymentService.generateInvoice(bookingId);
        return ResponseEntity.ok(ApiResponse.success("Invoice generated successfully", response));
    }

    // --- Partner & General Payment Endpoints ---

    @PostMapping("/api/payments")
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody PaymentCreateRequest request) {
        PaymentResponse response = paymentService.createPayment(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/payments/{id}")
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    @GetMapping("/api/payments/booking/{bookingId}")
    public ResponseEntity<PaymentResponse> getPaymentByBookingId(@PathVariable Long bookingId) {
        return ResponseEntity.ok(paymentService.getPaymentByBookingId(bookingId));
    }

    @GetMapping("/api/payments/farmer/{farmerId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByFarmer(@PathVariable Long farmerId) {
        return ResponseEntity.ok(paymentService.getPaymentsByFarmer(farmerId));
    }

    @GetMapping("/api/payments/partner/{partnerId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByPartner(@PathVariable Long partnerId) {
        return ResponseEntity.ok(paymentService.getPaymentsByPartner(partnerId));
    }

    @GetMapping("/api/payments/partner/{partnerId}/earnings")
    public ResponseEntity<PartnerEarningsSummary> getPartnerEarningsSummary(@PathVariable Long partnerId) {
        return ResponseEntity.ok(paymentService.getPartnerEarningsSummary(partnerId));
    }
}
