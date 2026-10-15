package com.agrorental.operator.repository;

import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for Operator entity persistence and query methods.
 */
@Repository
public interface OperatorRepository extends JpaRepository<Operator, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Operator o WHERE o.id = :id")
    Optional<Operator> findByIdWithLock(@Param("id") Long id);

    boolean existsByMobileNumber(String mobileNumber);

    Optional<Operator> findByMobileNumber(String mobileNumber);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    Optional<Operator> findByEmail(String email);

    Optional<Operator> findByEmailIgnoreCase(String email);

    Optional<Operator> findByMobileNumberOrEmail(String mobileNumber, String email);

    @Query("SELECT o FROM Operator o WHERE o.mobileNumber = :mobile OR LOWER(o.email) = LOWER(:email)")
    Optional<Operator> findByMobileNumberOrEmailIgnoreCase(@Param("mobile") String mobile, @Param("email") String email);

    Page<Operator> findByStatus(OperatorStatus status, Pageable pageable);

    Page<Operator> findByStatusAndMobileVerified(OperatorStatus status, boolean mobileVerified, Pageable pageable);

    Page<Operator> findByMobileVerified(boolean mobileVerified, Pageable pageable);

    List<Operator> findByStatus(OperatorStatus status);

    long countByStatus(OperatorStatus status);

    Page<Operator> findByStatusAndActiveAndMobileVerified(OperatorStatus status, boolean active, boolean mobileVerified, Pageable pageable);

    /**
     * Eligible-operator search for partners: matches name and skills only, so mobile numbers and home
     * addresses cannot be probed through search results.
     */
    @Query("""
        SELECT o FROM Operator o
        WHERE o.status = :status
          AND o.active = :active
          AND o.mobileVerified = :mobileVerified
          AND (
              LOWER(o.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
              OR LOWER(o.skills) LIKE LOWER(CONCAT('%', :search, '%'))
          )
    """)
    Page<Operator> searchEligibleOperatorsByNameOrSkills(
            @Param("status") OperatorStatus status,
            @Param("active") boolean active,
            @Param("mobileVerified") boolean mobileVerified,
            @Param("search") String search,
            Pageable pageable);

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

    // ---- Assignment eligibility (contract v1.1): operational gates are status + active only. ----
    // mobileVerified is informational and deliberately NOT a filter here; the *MobileVerified methods
    // above remain for the admin listing filters.

    /** Assignable operators (e.g. APPROVED + active), regardless of mobile verification. */
    Page<Operator> findByStatusAndActive(OperatorStatus status, boolean active, Pageable pageable);

    /**
     * Assignable-operator search for partners: matches name and skills only, so mobile numbers and home
     * addresses cannot be probed through search results. No mobile-verification filter.
     */
    @Query("""
        SELECT o FROM Operator o
        WHERE o.status = :status
          AND o.active = :active
          AND (
              LOWER(o.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
              OR LOWER(o.skills) LIKE LOWER(CONCAT('%', :search, '%'))
          )
    """)
    Page<Operator> searchAssignableOperatorsByNameOrSkills(
            @Param("status") OperatorStatus status,
            @Param("active") boolean active,
            @Param("search") String search,
            Pageable pageable);

    /** Assignable-operator search for admins (name, skills, mobile, address). No mobile-verification filter. */
    @Query("""
        SELECT o FROM Operator o
        WHERE o.status = :status
          AND o.active = :active
          AND (
              LOWER(o.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
              OR LOWER(o.skills) LIKE LOWER(CONCAT('%', :search, '%'))
              OR o.mobileNumber LIKE CONCAT('%', :search, '%')
              OR LOWER(o.address) LIKE LOWER(CONCAT('%', :search, '%'))
          )
    """)
    Page<Operator> searchAssignableOperators(
            @Param("status") OperatorStatus status,
            @Param("active") boolean active,
            @Param("search") String search,
            Pageable pageable);
}