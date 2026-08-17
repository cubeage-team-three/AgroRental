package com.agrorental.operator.controller;

import com.agrorental.common.dto.ApiResponse;
import com.agrorental.operator.dto.OperatorOtpResponse;
import com.agrorental.operator.dto.OperatorOtpSendRequest;
import com.agrorental.operator.dto.OperatorOtpVerifyRequest;
import com.agrorental.operator.service.OperatorOtpService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/operators/otp")
public class OperatorOtpController {

    private final OperatorOtpService operatorOtpService;

    public OperatorOtpController(OperatorOtpService operatorOtpService) {
        this.operatorOtpService = operatorOtpService;
    }

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<OperatorOtpResponse>> sendOtp(
            @Valid @RequestBody OperatorOtpSendRequest request) {

        OperatorOtpResponse response = operatorOtpService.sendOtp(request);
        return ResponseEntity.ok(ApiResponse.success("OTP generated and sent successfully", response));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<OperatorOtpResponse>> verifyOtp(
            @Valid @RequestBody OperatorOtpVerifyRequest request) {

        OperatorOtpResponse response = operatorOtpService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.success("OTP verified successfully", response));
    }

    @PostMapping("/resend")
    public ResponseEntity<ApiResponse<OperatorOtpResponse>> resendOtp(
            @Valid @RequestBody OperatorOtpSendRequest request) {

        OperatorOtpResponse response = operatorOtpService.resendOtp(request);
        return ResponseEntity.ok(ApiResponse.success("OTP resent successfully", response));
    }
}
