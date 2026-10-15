package com.agrorental.payment.controller;

import com.agrorental.common.dto.ApiResponse;
import com.agrorental.payment.dto.RefundCompletionRequest;
import com.agrorental.payment.dto.RefundFailureRequest;
import com.agrorental.payment.dto.RefundResponse;
import com.agrorental.payment.dto.RefundRetryRequest;
import com.agrorental.payment.entity.RefundStatus;
import com.agrorental.payment.service.RefundService;
import com.agrorental.security.principal.CurrentCaller;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin refund operations (/api/admin/** is ADMIN-only; the service re-checks). Completion requires the
 * verified transfer reference of the actual bank/UPI refund; there is no payment-provider integration.
 */
@RestController
@RequestMapping("/api/admin/refunds")
@RequiredArgsConstructor
public class AdminRefundController {

    private final RefundService refundService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RefundResponse>>> listRefunds(
            @RequestParam(required = false) RefundStatus status, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Refunds retrieved successfully",
                refundService.listRefunds(status, CurrentCaller.from(authentication))));
    }

    @GetMapping("/{refundId}")
    public ResponseEntity<ApiResponse<RefundResponse>> getRefund(@PathVariable Long refundId, Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Refund retrieved successfully",
                refundService.getRefund(refundId, CurrentCaller.from(authentication))));
    }

    @PostMapping("/{refundId}/complete")
    public ResponseEntity<ApiResponse<RefundResponse>> completeRefund(@PathVariable Long refundId,
                                                                      @Valid @RequestBody RefundCompletionRequest request,
                                                                      Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Refund marked as completed",
                refundService.completeRefund(refundId, request.getTransferReference(), request.getNote(),
                        CurrentCaller.from(authentication), authentication.getName())));
    }

    @PostMapping("/{refundId}/fail")
    public ResponseEntity<ApiResponse<RefundResponse>> markRefundFailed(@PathVariable Long refundId,
                                                                        @Valid @RequestBody RefundFailureRequest request,
                                                                        Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Refund marked as failed",
                refundService.markRefundFailed(refundId, request.getReason(),
                        CurrentCaller.from(authentication), authentication.getName())));
    }

    @PostMapping("/{refundId}/retry")
    public ResponseEntity<ApiResponse<RefundResponse>> retryRefund(@PathVariable Long refundId,
                                                                   @RequestBody(required = false) RefundRetryRequest request,
                                                                   Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success("Refund re-opened for another transfer attempt",
                refundService.retryRefund(refundId, request != null ? request.getNote() : null,
                        CurrentCaller.from(authentication), authentication.getName())));
    }
}
