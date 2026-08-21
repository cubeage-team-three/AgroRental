package com.agrorental.operator.repository;

import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for Operator entity persistence and query methods.
 */
@Repository
public interface OperatorRepository extends JpaRepository<Operator, Long> {

    boolean existsByMobileNumber(String mobileNumber);

    Optional<Operator> findByMobileNumber(String mobileNumber);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    Optional<Operator> findByEmail(String email);

    Optional<Operator> findByMobileNumberOrEmail(String mobileNumber, String email);

    Page<Operator> findByStatus(OperatorStatus status, Pageable pageable);

    Page<Operator> findByStatusAndMobileVerified(OperatorStatus status, boolean mobileVerified, Pageable pageable);

    Page<Operator> findByMobileVerified(boolean mobileVerified, Pageable pageable);

    List<Operator> findByStatus(OperatorStatus status);

    long countByStatus(OperatorStatus status);

    Page<Operator> findByStatusAndActiveAndMobileVerified(OperatorStatus status, boolean active, boolean mobileVerified, Pageable pageable);

    @Query("""
        SELECT o FROM Operator o
        WHERE o.status = :status
          AND o.active = :active
          AND o.mobileVerified = :mobileVerified
          AND (
              LOWER(o.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
              OR LOWER(o.skills) LIKE LOWER(CONCAT('%', :search, '%'))
              OR o.mobileNumber LIKE CONCAT('%', :search, '%')
              OR LOWER(o.address) LIKE LOWER(CONCAT('%', :search, '%'))
          )
    """)
    Page<Operator> searchEligibleOperators(
            @Param("status") OperatorStatus status,
            @Param("active") boolean active,
            @Param("mobileVerified") boolean mobileVerified,
            @Param("search") String search,
            Pageable pageable
    );

    List<Operator> findByPartnerId(Long partnerId);

    @Query("SELECT o FROM Operator o WHERE o.status = :status AND (o.partner.id = :partnerId OR o.partner IS NULL)")
    List<Operator> findAvailableOperatorsForPartner(@Param("status") OperatorStatus status, @Param("partnerId") Long partnerId);
}