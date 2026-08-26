package com.agrorental.auth.controller;

import com.agrorental.auth.dto.ForgotPasswordRequest;
import com.agrorental.auth.dto.LoginRequest;
import com.agrorental.auth.dto.LoginResponse;
import com.agrorental.auth.dto.ResetPasswordRequest;
import com.agrorental.auth.service.AuthService;
import com.agrorental.auth.service.PasswordResetService;
import com.agrorental.common.dto.ApiResponse;
import com.agrorental.user.dto.UserRegistrationRequest;
import com.agrorental.user.dto.UserRegistrationResponse;
import com.agrorental.user.service.UserRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRegistrationService userRegistrationService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserRegistrationResponse>> register(@Valid @RequestBody UserRegistrationRequest request) {
        log.info("Received user registration request for email: {}", request.getEmail());
        UserRegistrationResponse response = userRegistrationService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", response));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<String>> verifyEmail(@RequestParam("token") String token) {
        log.info("Received email verification request for token");
        String message = userRegistrationService.verifyEmailToken(token);
        return ResponseEntity.ok(ApiResponse.success(message, message));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Received forgot password request for email");
        String message = passwordResetService.processForgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success(message, message));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Received reset password request");
        String message = passwordResetService.processResetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(message, message));
    }

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
