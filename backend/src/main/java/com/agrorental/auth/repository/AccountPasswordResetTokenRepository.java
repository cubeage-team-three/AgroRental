package com.agrorental.auth.repository;

import com.agrorental.auth.entity.AccountPasswordResetToken;
import com.agrorental.auth.entity.PasswordResetAccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountPasswordResetTokenRepository extends JpaRepository<AccountPasswordResetToken, Long> {

    Optional<AccountPasswordResetToken> findByTokenHash(String tokenHash);

    List<AccountPasswordResetToken> findByAccountTypeAndAccountId(PasswordResetAccountType accountType, Long accountId);

    /**
     * Atomically redeems a token: succeeds (returns 1) only for an unused, unexpired token, so concurrent
     * redemptions of the same token yield exactly one success.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE AccountPasswordResetToken t SET t.usedAt = :now "
            + "WHERE t.tokenHash = :tokenHash AND t.usedAt IS NULL AND t.expiresAt > :now")
    int consumeIfValid(@Param("tokenHash") String tokenHash, @Param("now") LocalDateTime now);

    /** Invalidates every outstanding (unused) token of one account. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE AccountPasswordResetToken t SET t.usedAt = :now "
            + "WHERE t.accountType = :accountType AND t.accountId = :accountId AND t.usedAt IS NULL")
    int invalidateOutstanding(@Param("accountType") PasswordResetAccountType accountType,
                              @Param("accountId") Long accountId,
                              @Param("now") LocalDateTime now);
}
