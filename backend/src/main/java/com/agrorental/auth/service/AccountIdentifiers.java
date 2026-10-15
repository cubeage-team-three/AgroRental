package com.agrorental.auth.service;

import java.util.Locale;

/** Normalization of user-entered account identifiers (mobile number or email address). */
public final class AccountIdentifiers {

    private AccountIdentifiers() {
    }

    public static boolean looksLikeEmail(String identifier) {
        return identifier != null && identifier.contains("@");
    }

    /**
     * The 10-digit mobile number behind common input formats ("+91 98765 43210", "09876543210",
     * "98765-43210"), or null when the input is an email address or not a recognizable mobile number.
     */
    public static String normalizeMobile(String input) {
        if (input == null || looksLikeEmail(input)) {
            return null;
        }
        String digits = input.replaceAll("\\D", "");
        if (digits.length() == 10) {
            return digits;
        }
        if (digits.length() == 12 && digits.startsWith("91")) {
            return digits.substring(2);
        }
        if (digits.length() == 11 && digits.startsWith("0")) {
            return digits.substring(1);
        }
        return null;
    }

    /** Log-safe form of a mobile number or email: only the last 4 characters (or the domain) remain visible. */
    public static String mask(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return "<none>";
        }
        String trimmed = identifier.trim();
        int at = trimmed.indexOf('@');
        if (at >= 0) {
            return "***" + trimmed.substring(at);
        }
        return trimmed.length() <= 4 ? "****" : "******" + trimmed.substring(trimmed.length() - 4);
    }

    /** Canonical form used for rate-limit keys, so formatting variations of one identifier share a bucket. */
    public static String canonical(String identifier) {
        if (identifier == null) {
            return "";
        }
        String trimmed = identifier.trim();
        String mobile = normalizeMobile(trimmed);
        return mobile != null ? mobile : trimmed.toLowerCase(Locale.ROOT);
    }
}
