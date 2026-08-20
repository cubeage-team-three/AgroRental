package com.agrorental.security.principal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Authenticated principal representation for an Equipment Operator in Spring Security.
 * Encapsulates essential identity metadata without leaking passwords or PII.
 */
@Getter
@Builder
@AllArgsConstructor
public class OperatorPrincipal implements UserDetails {

    private final Long id;
    private final String mobileNumber;
    private final String fullName;
    private final String role;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + (role != null ? role : "OPERATOR")));
    }

    @Override
    public String getPassword() {
        return null; // Credential hash is not stored in security context
    }

    @Override
    public String getUsername() {
        return mobileNumber;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
