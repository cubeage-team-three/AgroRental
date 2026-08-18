package com.agrorental.farmer.repository;

import com.agrorental.farmer.entity.Farmer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FarmerRepository extends JpaRepository<Farmer, Long> {

    boolean existsByMobileNumber(String mobileNumber);

    boolean existsByEmail(String email);

    Optional<Farmer> findByMobileNumber(String mobileNumber);

    Optional<Farmer> findByEmail(String email);

    Optional<Farmer> findByMobileNumberOrEmail(String mobileNumber, String email);
}
