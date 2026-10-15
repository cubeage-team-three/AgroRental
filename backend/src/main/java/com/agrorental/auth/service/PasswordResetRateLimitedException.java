package com.agrorental.auth.service;

/**
 * Raised when a forgot-password request exceeds its rate limit. Mapped to HTTP 429 with a generic body that
 * is identical for existing and non-existing accounts (the limit is checked before any account lookup).
 */
public class PasswordResetRateLimitedException extends RuntimeException {

    public static final String MESSAGE = "Too many password reset requests. Please try again later.";

    private final long retryAfterSeconds;

    public PasswordResetRateLimitedException(long retryAfterSeconds) {
        super(MESSAGE);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
