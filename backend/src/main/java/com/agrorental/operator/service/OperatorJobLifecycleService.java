package com.agrorental.operator.service;

import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.booking.repository.BookingRepository;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.ForbiddenException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.notification.service.NotificationService;
import com.agrorental.operator.dto.OperatorAssignedJobResponse;
import com.agrorental.operator.dto.OperatorJobCompletionRequest;
import com.agrorental.operator.dto.OperatorJobPauseRequest;
import com.agrorental.operator.dto.OperatorJobRejectionRequest;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorJobAssignment;
import com.agrorental.operator.entity.OperatorJobPauseInterval;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.enums.OperatorAssignmentStatus;
import com.agrorental.operator.mapper.OperatorJobAssignmentMapper;
import com.agrorental.operator.repository.OperatorJobAssignmentRepository;
import com.agrorental.operator.repository.OperatorJobPauseIntervalRepository;
import com.agrorental.operator.repository.OperatorLocationRepository;
import com.agrorental.operator.repository.OperatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Service managing the Operator Active Work Lifecycle state machine transitions and audit trails.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperatorJobLifecycleService {

    /** Booking states in which the assigned operator's job may still move (and drive the booking). */
    private static final Set<BookingStatus> OPERATOR_PHASE_BOOKING_STATUSES =
            EnumSet.of(BookingStatus.OPERATOR_ASSIGNED, BookingStatus.ON_THE_WAY, BookingStatus.WORK_STARTED);

    /** Notification messages are stored in a VARCHAR(500) column; free-text reasons are quoted shorter. */
    static final int MAX_NOTIFICATION_MESSAGE = 500;
    static final int MAX_QUOTED_REASON = 300;

    /** Order of the operator-driven booking states; a job step only ever moves its booking forward. */
    private static final List<BookingStatus> BOOKING_PROGRESS = List.of(
            BookingStatus.OPERATOR_ASSIGNED, BookingStatus.ON_THE_WAY, BookingStatus.WORK_STARTED, BookingStatus.COMPLETED);

    private final OperatorRepository operatorRepository;
    private final OperatorJobAssignmentRepository assignmentRepository;
    private final BookingRepository bookingRepository;
    private final OperatorJobAssignmentMapper assignmentMapper;
    private final NotificationService notificationService;
    private final OperatorLocationRepository locationRepository;
    private final OperatorJobPauseIntervalRepository pauseIntervalRepository;

    private com.agrorental.equipment.service.EquipmentAvailabilityService equipmentAvailabilityService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setEquipmentAvailabilityService(com.agrorental.equipment.service.EquipmentAvailabilityService equipmentAvailabilityService) {
        this.equipmentAvailabilityService = equipmentAvailabilityService;
    }

    /**
     * Validates that the requested status transition is permitted by the state machine.
     *
     * @param current Current status of the assignment
     * @param target Target status requested
     */
    public void validateTransition(OperatorAssignmentStatus current, OperatorAssignmentStatus target) {
        boolean valid = switch (current) {
            case ASSIGNED -> target == OperatorAssignmentStatus.ACCEPTED || target == OperatorAssignmentStatus.REJECTED;
            case ACCEPTED -> target == OperatorAssignmentStatus.TRAVELING;
            case TRAVELING -> target == OperatorAssignmentStatus.REACHED;
            case REACHED -> target == OperatorAssignmentStatus.IN_PROGRESS;
            case IN_PROGRESS -> target == OperatorAssignmentStatus.PAUSED || target == OperatorAssignmentStatus.COMPLETED;
            case PAUSED -> target == OperatorAssignmentStatus.IN_PROGRESS;
            case REJECTED, COMPLETED, CANCELLED -> false;
        };

        if (!valid) {
            log.warn("Illegal lifecycle transition attempted: {} -> {}", current, target);
            throw new BadRequestException(
                    String.format("Invalid job status transition from %s to %s", current, target)
            );
        }
    }

    /**
     * Validates operator account standing, loads assignment, and enforces IDOR ownership.
     */
    private OperatorJobAssignment validateAndGetAssignment(Long assignmentId, Long operatorId) {
        Operator operator = operatorRepository.findById(operatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found with ID: " + operatorId));

        if (!operator.isActive()) {
            log.warn("Lifecycle action rejected: Operator {} is inactive", operatorId);
            throw new ForbiddenException("Operator account is inactive");
        }

        if (operator.getStatus() != OperatorStatus.APPROVED) {
            log.warn("Lifecycle action rejected: Operator {} status is {}", operatorId, operator.getStatus());
            throw new ForbiddenException("Operator is not approved");
        }
        // Contract v1.1: mobileVerified is informational only, not a lifecycle gate

        OperatorJobAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Job assignment not found with ID: " + assignmentId));

        if (!assignment.getOperator().getId().equals(operatorId)) {
            log.warn("IDOR violation: Operator {} attempted to mutate assignment {} belonging to operator {}",
                    operatorId, assignmentId, assignment.getOperator().getId());
            throw new ForbiddenException("Access denied: You do not have permission to modify this assignment");
        }

        return assignment;
    }

    /**
     * Takes the booking row lock before anything is written. Every other writer of a booking (farmer cancellation,
     * admin status changes, operator assignment) locks the booking row first and then touches the assignment;
     * locking in the same order here keeps one lock order (booking, then assignment) across modules. Without it a
     * cancellation racing an operator step deadlocks and one side fails with a 500. The locking read also loads
     * the booking's latest committed state for {@link #requireBookingOpenForJob}.
     */
    private void lockBooking(OperatorJobAssignment assignment) {
        if (assignment.getBooking() != null && assignment.getBooking().getId() != null) {
            bookingRepository.findByIdWithLock(assignment.getBooking().getId());
        }
    }

    /**
     * A job step is allowed only while its booking is still in the operator phase and still assigned to this
     * job's operator. A booking completed, cancelled, rejected, re-confirmed or re-pointed elsewhere (for example by
     * an administrator's status override) closes the job: its steps must never move that booking again.
     */
    private void requireBookingOpenForJob(OperatorJobAssignment assignment) {
        Booking booking = assignment.getBooking();
        if (booking == null) {
            throw new BadRequestException("This job has no booking and can no longer be updated.");
        }
        if (!OPERATOR_PHASE_BOOKING_STATUSES.contains(booking.getStatus())) {
            log.warn("Lifecycle action rejected: assignment {} belongs to booking {} in status {}",
                    assignment.getId(), booking.getId(), booking.getStatus());
            throw new BadRequestException("Booking #" + booking.getId() + " is " + booking.getStatus()
                    + "; this job can no longer be updated.");
        }
        if (booking.getOperator() == null || !booking.getOperator().getId().equals(assignment.getOperator().getId())) {
            log.warn("Lifecycle action rejected: booking {} is no longer assigned to operator {} (assignment {})",
                    booking.getId(), assignment.getOperator().getId(), assignment.getId());
            throw new BadRequestException("Booking #" + booking.getId()
                    + " is no longer assigned to you; this job can no longer be updated.");
        }
    }

    /** Moves the booking to {@code target} only if that is forward progress; never moves it backwards. */
    private void advanceBooking(Booking booking, BookingStatus target) {
        if (BOOKING_PROGRESS.indexOf(target) > BOOKING_PROGRESS.indexOf(booking.getStatus())) {
            booking.setStatus(target);
        }
    }

    /**
     * Accepts an assigned job (ASSIGNED -> ACCEPTED).
     */
    @Transactional
    public OperatorAssignedJobResponse acceptJob(Long assignmentId, Long operatorId) {
        log.info("Operator {} accepting job assignment {}", operatorId, assignmentId);
        OperatorJobAssignment assignment = validateAndGetAssignment(assignmentId, operatorId);
        lockBooking(assignment);

        validateTransition(assignment.getAssignmentStatus(), OperatorAssignmentStatus.ACCEPTED);
        requireBookingOpenForJob(assignment);

        assignment.setAssignmentStatus(OperatorAssignmentStatus.ACCEPTED);
        assignment.setAcceptedAt(LocalDateTime.now());

        OperatorJobAssignment saved = assignmentRepository.save(assignment);
        notifyStakeholders(saved, "Job Accepted", "Operator " + saved.getOperator().getFullName() + " has accepted job #" + saved.getBooking().getId());

        log.info("Successfully accepted assignment ID {}", assignmentId);
        return assignmentMapper.toAssignedJobResponse(saved);
    }

    /**
     * Rejects an assigned job with mandatory reason (ASSIGNED -> REJECTED).
     */
    @Transactional
    public OperatorAssignedJobResponse rejectJob(Long assignmentId, Long operatorId, OperatorJobRejectionRequest request) {
        log.info("Operator {} rejecting job assignment {}", operatorId, assignmentId);
        OperatorJobAssignment assignment = validateAndGetAssignment(assignmentId, operatorId);
        lockBooking(assignment);

        validateTransition(assignment.getAssignmentStatus(), OperatorAssignmentStatus.REJECTED);
        requireBookingOpenForJob(assignment);

        if (request == null || request.getRejectionReason() == null || request.getRejectionReason().trim().length() < 3) {
            throw new BadRequestException("Rejection reason is required and must be at least 3 characters");
        }

        assignment.setAssignmentStatus(OperatorAssignmentStatus.REJECTED);
        assignment.setRejectedAt(LocalDateTime.now());
        assignment.setRejectionReason(request.getRejectionReason().trim());

        locationRepository.deactivateTrackingForAssignment(assignment.getId());

        if (assignment.getBooking() != null && bookingRepository != null) {
            Booking booking = assignment.getBooking();
            booking.setOperator(null);
            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
        }

        OperatorJobAssignment saved = assignmentRepository.save(assignment);
        notifyStakeholders(saved, "Job Declined", "Operator " + saved.getOperator().getFullName() + " declined job #" + saved.getBooking().getId() + ". Reason: " + quoteReason(saved.getRejectionReason()));

        log.info("Successfully rejected assignment ID {}", assignmentId);
        return assignmentMapper.toAssignedJobResponse(saved);
    }

    /**
     * Starts traveling to the service location (ACCEPTED -> TRAVELING).
     */
    @Transactional
    public OperatorAssignedJobResponse startTravel(Long assignmentId, Long operatorId) {
        log.info("Operator {} starting travel for assignment {}", operatorId, assignmentId);
        OperatorJobAssignment assignment = validateAndGetAssignment(assignmentId, operatorId);
        lockBooking(assignment);

        validateTransition(assignment.getAssignmentStatus(), OperatorAssignmentStatus.TRAVELING);
        requireBookingOpenForJob(assignment);

        assignment.setAssignmentStatus(OperatorAssignmentStatus.TRAVELING);
        assignment.setTravelingAt(LocalDateTime.now());
        advanceBooking(assignment.getBooking(), BookingStatus.ON_THE_WAY);
        bookingRepository.save(assignment.getBooking());

        OperatorJobAssignment saved = assignmentRepository.save(assignment);
        notifyStakeholders(saved, "Operator En Route", "Operator " + saved.getOperator().getFullName() + " is traveling to the farm location for job #" + saved.getBooking().getId());

        log.info("Successfully started travel for assignment ID {}", assignmentId);
        return assignmentMapper.toAssignedJobResponse(saved);
    }

    /**
     * Marks arrival at the service location (TRAVELING -> REACHED).
     */
    @Transactional
    public OperatorAssignedJobResponse markReached(Long assignmentId, Long operatorId) {
        log.info("Operator {} marked reached for assignment {}", operatorId, assignmentId);
        OperatorJobAssignment assignment = validateAndGetAssignment(assignmentId, operatorId);
        lockBooking(assignment);

        validateTransition(assignment.getAssignmentStatus(), OperatorAssignmentStatus.REACHED);
        requireBookingOpenForJob(assignment);

        assignment.setAssignmentStatus(OperatorAssignmentStatus.REACHED);
        assignment.setReachedAt(LocalDateTime.now());

        OperatorJobAssignment saved = assignmentRepository.save(assignment);
        notifyStakeholders(saved, "Operator Arrived", "Operator " + saved.getOperator().getFullName() + " has arrived at the farm location for job #" + saved.getBooking().getId());

        log.info("Successfully marked reached for assignment ID {}", assignmentId);
        return assignmentMapper.toAssignedJobResponse(saved);
    }

    /**
     * Starts field work / machinery operations (REACHED -> IN_PROGRESS).
     */
    @Transactional
    public OperatorAssignedJobResponse startWork(Long assignmentId, Long operatorId) {
        log.info("Operator {} starting work for assignment {}", operatorId, assignmentId);
        OperatorJobAssignment assignment = validateAndGetAssignment(assignmentId, operatorId);
        lockBooking(assignment);

        validateTransition(assignment.getAssignmentStatus(), OperatorAssignmentStatus.IN_PROGRESS);
        requireBookingOpenForJob(assignment);

        assignment.setAssignmentStatus(OperatorAssignmentStatus.IN_PROGRESS);
        assignment.setWorkStartedAt(LocalDateTime.now());
        advanceBooking(assignment.getBooking(), BookingStatus.WORK_STARTED);
        bookingRepository.save(assignment.getBooking());

        OperatorJobAssignment saved = assignmentRepository.save(assignment);
        notifyStakeholders(saved, "Work In Progress", "Machinery operations started for job #" + saved.getBooking().getId());

        log.info("Successfully started work for assignment ID {}", assignmentId);
        return assignmentMapper.toAssignedJobResponse(saved);
    }

    /**
     * Pauses field work with mandatory reason (IN_PROGRESS -> PAUSED).
     */
    @Transactional
    public OperatorAssignedJobResponse pauseWork(Long assignmentId, Long operatorId, OperatorJobPauseRequest request) {
        log.info("Operator {} pausing work for assignment {}", operatorId, assignmentId);
        OperatorJobAssignment assignment = validateAndGetAssignment(assignmentId, operatorId);
        lockBooking(assignment);

        validateTransition(assignment.getAssignmentStatus(), OperatorAssignmentStatus.PAUSED);
        requireBookingOpenForJob(assignment);

        if (request == null || request.getPauseReason() == null || request.getPauseReason().trim().length() < 3) {
            throw new BadRequestException("Pause reason is required and must be at least 3 characters");
        }

        LocalDateTime now = LocalDateTime.now();
        assignment.setAssignmentStatus(OperatorAssignmentStatus.PAUSED);
        assignment.setPausedAt(now);
        assignment.setPauseReason(request.getPauseReason().trim());

        OperatorJobPauseInterval interval = OperatorJobPauseInterval.builder()
                .assignment(assignment)
                .operator(assignment.getOperator())
                .pausedAt(now)
                .pauseReason(request.getPauseReason().trim())
                .build();
        pauseIntervalRepository.save(interval);

        OperatorJobAssignment saved = assignmentRepository.save(assignment);
        notifyStakeholders(saved, "Work Paused", "Operations temporarily paused for job #" + saved.getBooking().getId() + ". Reason: " + quoteReason(saved.getPauseReason()));

        log.info("Successfully paused work for assignment ID {}", assignmentId);
        return assignmentMapper.toAssignedJobResponse(saved);
    }

    /**
     * Resumes field work (PAUSED -> IN_PROGRESS).
     */
    @Transactional
    public OperatorAssignedJobResponse resumeWork(Long assignmentId, Long operatorId) {
        log.info("Operator {} resuming work for assignment {}", operatorId, assignmentId);
        OperatorJobAssignment assignment = validateAndGetAssignment(assignmentId, operatorId);
        lockBooking(assignment);

        validateTransition(assignment.getAssignmentStatus(), OperatorAssignmentStatus.IN_PROGRESS);
        requireBookingOpenForJob(assignment);

        LocalDateTime now = LocalDateTime.now();
        assignment.setAssignmentStatus(OperatorAssignmentStatus.IN_PROGRESS);
        assignment.setResumedAt(now);

        pauseIntervalRepository.findTopByAssignmentIdAndResumedAtIsNullOrderByPausedAtDesc(assignmentId)
                .ifPresent(interval -> {
                    interval.setResumedAt(now);
                    long duration = Duration.between(interval.getPausedAt(), now).toMinutes();
                    interval.setDurationMinutes(Math.max(0, duration));
                    pauseIntervalRepository.save(interval);
                });

        OperatorJobAssignment saved = assignmentRepository.save(assignment);
        notifyStakeholders(saved, "Work Resumed", "Machinery operations resumed for job #" + saved.getBooking().getId());

        log.info("Successfully resumed work for assignment ID {}", assignmentId);
        return assignmentMapper.toAssignedJobResponse(saved);
    }

    /**
     * Marks field work completed (IN_PROGRESS -> COMPLETED).
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OperatorAssignedJobResponse completeWork(Long assignmentId, Long operatorId, OperatorJobCompletionRequest request) {
        log.info("Operator {} completing work for assignment {}", operatorId, assignmentId);
        OperatorJobAssignment assignment = validateAndGetAssignment(assignmentId, operatorId);
        lockBooking(assignment);

        validateTransition(assignment.getAssignmentStatus(), OperatorAssignmentStatus.COMPLETED);
        requireBookingOpenForJob(assignment);

        LocalDateTime now = LocalDateTime.now();
        assignment.setAssignmentStatus(OperatorAssignmentStatus.COMPLETED);
        assignment.setCompletedAt(now);
        if (request != null && request.getCompletionNotes() != null) {
            assignment.setCompletionNotes(request.getCompletionNotes().trim());
        }

        if (assignment.getBooking() != null) {
            advanceBooking(assignment.getBooking(), BookingStatus.COMPLETED);
            // The equipment flag is recomputed from all reserving bookings (single owner: EquipmentAvailabilityService);
            // the booking row is already locked, so the lock order stays booking -> equipment
            if (equipmentAvailabilityService != null) {   // null only in plain unit tests
                equipmentAvailabilityService.refreshAfterBookingChange(assignment.getBooking());
            }
            bookingRepository.save(assignment.getBooking());
        }

        // Close any dangling open pause interval
        pauseIntervalRepository.findTopByAssignmentIdAndResumedAtIsNullOrderByPausedAtDesc(assignmentId)
                .ifPresent(interval -> {
                    interval.setResumedAt(now);
                    long duration = Duration.between(interval.getPausedAt(), now).toMinutes();
                    interval.setDurationMinutes(Math.max(0, duration));
                    pauseIntervalRepository.save(interval);
                });

        locationRepository.deactivateTrackingForAssignment(assignment.getId());

        OperatorJobAssignment saved = assignmentRepository.save(assignment);
        notifyStakeholders(saved, "Job Completed", "Machinery operations completed successfully for job #" + saved.getBooking().getId());

        log.info("Successfully completed work for assignment ID {}", assignmentId);
        return assignmentMapper.toAssignedJobResponse(saved);
    }

    /**
     * Helper to dispatch notifications without failing the primary transaction.
     */
    private void notifyStakeholders(OperatorJobAssignment assignment, String title, String rawMessage) {
        // The notification message column holds at most 500 characters. An over-long INSERT would mark this
        // transaction rollback-only (the catch below cannot save it) and fail the whole job step.
        String message = truncate(rawMessage, MAX_NOTIFICATION_MESSAGE);
        try {
            Booking booking = assignment.getBooking();
            if (booking != null && booking.getFarmerId() != null) {
                notificationService.sendNotification("FARMER", booking.getFarmerId(), title, message, "OPERATOR_LIFECYCLE", booking.getId());
            }
            if (booking != null && booking.getPartner() != null && booking.getPartner().getId() != null) {
                notificationService.sendNotification("PARTNER", booking.getPartner().getId(), title, message, "OPERATOR_LIFECYCLE", booking.getId());
            }
        } catch (Exception e) {
            log.warn("Notification dispatch failed for assignment {}: {}", assignment.getId(), e.getMessage());
        }
    }

    /** A free-text reason as quoted in notifications: trimmed and at most {@link #MAX_QUOTED_REASON} characters. */
    static String quoteReason(String reason) {
        return reason == null ? "" : truncate(reason.trim(), MAX_QUOTED_REASON);
    }

    /** Cuts {@code text} to at most {@code max} UTF-16 units (ending in "..."), never splitting a surrogate pair. */
    static String truncate(String text, int max) {
        if (text == null || text.length() <= max) {
            return text;
        }
        int end = max - 3;
        if (Character.isHighSurrogate(text.charAt(end - 1))) {
            end--;
        }
        return text.substring(0, end) + "...";
    }
}
