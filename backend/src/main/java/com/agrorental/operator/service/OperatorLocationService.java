package com.agrorental.operator.service;

import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.ForbiddenException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.operator.dto.OperatorLocationResponse;
import com.agrorental.operator.dto.OperatorLocationUpdateRequest;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorJobAssignment;
import com.agrorental.operator.entity.OperatorLocation;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.enums.OperatorAssignmentStatus;
import com.agrorental.operator.repository.OperatorJobAssignmentRepository;
import com.agrorental.operator.repository.OperatorLocationRepository;
import com.agrorental.operator.repository.OperatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Service orchestrating Operator GPS Location Tracking lifecycle: start, update coordinates,
 * retrieve latest location, and stop tracking with strict IDOR defense and active state enforcement.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperatorLocationService {

    private final OperatorLocationRepository locationRepository;
    private final OperatorJobAssignmentRepository assignmentRepository;
    private final OperatorRepository operatorRepository;

    private static final Set<OperatorAssignmentStatus> TRACKABLE_STATUSES = EnumSet.of(
            OperatorAssignmentStatus.TRAVELING,
            OperatorAssignmentStatus.REACHED,
            OperatorAssignmentStatus.IN_PROGRESS,
            OperatorAssignmentStatus.PAUSED
    );

    /**
     * Starts GPS location tracking for an active job assignment.
     */
    @Transactional
    public OperatorLocationResponse startTracking(Long assignmentId, Long operatorId) {
        log.info("Starting GPS tracking for assignment {} by operator {}", assignmentId, operatorId);
        Operator operator = validateActiveApprovedOperator(operatorId);
        OperatorJobAssignment assignment = validateAssignmentOwnership(assignmentId, operator);

        if (!TRACKABLE_STATUSES.contains(assignment.getAssignmentStatus())) {
            throw new BadRequestException("Location tracking cannot be started for assignment in status: "
                    + assignment.getAssignmentStatus() + ". Valid active statuses are: TRAVELING, REACHED, IN_PROGRESS, PAUSED.");
        }

        // Deactivate previous stale records if any
        locationRepository.deactivateTrackingForAssignment(assignmentId);

        // Never fabricate a position: farmers see the latest location row as the operator's live position.
        // Resume on the latest real fix (keeping its real timestamp, so a stale point is not shown as fresh);
        // without any fix yet, report tracking as started with no coordinates until the first GPS update.
        Optional<OperatorLocation> latestOpt = locationRepository
                .findTopByAssignmentIdOrderByRecordedAtDesc(assignmentId);

        if (latestOpt.isPresent()) {
            OperatorLocation latest = latestOpt.get();
            latest.setTrackingActive(true);
            return mapToResponse(locationRepository.save(latest));
        }

        return OperatorLocationResponse.builder()
                .assignmentId(assignment.getId())
                .operatorId(operator.getId())
                .trackingActive(true)
                .build();
    }

    /**
     * Records an incoming GPS coordinate update for the assignment.
     */
    @Transactional
    public OperatorLocationResponse updateLocation(Long assignmentId, Long operatorId, OperatorLocationUpdateRequest request) {
        log.info("Received GPS location update for assignment {} by operator {}: ({}, {})",
                assignmentId, operatorId, request.getLatitude(), request.getLongitude());

        Operator operator = validateActiveApprovedOperator(operatorId);
        OperatorJobAssignment assignment = validateAssignmentOwnership(assignmentId, operator);

        if (!TRACKABLE_STATUSES.contains(assignment.getAssignmentStatus())) {
            throw new BadRequestException("GPS updates rejected. Assignment is in terminal or inactive status: "
                    + assignment.getAssignmentStatus());
        }

        validateCoordinates(request);

        // Deactivate previous active markers
        locationRepository.deactivateTrackingForAssignment(assignmentId);

        OperatorLocation location = OperatorLocation.builder()
                .assignment(assignment)
                .operator(operator)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .accuracy(request.getAccuracy())
                .speed(request.getSpeed())
                .heading(request.getHeading())
                .recordedAt(LocalDateTime.now())
                .trackingActive(true)
                .build();

        OperatorLocation saved = locationRepository.save(location);
        return mapToResponse(saved);
    }

    /**
     * Retrieves the latest recorded GPS location for the authenticated operator's assignment.
     */
    @Transactional(readOnly = true)
    public OperatorLocationResponse getLatestLocation(Long assignmentId, Long operatorId) {
        log.info("Fetching latest GPS location for assignment {} by operator {}", assignmentId, operatorId);
        Operator operator = validateActiveApprovedOperator(operatorId);
        validateAssignmentOwnership(assignmentId, operator);

        OperatorLocation latest = locationRepository
                .findTopByAssignmentIdOrderByRecordedAtDesc(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("No GPS location has been recorded for assignment " + assignmentId));

        return mapToResponse(latest);
    }

    /**
     * Stops GPS location tracking for the active assignment.
     */
    @Transactional
    public OperatorLocationResponse stopTracking(Long assignmentId, Long operatorId) {
        log.info("Stopping GPS tracking for assignment {} by operator {}", assignmentId, operatorId);
        Operator operator = validateActiveApprovedOperator(operatorId);
        validateAssignmentOwnership(assignmentId, operator);

        locationRepository.deactivateTrackingForAssignment(assignmentId);

        Optional<OperatorLocation> latestOpt = locationRepository
                .findTopByAssignmentIdOrderByRecordedAtDesc(assignmentId);

        if (latestOpt.isPresent()) {
            OperatorLocation latest = latestOpt.get();
            latest.setTrackingActive(false);
            return mapToResponse(locationRepository.save(latest));
        }

        return OperatorLocationResponse.builder()
                .assignmentId(assignmentId)
                .operatorId(operatorId)
                .trackingActive(false)
                .recordedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Automatically deactivates tracking when a job reaches a terminal lifecycle status.
     */
    @Transactional
    public void deactivateTrackingOnJobCompletion(Long assignmentId) {
        log.info("Deactivating active GPS tracking for completed/terminal assignment {}", assignmentId);
        locationRepository.deactivateTrackingForAssignment(assignmentId);
    }

    private Operator validateActiveApprovedOperator(Long operatorId) {
        Operator operator = operatorRepository.findById(operatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found with ID: " + operatorId));

        if (!operator.isActive()) {
            throw new ForbiddenException("Operator account is inactive");
        }
        if (operator.getStatus() != OperatorStatus.APPROVED) {
            throw new ForbiddenException("Operator account is not approved. Current status: " + operator.getStatus());
        }
        // Contract v1.1: mobileVerified is informational only, not a tracking gate
        return operator;
    }

    private OperatorJobAssignment validateAssignmentOwnership(Long assignmentId, Operator operator) {
        OperatorJobAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Job assignment not found with ID: " + assignmentId));

        if (!assignment.getOperator().getId().equals(operator.getId())) {
            log.warn("IDOR violation: Operator {} attempted to access location for assignment {} belonging to Operator {}",
                    operator.getId(), assignmentId, assignment.getOperator().getId());
            throw new ForbiddenException("Access denied: You are not assigned to this job");
        }
        return assignment;
    }

    private void validateCoordinates(OperatorLocationUpdateRequest request) {
        if (request.getLatitude() == null || request.getLatitude() < -90.0 || request.getLatitude() > 90.0) {
            throw new BadRequestException("Latitude must be between -90.0 and 90.0");
        }
        if (request.getLongitude() == null || request.getLongitude() < -180.0 || request.getLongitude() > 180.0) {
            throw new BadRequestException("Longitude must be between -180.0 and 180.0");
        }
        if (request.getAccuracy() != null && request.getAccuracy() < 0.0) {
            throw new BadRequestException("Accuracy must be >= 0.0");
        }
        if (request.getSpeed() != null && request.getSpeed() < 0.0) {
            throw new BadRequestException("Speed must be >= 0.0");
        }
        if (request.getHeading() != null && (request.getHeading() < 0.0 || request.getHeading() >= 360.0)) {
            throw new BadRequestException("Heading must be >= 0.0 and < 360.0");
        }
    }

    private OperatorLocationResponse mapToResponse(OperatorLocation entity) {
        return OperatorLocationResponse.builder()
                .id(entity.getId())
                .assignmentId(entity.getAssignment() != null ? entity.getAssignment().getId() : null)
                .operatorId(entity.getOperator() != null ? entity.getOperator().getId() : null)
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .accuracy(entity.getAccuracy())
                .speed(entity.getSpeed())
                .heading(entity.getHeading())
                .trackingActive(entity.isTrackingActive())
                .recordedAt(entity.getRecordedAt())
                .build();
    }
}
