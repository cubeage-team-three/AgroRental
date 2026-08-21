package com.agrorental.operator.service;

import com.agrorental.booking.entity.Booking;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Service managing the Operator Active Work Lifecycle state machine transitions and audit trails.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperatorJobLifecycleService {

    private final OperatorRepository operatorRepository;
    private final OperatorJobAssignmentRepository assignmentRepository;
    private final OperatorJobAssignmentMapper assignmentMapper;
    private final NotificationService notificationService;
    private final OperatorLocationRepository locationRepository;
    private final OperatorJobPauseIntervalRepository pauseIntervalRepository;

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

        if (!operator.isMobileVerified()) {
            log.warn("Lifecycle action rejected: Operator {} mobile is unverified", operatorId);
            throw new ForbiddenException("Operator mobile number is not verified");
        }

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
     * Accepts an assigned job (ASSIGNED -> ACCEPTED).
     */
    @Transactional
    public OperatorAssignedJobResponse acceptJob(Long assignmentId, Long operatorId) {
        log.info("Operator {} accepting job assignment {}", operatorId, assignmentId);
        OperatorJobAssignment assignment = validateAndGetAssignment(assignmentId, operatorId);

        validateTransition(assignment.getAssignmentStatus(), OperatorAssignmentStatus.ACCEPTED);

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

        validateTransition(assignment.getAssignmentStatus(), OperatorAssignmentStatus.REJECTED);

        if (request == null || request.getRejectionReason() == null || request.getRejectionReason().trim().length() < 3) {
            throw new BadRequestException("Rejection reason is required and must be at least 3 characters");
        }

        assignment.setAssignmentStatus(OperatorAssignmentStatus.REJECTED);
        assignment.setRejectedAt(LocalDateTime.now());
        assignment.setRejectionReason(request.getRejectionReason().trim());

        locationRepository.deactivateTrackingForAssignment(assignment.getId());

        OperatorJobAssignment saved = assignmentRepository.save(assignment);
        notifyStakeholders(saved, "Job Declined", "Operator " + saved.getOperator().getFullName() + " declined job #" + saved.getBooking().getId() + ". Reason: " + saved.getRejectionReason());

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

        validateTransition(assignment.getAssignmentStatus(), OperatorAssignmentStatus.TRAVELING);

        assignment.setAssignmentStatus(OperatorAssignmentStatus.TRAVELING);
        assignment.setTravelingAt(LocalDateTime.now());

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

        validateTransition(assignment.getAssignmentStatus(), OperatorAssignmentStatus.REACHED);

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

        validateTransition(assignment.getAssignmentStatus(), OperatorAssignmentStatus.IN_PROGRESS);

        assignment.setAssignmentStatus(OperatorAssignmentStatus.IN_PROGRESS);
        assignment.setWorkStartedAt(LocalDateTime.now());

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

        validateTransition(assignment.getAssignmentStatus(), OperatorAssignmentStatus.PAUSED);

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
        notifyStakeholders(saved, "Work Paused", "Operations temporarily paused for job #" + saved.getBooking().getId() + ". Reason: " + saved.getPauseReason());

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

        validateTransition(assignment.getAssignmentStatus(), OperatorAssignmentStatus.IN_PROGRESS);

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
    @Transactional
    public OperatorAssignedJobResponse completeWork(Long assignmentId, Long operatorId, OperatorJobCompletionRequest request) {
        log.info("Operator {} completing work for assignment {}", operatorId, assignmentId);
        OperatorJobAssignment assignment = validateAndGetAssignment(assignmentId, operatorId);

        validateTransition(assignment.getAssignmentStatus(), OperatorAssignmentStatus.COMPLETED);

        LocalDateTime now = LocalDateTime.now();
        assignment.setAssignmentStatus(OperatorAssignmentStatus.COMPLETED);
        assignment.setCompletedAt(now);
        if (request != null && request.getCompletionNotes() != null) {
            assignment.setCompletionNotes(request.getCompletionNotes().trim());
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
    private void notifyStakeholders(OperatorJobAssignment assignment, String title, String message) {
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
}
