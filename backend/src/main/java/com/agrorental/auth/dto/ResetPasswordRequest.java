package com.agrorental.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Completes a password reset with the single-use token from the reset link. The new password must satisfy
 * {@link com.agrorental.auth.service.PasswordPolicy}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"token", "newPassword", "confirmPassword"})
public class ResetPasswordRequest {

    /** Raw token from the reset link (only its SHA-256 hash is stored server-side). */
    private String token;

    /**
     * Optional: FARMER, PARTNER or OPERATOR. When present it must match the token's account type, otherwise
     * the request fails exactly like an invalid token (and the token stays usable).
     */
    private String role;

    @NotBlank(message = "New password is required")
    private String newPassword;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;
}
