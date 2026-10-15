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
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Spring Data JPA repository for managing OperatorJobAssignment persistence and query operations.
 */
@Repository
public interface OperatorJobAssignmentRepository extends JpaRepository<OperatorJobAssignment, Long> {

    Optional<OperatorJobAssignment> findByBookingIdAndAssignmentStatus(Long bookingId, OperatorAssignmentStatus assignmentStatus);

    /**
     * Single-result lookup: fails with a non-unique-result error once a booking has more than one assignment
     * (an operator declined and the booking was reassigned). Use {@link #findCurrentAssignmentForBooking} instead.
     */
    Optional<OperatorJobAssignment> findByBookingId(Long bookingId);

    List<OperatorJobAssignment> findAllByBookingId(Long bookingId);

    /**
     * The booking's current operator assignment: the newest active one (ASSIGNED, ACCEPTED, TRAVELING, REACHED,
     * IN_PROGRESS, PAUSED), otherwise the newest COMPLETED one. REJECTED and CANCELLED assignments are history and
     * never "current". Safe for bookings that accumulated several assignments through decline and reassignment.
     */
    default Optional<OperatorJobAssignment> findCurrentAssignmentForBooking(Long bookingId) {
        List<OperatorJobAssignment> assignments = findAllByBookingId(bookingId);
        Comparator<OperatorJobAssignment> byId = Comparator.comparing(OperatorJobAssignment::getId,
                Comparator.nullsFirst(Comparator.naturalOrder()));
        Optional<OperatorJobAssignment> active = assignments.stream()
                .filter(a -> CURRENT_ACTIVE_STATUSES.contains(a.getAssignmentStatus()))
                .max(byId);
        return active.isPresent()
                ? active
                : assignments.stream().filter(a -> a.getAssignmentStatus() == OperatorAssignmentStatus.COMPLETED).max(byId);
    }

    /** Assignment states in which the operator still holds the job. */
    Set<OperatorAssignmentStatus> CURRENT_ACTIVE_STATUSES = Collections.unmodifiableSet(EnumSet.of(
            OperatorAssignmentStatus.ASSIGNED,
            OperatorAssignmentStatus.ACCEPTED,
            OperatorAssignmentStatus.TRAVELING,
            OperatorAssignmentStatus.REACHED,
            OperatorAssignmentStatus.IN_PROGRESS,
            OperatorAssignmentStatus.PAUSED));

    boolean existsByBookingIdAndAssignmentStatus(Long bookingId, OperatorAssignmentStatus assignmentStatus);

    /**
     * Checks if any active assignment exists for the booking across any of the specified assignment statuses.
     */
    @Query("""
        SELECT COUNT(a) > 0 FROM OperatorJobAssignment a
        WHERE a.booking.id = :bookingId
          AND a.assignmentStatus IN :statuses
    """)
    boolean existsByBookingIdAndAssignmentStatusIn(
            @Param("bookingId") Long bookingId,
            @Param("statuses") java.util.Collection<OperatorAssignmentStatus> statuses
    );

    Page<OperatorJobAssignment> findByOperatorId(Long operatorId, Pageable pageable);

    Page<OperatorJobAssignment> findByOperatorIdAndAssignmentStatus(Long operatorId, OperatorAssignmentStatus assignmentStatus, Pageable pageable);

    Optional<OperatorJobAssignment> findByIdAndOperatorId(Long id, Long operatorId);

    List<OperatorJobAssignment> findByOperatorIdAndAssignmentStatus(Long operatorId, OperatorAssignmentStatus assignmentStatus);

    /**
     * Checks if the operator has any conflicting active assignment for overlapping booking dates.
     * Considers all active assignment statuses (ASSIGNED, ACCEPTED, TRAVELING, REACHED, IN_PROGRESS, PAUSED)
     * and excludes terminal booking statuses (CANCELLED, REJECTED, COMPLETED).
     * Excludes the specified booking ID to allow reassignment scenarios if applicable.
     */
    @Query("""
        SELECT COUNT(a) > 0 FROM OperatorJobAssignment a
        JOIN a.booking b
        WHERE a.operator.id = :operatorId
          AND a.assignmentStatus IN :activeStatuses
          AND (:excludeBookingId IS NULL OR b.id != :excludeBookingId)
          AND b.status NOT IN (
              com.agrorental.booking.entity.BookingStatus.CANCELLED,
              com.agrorental.booking.entity.BookingStatus.REJECTED,
              com.agrorental.booking.entity.BookingStatus.COMPLETED
          )
          AND (b.startDate <= :endDate AND b.endDate >= :startDate)
    """)
    boolean existsActiveOverlappingAssignment(
            @Param("operatorId") Long operatorId,
            @Param("activeStatuses") java.util.Collection<OperatorAssignmentStatus> activeStatuses,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludeBookingId") Long excludeBookingId
    );

    /**
     * Checks if the operator has any conflicting assignment for overlapping booking dates matching a single status.
     */
    @Query("""
        SELECT COUNT(a) > 0 FROM OperatorJobAssignment a
        JOIN a.booking b
        WHERE a.operator.id = :operatorId
          AND a.assignmentStatus = :status
          AND b.status NOT IN (
              com.agrorental.booking.entity.BookingStatus.CANCELLED,
              com.agrorental.booking.entity.BookingStatus.REJECTED,
              com.agrorental.booking.entity.BookingStatus.COMPLETED
          )
          AND (b.startDate <= :endDate AND b.endDate >= :startDate)
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

    // ==========================================
    // PHASE 10: JOB HISTORY QUERIES
    // ==========================================

    /**
     * Retrieves historical job assignments for an operator with optional filtering by status, date range, equipment category, and search query.
     */
    @Query("""
        SELECT a
        FROM OperatorJobAssignment a
        JOIN a.booking b
        JOIN b.equipment e
        WHERE a.operator.id = :operatorId
          AND (:status IS NULL OR a.assignmentStatus = :status)
          AND (:startDate IS NULL OR b.endDate >= :startDate)
          AND (:endDate IS NULL OR b.startDate <= :endDate)
          AND (:equipmentCategory IS NULL OR LOWER(e.category) = LOWER(:equipmentCategory))
          AND (
              :search IS NULL OR :search = ''
              OR LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%'))
              OR LOWER(e.model) LIKE LOWER(CONCAT('%', :search, '%'))
              OR LOWER(b.deliveryAddress) LIKE LOWER(CONCAT('%', :search, '%'))
              OR CONCAT(b.id, '') LIKE CONCAT('%', :search, '%')
              OR CONCAT(a.id, '') LIKE CONCAT('%', :search, '%')
          )
    """)
    Page<OperatorJobAssignment> findJobHistory(
            @Param("operatorId") Long operatorId,
            @Param("status") OperatorAssignmentStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("equipmentCategory") String equipmentCategory,
            @Param("search") String search,
            Pageable pageable
    );

    /**
     * Retrieves all historical job assignments for an operator with optional filtering for summary calculation.
     */
    @Query("""
        SELECT a
        FROM OperatorJobAssignment a
        JOIN a.booking b
        JOIN b.equipment e
        WHERE a.operator.id = :operatorId
          AND (:startDate IS NULL OR b.endDate >= :startDate)
          AND (:endDate IS NULL OR b.startDate <= :endDate)
          AND (:equipmentCategory IS NULL OR LOWER(e.category) = LOWER(:equipmentCategory))
    """)
    List<OperatorJobAssignment> findJobHistoryForSummary(
            @Param("operatorId") Long operatorId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("equipmentCategory") String equipmentCategory
    );
}
