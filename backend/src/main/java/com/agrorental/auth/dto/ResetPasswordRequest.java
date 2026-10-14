package com.agrorental.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for fulfilling password reset with a valid token.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResetPasswordRequest {

    /**
     * Recovery token string (used for email link and OTP token redemption flows).
     */
    private String token;

    /**
     * Target role validation ("FARMER", "PARTNER", or "OPERATOR").
     * Prevents cross-role token redemption.
     */
    private String role;

    /**
     * Mobile number for direct OTP verification fallback.
     */
    private String mobileNumber;

    /**
     * OTP code for direct OTP verification fallback.
     */
    private String otp;

    @NotBlank(message = "New password is required")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String newPassword;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;
}
