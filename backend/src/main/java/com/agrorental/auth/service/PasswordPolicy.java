package com.agrorental.auth.service;

import com.agrorental.common.exception.BadRequestException;

import java.nio.charset.StandardCharsets;

/**
 * The single password policy for every account password chosen by a user: farmer, partner and operator
 * registration, the legacy users-table registration, password reset and in-app password changes.
 *
 * <p>Rules: 8-64 characters, at least one letter and one digit, not only whitespace. The password is
 * checked exactly as typed (it is never trimmed or otherwise altered before hashing). Because BCrypt only
 * accepts 72 bytes of input, passwords whose UTF-8 encoding exceeds 72 bytes are refused as well (this can
 * only happen with many non-ASCII characters).
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 64;
    static final int MAX_UTF8_BYTES = 72;

    public static final String REQUIREMENTS_MESSAGE =
            "Password must be 8-64 characters long and contain at least one letter and one number.";
    public static final String TOO_LONG_MESSAGE =
            "Password is too long. Please use fewer characters (non-English characters count more).";

    private PasswordPolicy() {
    }

    /**
     * @return the violation message, or {@code null} when the password satisfies the policy
     */
    public static String violation(String password) {
        if (password == null || password.isBlank()) {
            return REQUIREMENTS_MESSAGE;
        }
        int length = password.codePointCount(0, password.length());
        if (length < MIN_LENGTH || length > MAX_LENGTH) {
            return REQUIREMENTS_MESSAGE;
        }
        boolean hasLetter = password.codePoints().anyMatch(Character::isLetter);
        boolean hasDigit = password.codePoints().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) {
            return REQUIREMENTS_MESSAGE;
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_UTF8_BYTES) {
            return TOO_LONG_MESSAGE;
        }
        return null;
    }

    public static boolean isValid(String password) {
        return violation(password) == null;
    }

    /**
     * @throws BadRequestException with a user-facing message when the password violates the policy
     */
    public static void validate(String password) {
        String violation = violation(password);
        if (violation != null) {
            throw new BadRequestException(violation);
        }
    }
}
