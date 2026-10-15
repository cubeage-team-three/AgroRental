package com.agrorental.auth.service;

/**
 * Delivers password reset instructions (email and/or SMS) to an account holder.
 *
 * <p>The repository ships no implementation: no email or SMS provider is configured, so password reset
 * reports itself as unavailable (see {@link PasswordResetService}). To enable reset in staging/production,
 * register exactly one Spring bean implementing this interface that talks to a real provider and returns
 * {@code true} from {@link #isConfigured()} only when its provider credentials are present.
 *
 * <p>Implementations must never log, persist or echo the reset URL or the token it contains.
 */
public interface PasswordResetDelivery {

    /** True only when a real provider is configured and able to deliver reset instructions. */
    boolean isConfigured();

    /**
     * Sends the reset link. Must not throw for an account without a deliverable channel (for example a
     * farmer without an email address when only email is configured): skip silently, so responses stay
     * identical for every account.
     */
    void sendPasswordReset(PasswordResetInstructions instructions);
}
