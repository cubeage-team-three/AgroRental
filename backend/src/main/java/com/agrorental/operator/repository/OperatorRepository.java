
package com.agrorental.operator.repository;

import com.agrorental.operator.entity.Operator;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OperatorRepository extends JpaRepository<Operator, Long> {

    boolean existsByMobileNumber(String mobileNumber);

    Optional<Operator> findByMobileNumber(String mobileNumber);

    Optional<Operator> findByEmail(String email);

    Optional<Operator> findByMobileNumberOrEmail(String mobileNumber, String email);
}