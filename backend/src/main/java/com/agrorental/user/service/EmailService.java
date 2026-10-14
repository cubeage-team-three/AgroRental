package com.agrorental.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service handling email notifications, verification dispatches, and password reset dispatches.
 */
@Slf4j
@Service
public class EmailService {

    @Value("${app.mail.from:noreply@agrorent.in}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendBaseUrl;

    /**
     * Sends an email verification link to a registered user.
     *
     * @param recipientEmail Registered user email address.
     * @param token          Secure verification UUID token.
     */
    public void sendVerificationEmail(String recipientEmail, String token) {
        String verificationUrl = frontendBaseUrl + "/verify-email?token=" + token;
        log.info("Sending Email Verification to: {} from: {}", recipientEmail, fromEmail);
        log.info("Verification URL: {}", verificationUrl);
    }

    /**
     * Sends a password reset link to a user.
     *
     * @param recipientEmail Registered user email address.
     * @param token          Secure reset UUID token.
     */
    public void sendPasswordResetEmail(String recipientEmail, String token) {
        sendPasswordResetEmail(recipientEmail, token, null);
    }

    /**
     * Sends a role-aware password reset link to a user.
     *
     * @param recipientEmail Registered user email address.
     * @param token          Secure reset UUID token.
     * @param role           Role of the target account (FARMER, PARTNER, OPERATOR).
     */
    public void sendPasswordResetEmail(String recipientEmail, String token, String role) {
        String roleParam = (role != null && !role.isBlank()) ? "&role=" + role.trim().toUpperCase() : "";
        String resetUrl = frontendBaseUrl + "/reset-password?token=" + token + roleParam;
        log.info("Sending Password Reset Link to: {} for role: {} from: {}",
                recipientEmail, role != null ? role : "UNSPECIFIED", fromEmail);
        log.debug("Password Reset URL generated: [PROTECTED]");
    }
}
