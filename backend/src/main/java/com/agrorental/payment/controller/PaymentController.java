package com.agrorental.payment.controller;

import com.agrorental.payment.dto.PartnerEarningsSummary;
import com.agrorental.payment.dto.PaymentCreateRequest;
import com.agrorental.payment.dto.PaymentResponse;
import com.agrorental.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller exposing endpoints for rental payment processing and financial reports.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody PaymentCreateRequest request) {
        PaymentResponse response = paymentService.createPayment(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<PaymentResponse> getPaymentByBookingId(@PathVariable Long bookingId) {
        return ResponseEntity.ok(paymentService.getPaymentByBookingId(bookingId));
    }

    @GetMapping("/farmer/{farmerId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByFarmer(@PathVariable Long farmerId) {
        return ResponseEntity.ok(paymentService.getPaymentsByFarmer(farmerId));
    }

    @GetMapping("/partner/{partnerId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByPartner(@PathVariable Long partnerId) {
        return ResponseEntity.ok(paymentService.getPaymentsByPartner(partnerId));
    }

    @GetMapping("/partner/{partnerId}/earnings")
    public ResponseEntity<PartnerEarningsSummary> getPartnerEarningsSummary(@PathVariable Long partnerId) {
        return ResponseEntity.ok(paymentService.getPartnerEarningsSummary(partnerId));
    }
}
