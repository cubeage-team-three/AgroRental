package com.agrorental.auth.service;

import com.agrorental.auth.entity.PasswordResetAccountType;

import java.time.LocalDateTime;

/**
 * What a {@link PasswordResetDelivery} needs to send reset instructions. {@link #toString()} is redacted so
 * the reset URL (which carries the token) can never reach a log through string conversion.
 *
 * @param email        the account's email address, or null when it has none
 * @param mobileNumber the account's mobile number, or null when it has none
 * @param resetUrl     frontend link containing the single-use token; secret
 */
public record PasswordResetInstructions(
        PasswordResetAccountType accountType,
        Long accountId,
        String recipientName,
        String email,
        String mobileNumber,
        String resetUrl,
        LocalDateTime expiresAt) {

    @Override
    public String toString() {
        return "PasswordResetInstructions[accountType=" + accountType + ", accountId=" + accountId
                + ", expiresAt=" + expiresAt + ", resetUrl=<redacted>]";
    }
}
