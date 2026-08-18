package com.agrorental.farmer.controller;

import com.agrorental.common.dto.ApiResponse;
import com.agrorental.farmer.dto.OtpResponse;
import com.agrorental.farmer.dto.SendOtpRequest;
import com.agrorental.farmer.dto.VerifyOtpRequest;
import com.agrorental.farmer.service.FarmerOtpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/farmers")
@RequiredArgsConstructor
public class FarmerOtpController {

    private final FarmerOtpService farmerOtpService;

    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<OtpResponse>> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        log.info("REST request to send OTP to mobile: {}", request.getMobileNumber());
        OtpResponse otpResponse = farmerOtpService.sendOtp(request);
        return ResponseEntity.ok(ApiResponse.success(otpResponse.getMessage(), otpResponse));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<OtpResponse>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        log.info("REST request to verify OTP for mobile: {}", request.getMobileNumber());
        OtpResponse otpResponse = farmerOtpService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.success(otpResponse.getMessage(), otpResponse));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<OtpResponse>> resendOtp(@Valid @RequestBody SendOtpRequest request) {
        log.info("REST request to resend OTP for mobile: {}", request.getMobileNumber());
        OtpResponse otpResponse = farmerOtpService.resendOtp(request);
        return ResponseEntity.ok(ApiResponse.success(otpResponse.getMessage(), otpResponse));
    }
}
