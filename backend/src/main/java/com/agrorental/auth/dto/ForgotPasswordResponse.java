package com.agrorental.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body of POST /api/auth/forgot-password. Outside explicit development it depends only on server
 * configuration, never on whether an account matched the request.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ForgotPasswordResponse {

    private String message;

    /** True when an email/SMS provider is configured to deliver reset instructions. */
    private boolean deliveryConfigured;

    /**
     * DEVELOPMENT ONLY: the raw reset token, present only with an explicitly active 'dev' profile and
     * app.password-reset.dev-token-in-response=true, and only when an account matched. Never in staging/prod.
     */
    private String devResetToken;

    /** Explains {@link #devResetToken}; present whenever development token disclosure is active. */
    private String devNotice;
}
