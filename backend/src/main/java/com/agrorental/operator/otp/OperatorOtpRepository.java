package com.agrorental.operator.otp;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for Operator OTP management.
 */
@Repository
public interface OperatorOtpRepository extends JpaRepository<OperatorOtp, Long> {

    Optional<OperatorOtp> findTopByMobileNumberAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(
            String mobileNumber,
            OtpPurpose purpose
    );

    List<OperatorOtp> findByMobileNumberAndPurposeAndVerifiedFalse(
            String mobileNumber,
            OtpPurpose purpose
    );
}
