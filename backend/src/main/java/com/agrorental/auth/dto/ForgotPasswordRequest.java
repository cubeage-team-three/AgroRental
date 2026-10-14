package com.agrorental.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for initiating password reset request across FARMER, PARTNER, and OPERATOR roles.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForgotPasswordRequest {

    /**
     * Target role for account recovery: "FARMER", "PARTNER", or "OPERATOR".
     * Defaults to "FARMER" if omitted for backward compatibility.
     */
    private String role;

    /**
     * Registered email address.
     */
    private String email;

    /**
     * Mobile number or email address identifier.
     */
    private String mobileOrEmail;

    /**
     * Explicit identifier field (mobile number or email address).
     */
    private String identifier;

    public String getAccountIdentifier() {
        if (identifier != null && !identifier.trim().isEmpty()) {
            return identifier.trim();
        }
        if (mobileOrEmail != null && !mobileOrEmail.trim().isEmpty()) {
            return mobileOrEmail.trim();
        }
        if (email != null && !email.trim().isEmpty()) {
            return email.trim();
        }
        return null;
    }

    @AssertTrue(message = "Account identifier (email or mobile number) is required")
    public boolean isIdentifierValid() {
        String id = getAccountIdentifier();
        return id != null && !id.trim().isEmpty();
    }
}

