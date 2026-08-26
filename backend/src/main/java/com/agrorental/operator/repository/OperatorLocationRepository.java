package com.agrorental.operator.repository;

import com.agrorental.operator.entity.OperatorLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for OperatorLocation persistence and retrieval.
 */
@Repository
public interface OperatorLocationRepository extends JpaRepository<OperatorLocation, Long> {

    /**
     * Finds the latest recorded GPS location for a given assignment.
     */
    Optional<OperatorLocation> findTopByAssignmentIdOrderByRecordedAtDesc(Long assignmentId);

    /**
     * Finds the latest recorded GPS location for a given assignment and operator.
     */
    Optional<OperatorLocation> findTopByAssignmentIdAndOperatorIdOrderByRecordedAtDesc(Long assignmentId, Long operatorId);

    /**
     * Finds all active tracking location records for a given assignment.
     */
    List<OperatorLocation> findByAssignmentIdAndTrackingActiveTrue(Long assignmentId);

    /**
     * Deactivates tracking for an assignment.
     */
    @Modifying
    @Query("UPDATE OperatorLocation ol SET ol.trackingActive = false WHERE ol.assignment.id = :assignmentId AND ol.trackingActive = true")
    int deactivateTrackingForAssignment(@Param("assignmentId") Long assignmentId);
}
