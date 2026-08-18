package com.agrorental.farmer.repository;

import com.agrorental.farmer.entity.Farmer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FarmerRepository extends JpaRepository<Farmer, Long> {

    boolean existsByMobileNumber(String mobileNumber);

    Optional<Farmer> findByMobileNumber(String mobileNumber);
}
