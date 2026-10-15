package com.agrorental.security.principal;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

/**
 * The authenticated caller's account kind and account ID, derived solely from the JWT-backed principal
 * (never from request parameters, paths or bodies). Services use it to enforce resource ownership.
 */
public record CurrentCaller(Kind kind, Long accountId) {

    public enum Kind { FARMER, PARTNER, OPERATOR, ADMIN }

    public static CurrentCaller from(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required.");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof FarmerPrincipal farmer) {
            return new CurrentCaller(Kind.FARMER, farmer.getId());
        }
        if (principal instanceof PartnerPrincipal partner) {
            return new CurrentCaller(Kind.PARTNER, partner.getId());
        }
        if (principal instanceof OperatorPrincipal operator) {
            return new CurrentCaller(Kind.OPERATOR, operator.getId());
        }
        if (authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
            return new CurrentCaller(Kind.ADMIN, null);
        }
        throw new AccessDeniedException("Unsupported principal.");
    }

    public boolean isAdmin() {
        return kind == Kind.ADMIN;
    }

    /** True when the caller is of the given kind and owns the given account ID. */
    public boolean is(Kind expectedKind, Long ownerId) {
        return kind == expectedKind && accountId != null && accountId.equals(ownerId);
    }
}
