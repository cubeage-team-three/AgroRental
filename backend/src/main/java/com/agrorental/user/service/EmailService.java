package com.agrorental.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Email notifications for core-user accounts.
 *
 * <p>No email provider is configured in this application: nothing is actually sent. Links carrying a token
 * are never written to the logs (password reset delivery is handled separately, see
 * {@link com.agrorental.auth.service.PasswordResetDelivery}).
 */
@Slf4j
@Service
public class EmailService {

    /**
     * Would send the email verification link to a newly registered core user. With no provider configured the
     * link is dropped; only the fact that nothing was sent is logged (never the link or its token).
     *
     * @param recipientEmail Registered user email address.
     * @param token          Verification token (secret; never logged).
     */
    public void sendVerificationEmail(String recipientEmail, String token) {
        log.info("Email verification link generated; no email provider is configured, so no email was sent");
    }
}
