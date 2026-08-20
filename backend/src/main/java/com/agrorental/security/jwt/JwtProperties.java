package com.agrorental.security.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Configuration properties holder for JWT authentication parameters.
 */
@Getter
@Setter
@Component
public class JwtProperties {

    @Value("${app.jwt.secret:agrorental-super-secret-jwt-key-for-development-mode-only-must-be-256-bits-minimum-change-in-prod}")
    private String secret;

    @Value("${app.jwt.expiration:3600000}")
    private long expirationMs;
}
