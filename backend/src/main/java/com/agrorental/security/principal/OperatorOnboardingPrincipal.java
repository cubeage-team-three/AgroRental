package com.agrorental.security.principal;

/**
 * Principal of an operator onboarding token (authority ROLE_OPERATOR_ONBOARDING): a just-registered,
 * still PENDING operator whose only capability is uploading its own KYC documents.
 *
 * <p>Deliberately not an {@link OperatorPrincipal}: {@link CurrentCaller#from} does not recognize it, and
 * {@code @AuthenticationPrincipal OperatorPrincipal} parameters resolve to null for it, so no operator,
 * partner, farmer or admin endpoint can mistake it for a session.
 *
 * @param operatorId operators.id of the onboarding operator (the token subject)
 */
public record OperatorOnboardingPrincipal(Long operatorId) {

    public static final String AUTHORITY = "ROLE_OPERATOR_ONBOARDING";
}
