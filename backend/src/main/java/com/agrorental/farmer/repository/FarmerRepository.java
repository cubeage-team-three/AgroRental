package com.agrorental.farmer.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.agrorental.farmer.entity.Farmer;

@Repository
public interface FarmerRepository extends JpaRepository<Farmer, Long>, JpaSpecificationExecutor<Farmer> {

    boolean existsByMobileNumber(String mobileNumber);

    boolean existsByEmail(String email);

    Optional<Farmer> findByMobileNumber(String mobileNumber);

    Optional<Farmer> findByEmail(String email);

    Optional<Farmer> findByEmailIgnoreCase(String email);

    Optional<Farmer> findByMobileNumberOrEmail(String mobileNumber, String email);

    @Query("SELECT f FROM Farmer f WHERE f.mobileNumber = :mobile OR LOWER(f.email) = LOWER(:email)")
    Optional<Farmer> findByMobileNumberOrEmailIgnoreCase(@Param("mobile") String mobile, @Param("email") String email);

    /**
     * Search farmers by full name, mobile number, or email.
     *
     * @param search Search term to match against fullName, mobileNumber, or email
     * @param pageable Pagination parameters
     * @return Page of matching farmers
     */
    @Query("SELECT f FROM Farmer f WHERE " +
            "LOWER(f.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "f.mobileNumber LIKE CONCAT('%', :search, '%') OR " +
            "LOWER(f.email) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Farmer> searchByNameOrPhoneOrEmail(@Param("search") String search, Pageable pageable);
}
