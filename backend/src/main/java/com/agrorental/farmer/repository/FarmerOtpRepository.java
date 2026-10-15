package com.agrorental.farmer.repository;

import com.agrorental.farmer.entity.FarmerOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface FarmerOtpRepository extends JpaRepository<FarmerOtp, Long> {

    Optional<FarmerOtp> findTopByMobileNumberOrderByCreatedAtDesc(String mobileNumber);

    long countByMobileNumberAndCreatedAtAfter(String mobileNumber, LocalDateTime createdAfter);

    /**
     * Atomically consumes one verification attempt. Returns 0 when the OTP is already used
     * or its attempt limit is reached, so concurrent guesses cannot exceed the limit.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE FarmerOtp o SET o.attemptCount = o.attemptCount + 1 "
            + "WHERE o.id = :id AND o.verified = false AND o.attemptCount < :maxAttempts")
    int incrementAttemptCountIfBelowLimit(@Param("id") Long id, @Param("maxAttempts") int maxAttempts);

    /**
     * Atomically marks an OTP as used. Returns 0 when it was already used, enforcing single use.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE FarmerOtp o SET o.verified = true WHERE o.id = :id AND o.verified = false")
    int markVerifiedIfUnused(@Param("id") Long id);
}
