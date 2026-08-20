package com.agrorental.operator.repository;

import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OperatorRepository extends JpaRepository<Operator, Long> {

    boolean existsByMobileNumber(String mobileNumber);

    Optional<Operator> findByMobileNumber(String mobileNumber);

    List<Operator> findByStatus(OperatorStatus status);

    List<Operator> findByPartnerId(Long partnerId);

    @Query("SELECT o FROM Operator o WHERE o.status = :status AND (o.partner.id = :partnerId OR o.partner IS NULL)")
    List<Operator> findAvailableOperatorsForPartner(@Param("status") OperatorStatus status, @Param("partnerId") Long partnerId);
}