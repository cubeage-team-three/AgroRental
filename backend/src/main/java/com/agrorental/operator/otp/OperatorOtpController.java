package com.agrorental.operator.otp;

import com.agrorental.common.dto.ApiResponse;
import com.agrorental.operator.dto.OperatorOtpResponse;
import com.agrorental.operator.dto.OperatorOtpSendRequest;
import com.agrorental.operator.dto.OperatorOtpVerifyRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller exposing endpoints for Operator mobile OTP dispatch and verification.
 */
@Slf4j
@RestController
@RequestMapping("/api/operators/otp")
@RequiredArgsConstructor
public class OperatorOtpController {

    private final OperatorOtpService operatorOtpService;

    /**
     * Dispatches an OTP to the Operator's mobile number.
     *
     * @param request Validated OTP send request
     * @return OperatorOtpResponse envelope
     */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<OperatorOtpResponse>> sendOtp(
            @Valid @RequestBody OperatorOtpSendRequest request) {
        log.info("Received request to send OTP to mobile: [PROTECTED]");
        OperatorOtpResponse response = operatorOtpService.sendOtp(request);
        return ResponseEntity.ok(ApiResponse.success("OTP sent successfully", response));
    }

    /**
     * Verifies the submitted OTP for the Operator's mobile number.
     *
     * @param request Validated OTP verify request
     * @return OperatorOtpResponse envelope
     */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<OperatorOtpResponse>> verifyOtp(
            @Valid @RequestBody OperatorOtpVerifyRequest request) {
        log.info("Received request to verify OTP for mobile: [PROTECTED]");
        OperatorOtpResponse response = operatorOtpService.verifyOtp(request);
        return ResponseEntity.ok(ApiResponse.success("OTP verified successfully", response));
    }
}
