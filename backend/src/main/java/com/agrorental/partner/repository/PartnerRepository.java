package com.agrorental.partner.repository;

import com.agrorental.partner.entity.Partner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PartnerRepository extends JpaRepository<Partner, Long> {

    boolean existsByMobileNumber(String mobileNumber);

    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<Partner> findByMobileNumber(String mobileNumber);

    Optional<Partner> findByEmail(String email);

    Optional<Partner> findByEmailIgnoreCase(String email);

    Optional<Partner> findByMobileNumberOrEmail(String mobileNumber, String email);

    Optional<Partner> findByMobileNumberOrEmailIgnoreCase(String mobileNumber, String email);
}