package com.agrorental.auth.entity;

import java.util.Locale;

/**
 * The account tables that support self-service password reset. Admin accounts are deliberately excluded.
 * A reset token's account_id is only meaningful together with this type and is never resolved against
 * any other table.
 */
public enum PasswordResetAccountType {
    /** farmers.id */
    FARMER,
    /** operators.id */
    OPERATOR,
    /** partners.id */
    PARTNER;

    /** Case-insensitive parse of a requested role; null for missing or unsupported roles (including ADMIN). */
    public static PasswordResetAccountType fromRole(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        for (PasswordResetAccountType type : values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        return null;
    }
}
