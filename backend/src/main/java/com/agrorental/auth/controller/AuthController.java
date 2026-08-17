package com.agrorental.auth.controller;

import com.agrorental.auth.dto.LoginRequest;
import com.agrorental.auth.dto.LoginResponse;
import com.agrorental.auth.service.AuthService;
import com.agrorental.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Received login request for: {}", request.getMobileOrEmail());
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }

    @PostMapping("/login-otp")
    public ResponseEntity<ApiResponse<LoginResponse>> loginWithOtp(@Valid @RequestBody LoginRequest request) {
        log.info("Received OTP login request for: {}", request.getMobileOrEmail());
        request.setLoginType("OTP");
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }
}
