package com.agrorental.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * A single-use password reset token (Flyway V3, table account_password_reset_tokens).
 *
 * <p>Only the SHA-256 hash of the token is persisted; the raw token exists solely in the reset link handed
 * to the account holder. {@code usedAt} is set exactly once, through an atomic conditional update, when the
 * token is redeemed or invalidated.
 */
@Entity
@Table(
    name = "account_password_reset_tokens",
    uniqueConstraints = @UniqueConstraint(name = "uk_account_password_reset_tokens_token_hash", columnNames = "token_hash"),
    indexes = @Index(name = "idx_account_password_reset_tokens_account", columnList = "account_type, account_id, used_at")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "tokenHash")
public class AccountPasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private PasswordResetAccountType accountType;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    /** Lowercase hex SHA-256 of the raw token. */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
