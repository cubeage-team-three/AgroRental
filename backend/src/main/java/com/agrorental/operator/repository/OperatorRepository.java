
package com.agrorental.operator.repository;

import com.agrorental.operator.entity.Operator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA Repository for Operator entity persistence and query methods.
 */
@Repository
public interface OperatorRepository extends JpaRepository<Operator, Long> {

    boolean existsByMobileNumber(String mobileNumber);

    Optional<Operator> findByMobileNumber(String mobileNumber);

    boolean existsByEmail(String email);

    Optional<Operator> findByEmail(String email);

    Optional<Operator> findByMobileNumberOrEmail(String mobileNumber, String email);
}