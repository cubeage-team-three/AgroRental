package com.agrorental.operator.service;

import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.booking.repository.BookingRepository;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.ForbiddenException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.notification.service.NotificationService;
import com.agrorental.operator.dto.EligibleOperatorResponse;
import com.agrorental.operator.dto.OperatorAssignedJobResponse;
import com.agrorental.operator.dto.OperatorAssignmentRequest;
import com.agrorental.operator.dto.OperatorAssignmentResponse;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorJobAssignment;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.enums.OperatorAssignmentStatus;
import com.agrorental.operator.mapper.OperatorJobAssignmentMapper;
import com.agrorental.operator.repository.OperatorJobAssignmentRepository;
import com.agrorental.operator.repository.OperatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service managing Operator Job Assignments, eligibility verification, schedule conflict detection,
 * and authenticated operator task access.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperatorAssignmentService {

    private final BookingRepository bookingRepository;
    private final OperatorRepository operatorRepository;
    private final OperatorJobAssignmentRepository assignmentRepository;
    private final OperatorJobAssignmentMapper assignmentMapper;
    private final NotificationService notificationService;

    /**
     * Assigns an eligible approved Operator to a confirmed Booking.
     *
     * @param bookingId Target confirmed booking ID
     * @param request Validated assignment request containing operatorId and optional notes
     * @param assignedBy Identity/username of the administrative or partner user performing assignment
     * @return Populated OperatorAssignmentResponse DTO
     */
    @Transactional
    public OperatorAssignmentResponse assignOperator(Long bookingId, OperatorAssignmentRequest request, String assignedBy) {
        log.info("Processing operator assignment for booking ID {} with operator ID {} by {}",
                bookingId, request.getOperatorId(), assignedBy);

        // 1. Validate Booking existence
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

        // 2. Validate Booking assignable state
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            log.warn("Assignment rejected: Booking {} has status {}, only CONFIRMED bookings can be assigned",
                    bookingId, booking.getStatus());
            throw new BadRequestException("Booking with status " + booking.getStatus() + " is not eligible for operator assignment");
        }

        // 3. Validate Booking does not already have an active assignment
        if (assignmentRepository.existsByBookingIdAndAssignmentStatus(bookingId, OperatorAssignmentStatus.ASSIGNED)) {
            log.warn("Assignment rejected: Booking {} already has an active operator assignment", bookingId);
            throw new BadRequestException("Booking is already assigned to an active operator");
        }

        // 4. Validate Operator existence
        Operator operator = operatorRepository.findById(request.getOperatorId())
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found with ID: " + request.getOperatorId()));

        // 5. Validate Operator eligibility
        if (!operator.isActive()) {
            log.warn("Assignment rejected: Operator {} is inactive", operator.getId());
            throw new ForbiddenException("Operator account is inactive");
        }

        if (operator.getStatus() != OperatorStatus.APPROVED) {
            log.warn("Assignment rejected: Operator {} status is {}", operator.getId(), operator.getStatus());
            throw new ForbiddenException("Operator is not approved");
        }

        if (!operator.isMobileVerified()) {
            log.warn("Assignment rejected: Operator {} mobile is unverified", operator.getId());
            throw new ForbiddenException("Operator mobile number is not verified");
        }

        // 6. Validate schedule conflict / overlapping active assignments
        if (booking.getStartDate() != null && booking.getEndDate() != null) {
            boolean hasConflict = assignmentRepository.existsOverlappingAssignment(
                    operator.getId(),
                    OperatorAssignmentStatus.ASSIGNED,
                    booking.getStartDate(),
                    booking.getEndDate()
            );

            if (hasConflict) {
                log.warn("Assignment rejected: Operator {} has overlapping assignment for {} to {}",
                        operator.getId(), booking.getStartDate(), booking.getEndDate());
                throw new BadRequestException("Operator already has an active assignment for overlapping dates (" +
                        booking.getStartDate() + " to " + booking.getEndDate() + ")");
            }
        }

        // 7. Create and persist OperatorJobAssignment
        OperatorJobAssignment assignment = OperatorJobAssignment.builder()
                .operator(operator)
                .booking(booking)
                .assignmentStatus(OperatorAssignmentStatus.ASSIGNED)
                .assignedAt(LocalDateTime.now())
                .assignedBy(assignedBy != null ? assignedBy : "SYSTEM")
                .notes(request.getNotes() != null ? request.getNotes().trim() : null)
                .build();

        OperatorJobAssignment savedAssignment = assignmentRepository.save(assignment);

        // 8. Synchronize booking relationship
        booking.setOperator(operator);
        bookingRepository.save(booking);

        // 9. Dispatch notifications
        try {
            String equipName = booking.getEquipment() != null ? booking.getEquipment().getName() : "Machinery";
            notificationService.sendNotification(
                    "OPERATOR",
                    operator.getId(),
                    "New Job Assignment",
                    "You have been assigned to job #" + booking.getId() + " (" + equipName + ").",
                    "OPERATOR_ASSIGNED",
                    booking.getId()
            );
            notificationService.sendNotification(
                    "FARMER",
                    booking.getFarmerId(),
                    "Operator Assigned",
                    "Operator " + operator.getFullName() + " has been assigned to your booking #" + booking.getId() + ".",
                    "OPERATOR_ASSIGNED",
                    booking.getId()
            );
        } catch (Exception e) {
            log.warn("Notification dispatch failed for assignment {}: {}", savedAssignment.getId(), e.getMessage());
        }

        log.info("Successfully created assignment ID {} for operator ID {} on booking ID {}",
                savedAssignment.getId(), operator.getId(), bookingId);

        return assignmentMapper.toAssignmentResponse(savedAssignment);
    }

    /**
     * Retrieves a paginated list of assigned jobs for an authenticated Operator.
     *
     * @param operatorId Authenticated Operator ID
     * @param pageable Pagination and sorting criteria
     * @return Page of OperatorAssignedJobResponse DTOs
     */
    @Transactional(readOnly = true)
    public Page<OperatorAssignedJobResponse> getAssignedJobs(Long operatorId, Pageable pageable) {
        log.info("Fetching assigned jobs for operator ID: {}", operatorId);

        Operator operator = operatorRepository.findById(operatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found with ID: " + operatorId));

        if (!operator.isActive()) {
            throw new ForbiddenException("Operator account is inactive");
        }

        Page<OperatorJobAssignment> assignments = assignmentRepository.findByOperatorId(operatorId, pageable);
        return assignments.map(assignmentMapper::toAssignedJobResponse);
    }

    /**
     * Retrieves specific job assignment details for an authenticated Operator.
     * Enforces IDOR security by verifying the assignment belongs to the requesting operator.
     *
     * @param operatorId Authenticated Operator ID
     * @param assignmentId Target assignment ID
     * @return Populated OperatorAssignedJobResponse DTO
     */
    @Transactional(readOnly = true)
    public OperatorAssignedJobResponse getAssignedJob(Long operatorId, Long assignmentId) {
        log.info("Fetching assignment ID {} for operator ID: {}", assignmentId, operatorId);

        OperatorJobAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Job assignment not found with ID: " + assignmentId));

        if (!assignment.getOperator().getId().equals(operatorId)) {
            log.warn("IDOR access blocked: Operator ID {} attempted to access assignment ID {} belonging to operator ID {}",
                    operatorId, assignmentId, assignment.getOperator().getId());
            throw new ForbiddenException("Access denied: You do not have permission to view this assignment");
        }

        return assignmentMapper.toAssignedJobResponse(assignment);
    }

    /**
     * Retrieves the current active assignment for a Booking (Admin/Partner visibility).
     *
     * @param bookingId Booking identifier
     * @return OperatorAssignmentResponse DTO or null if no active assignment
     */
    @Transactional(readOnly = true)
    public OperatorAssignmentResponse getBookingAssignment(Long bookingId) {
        log.info("Fetching operator assignment for booking ID: {}", bookingId);

        return assignmentRepository.findByBookingIdAndAssignmentStatus(bookingId, OperatorAssignmentStatus.ASSIGNED)
                .map(assignmentMapper::toAssignmentResponse)
                .orElse(null);
    }

    /**
     * Searches and lists eligible, approved operators for Partner/Admin assignment.
     *
     * @param search Optional search query (name, skills, mobile, location)
     * @param pageable Pagination criteria
     * @return Page of EligibleOperatorResponse DTOs
     */
    @Transactional(readOnly = true)
    public Page<EligibleOperatorResponse> findEligibleOperators(String search, Pageable pageable) {
        log.info("Searching eligible operators with query: '{}'", search);

        Page<Operator> operators;
        if (search != null && !search.trim().isEmpty()) {
            operators = operatorRepository.searchEligibleOperators(
                    OperatorStatus.APPROVED,
                    true,
                    true,
                    search.trim(),
                    pageable
            );
        } else {
            operators = operatorRepository.findByStatusAndActiveAndMobileVerified(
                    OperatorStatus.APPROVED,
                    true,
                    true,
                    pageable
            );
        }

        return operators.map(assignmentMapper::toEligibleResponse);
    }
}
