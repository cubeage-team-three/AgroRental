package com.agrorental.payment.controller;

import com.agrorental.common.dto.ApiResponse;
import com.agrorental.payment.dto.InvoiceResponse;
import com.agrorental.payment.dto.PaymentRequest;
import com.agrorental.payment.dto.PaymentResponse;
import com.agrorental.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/farmers")
@CrossOrigin(origins = "http://localhost:5173")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/payments")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @Valid @RequestBody PaymentRequest request) {

        PaymentResponse response = paymentService.processPayment(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Payment processed successfully", response));
    }

    @GetMapping("/payments/{id}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(
            @PathVariable Long id) {

        PaymentResponse response = paymentService.getPaymentById(id);

        return ResponseEntity.ok(
                ApiResponse.success("Payment details retrieved successfully", response));
    }

    @GetMapping("/payments/farmer/{farmerId}")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getFarmerPayments(
            @PathVariable Long farmerId) {

        List<PaymentResponse> response = paymentService.getFarmerPayments(farmerId);

        return ResponseEntity.ok(
                ApiResponse.success("Farmer payments retrieved successfully", response));
    }

    @GetMapping("/bookings/{bookingId}/invoice")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoice(
            @PathVariable Long bookingId) {

        InvoiceResponse response = paymentService.generateInvoice(bookingId);

        return ResponseEntity.ok(
                ApiResponse.success("Invoice generated successfully", response));
    }
}
