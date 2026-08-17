package com.agrorental.operator.repository;

import com.agrorental.operator.entity.OperatorOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OperatorOtpRepository extends JpaRepository<OperatorOtp, Long> {

    Optional<OperatorOtp> findTopByMobileNumberAndUsedFalseOrderByCreatedAtDesc(String mobileNumber);

    List<OperatorOtp> findAllByMobileNumberAndUsedFalse(String mobileNumber);
}
