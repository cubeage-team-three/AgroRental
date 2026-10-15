package com.agrorental.security.jwt;

/**
 * The single account table a JWT subject ID belongs to. Every account table has its own
 * auto-increment ID sequence, so a token's numeric subject is only meaningful together with
 * this type; it must never be resolved against any other table.
 */
public enum AccountType {

    /** farmers.id, role FARMER */
    FARMER("FARMER"),
    /** partners.id, role PARTNER */
    PARTNER("PARTNER"),
    /** operators.id, role OPERATOR */
    OPERATOR("OPERATOR"),
    /** admins.id, role ADMIN */
    ADMIN("ADMIN"),
    /** users.id; only core-user administrators are issued tokens from this table */
    USER("ADMIN");

    private final String role;

    AccountType(String role) {
        this.role = role;
    }

    /** The only role a token of this account type may carry. */
    public String getRole() {
        return role;
    }

    public boolean permitsRole(String tokenRole) {
        return role.equals(tokenRole);
    }

    /** Exact (case-sensitive) parse; returns null for missing or unsupported values. */
    public static AccountType fromClaim(String claim) {
        if (claim == null) {
            return null;
        }
        for (AccountType type : values()) {
            if (type.name().equals(claim)) {
                return type;
            }
        }
        return null;
    }
}
