package com.agrorental.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Starts a password reset for a FARMER, PARTNER or OPERATOR account: {@code {role, identifier}}, where the
 * identifier is the registered mobile number or email address. Admin accounts cannot use self-service reset.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForgotPasswordRequest {

    /**
     * Target role: "FARMER", "PARTNER" or "OPERATOR". Defaults to "FARMER" when omitted (backward compatibility).
     */
    private String role;

    /** Legacy alias of {@link #identifier}. */
    private String email;

    /** Legacy alias of {@link #identifier}. */
    private String mobileOrEmail;

    /** Registered mobile number or email address. */
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
