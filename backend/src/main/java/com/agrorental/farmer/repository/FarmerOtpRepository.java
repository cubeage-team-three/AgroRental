package com.agrorental.farmer.repository;

import com.agrorental.farmer.entity.FarmerOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FarmerOtpRepository extends JpaRepository<FarmerOtp, Long> {

    Optional<FarmerOtp> findTopByMobileNumberOrderByCreatedAtDesc(String mobileNumber);
}
