package com.agrorental.farmer.repository;

import com.agrorental.farmer.entity.Farm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for Farm entity operations.
 */
@Repository
public interface FarmRepository extends JpaRepository<Farm, Long> {

    List<Farm> findByFarmerId(Long farmerId);

    Optional<Farm> findByIdAndFarmerId(Long id, Long farmerId);
}
