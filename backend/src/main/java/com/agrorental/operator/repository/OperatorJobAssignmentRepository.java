package com.agrorental.operator.repository;

import com.agrorental.operator.entity.OperatorJobAssignment;
import com.agrorental.operator.enums.OperatorAssignmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for managing OperatorJobAssignment persistence and query operations.
 */
@Repository
public interface OperatorJobAssignmentRepository extends JpaRepository<OperatorJobAssignment, Long> {

    Optional<OperatorJobAssignment> findByBookingIdAndAssignmentStatus(Long bookingId, OperatorAssignmentStatus assignmentStatus);

    Optional<OperatorJobAssignment> findByBookingId(Long bookingId);

    boolean existsByBookingIdAndAssignmentStatus(Long bookingId, OperatorAssignmentStatus assignmentStatus);

    Page<OperatorJobAssignment> findByOperatorId(Long operatorId, Pageable pageable);

    Page<OperatorJobAssignment> findByOperatorIdAndAssignmentStatus(Long operatorId, OperatorAssignmentStatus assignmentStatus, Pageable pageable);

    Optional<OperatorJobAssignment> findByIdAndOperatorId(Long id, Long operatorId);

    List<OperatorJobAssignment> findByOperatorIdAndAssignmentStatus(Long operatorId, OperatorAssignmentStatus assignmentStatus);

    /**
     * Checks if the operator has any conflicting active assignment for overlapping booking dates.
     */
    @Query("""
        SELECT COUNT(a) > 0 FROM OperatorJobAssignment a
        JOIN a.booking b
        WHERE a.operator.id = :operatorId
          AND a.assignmentStatus = :status
          AND b.status = com.agrorental.booking.entity.BookingStatus.CONFIRMED
          AND NOT (b.endDate < :startDate OR b.startDate > :endDate)
    """)
    boolean existsOverlappingAssignment(
            @Param("operatorId") Long operatorId,
            @Param("status") OperatorAssignmentStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // ==========================================
    // PHASE 6: DASHBOARD METRICS QUERIES
    // ==========================================

    /**
     * Aggregates total count of job assignments grouped by assignment status for an operator.
     */
    @Query("""
        SELECT a.assignmentStatus, COUNT(a)
        FROM OperatorJobAssignment a
        WHERE a.operator.id = :operatorId
        GROUP BY a.assignmentStatus
    """)
    List<Object[]> countGroupedByStatusForOperator(@Param("operatorId") Long operatorId);

    /**
     * Counts the total number of all assignments belonging to the operator.
     */
    Long countByOperatorId(Long operatorId);

    /**
     * Counts active assignments scheduled for today's date (startDate <= today <= endDate).
     */
    @Query("""
        SELECT COUNT(a)
        FROM OperatorJobAssignment a
        JOIN a.booking b
        WHERE a.operator.id = :operatorId
          AND a.assignmentStatus NOT IN (
              com.agrorental.operator.enums.OperatorAssignmentStatus.REJECTED,
              com.agrorental.operator.enums.OperatorAssignmentStatus.CANCELLED
          )
          AND b.startDate <= :today
          AND b.endDate >= :today
    """)
    Long countTodayJobsForOperator(@Param("operatorId") Long operatorId, @Param("today") LocalDate today);

    /**
     * Counts upcoming assigned jobs where startDate > today and status is ASSIGNED or ACCEPTED.
     */
    @Query("""
        SELECT COUNT(a)
        FROM OperatorJobAssignment a
        JOIN a.booking b
        WHERE a.operator.id = :operatorId
          AND a.assignmentStatus IN (
              com.agrorental.operator.enums.OperatorAssignmentStatus.ASSIGNED,
              com.agrorental.operator.enums.OperatorAssignmentStatus.ACCEPTED
          )
          AND b.startDate > :today
    """)
    Long countUpcomingJobsForOperator(@Param("operatorId") Long operatorId, @Param("today") LocalDate today);

    /**
     * Retrieves active assignments ordered by operational urgency and update timestamp.
     */
    @Query("""
        SELECT a
        FROM OperatorJobAssignment a
        WHERE a.operator.id = :operatorId
          AND a.assignmentStatus IN (
              com.agrorental.operator.enums.OperatorAssignmentStatus.IN_PROGRESS,
              com.agrorental.operator.enums.OperatorAssignmentStatus.PAUSED,
              com.agrorental.operator.enums.OperatorAssignmentStatus.REACHED,
              com.agrorental.operator.enums.OperatorAssignmentStatus.TRAVELING,
              com.agrorental.operator.enums.OperatorAssignmentStatus.ACCEPTED,
              com.agrorental.operator.enums.OperatorAssignmentStatus.ASSIGNED
          )
        ORDER BY
          CASE a.assignmentStatus
            WHEN com.agrorental.operator.enums.OperatorAssignmentStatus.IN_PROGRESS THEN 1
            WHEN com.agrorental.operator.enums.OperatorAssignmentStatus.PAUSED THEN 2
            WHEN com.agrorental.operator.enums.OperatorAssignmentStatus.REACHED THEN 3
            WHEN com.agrorental.operator.enums.OperatorAssignmentStatus.TRAVELING THEN 4
            WHEN com.agrorental.operator.enums.OperatorAssignmentStatus.ACCEPTED THEN 5
            WHEN com.agrorental.operator.enums.OperatorAssignmentStatus.ASSIGNED THEN 6
            ELSE 7
          END ASC,
          a.updatedAt DESC
    """)
    List<OperatorJobAssignment> findActiveAssignmentsForOperator(@Param("operatorId") Long operatorId);
}
