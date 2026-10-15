package com.agrorental.auth.controller;

import com.agrorental.auth.dto.ForgotPasswordRequest;
import com.agrorental.auth.dto.ForgotPasswordResponse;
import com.agrorental.auth.dto.LoginRequest;
import com.agrorental.auth.dto.LoginResponse;
import com.agrorental.auth.dto.ResetPasswordRequest;
import com.agrorental.auth.service.AuthService;
import com.agrorental.auth.service.PasswordResetRateLimitedException;
import com.agrorental.auth.service.PasswordResetService;
import com.agrorental.common.dto.ApiResponse;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.user.dto.UserRegistrationRequest;
import com.agrorental.user.dto.UserRegistrationResponse;
import com.agrorental.user.service.UserRegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
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

    /**
     * Legacy users-table farmer registration (no UI uses it; the app registers farmers through
     * /api/farmers/register). Applies the shared password policy and creates an active account.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserRegistrationResponse>> register(@Valid @RequestBody UserRegistrationRequest request) {
        log.info("Received legacy user registration request");
        UserRegistrationResponse response = userRegistrationService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", response));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<String>> verifyEmail(@RequestParam("token") String token) {
        log.info("Received email verification request");
        String message = userRegistrationService.verifyEmailToken(token);
        return ResponseEntity.ok(ApiResponse.success(message, message));
    }

    /**
     * Starts a password reset. The status and body are identical for existing and non-existing accounts;
     * the message depends only on whether email/SMS delivery is configured.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<ForgotPasswordResponse>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request,
                                                                              HttpServletRequest httpRequest) {
        log.info("Received forgot password request");
        ForgotPasswordResponse response = passwordResetService.requestReset(request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Received reset password request");
        String message = passwordResetService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(message, message));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Received login request (role {})", request.getRole());
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
    }

    /** OTP login has been removed; every account signs in with its password. */
    @PostMapping("/login-otp")
    public ResponseEntity<ApiResponse<LoginResponse>> loginWithOtp(@RequestBody(required = false) LoginRequest request) {
        log.info("Rejected OTP login request: OTP login is disabled");
        throw new BadRequestException(AuthService.OTP_LOGIN_DISABLED_MESSAGE);
    }

    @ExceptionHandler(PasswordResetRateLimitedException.class)
    public ResponseEntity<ApiResponse<Object>> handleRateLimited(PasswordResetRateLimitedException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
                .body(ApiResponse.error(PasswordResetRateLimitedException.MESSAGE));
    }
}
