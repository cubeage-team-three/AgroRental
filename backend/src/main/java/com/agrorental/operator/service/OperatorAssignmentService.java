package com.agrorental.operator.service;

import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.booking.repository.BookingRepository;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.ForbiddenException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.notification.service.NotificationService;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.operator.dto.EligibleOperatorResponse;
import com.agrorental.operator.dto.OperatorAssignedJobResponse;
import com.agrorental.operator.dto.OperatorAssignmentRequest;
import com.agrorental.operator.dto.OperatorAssignmentResponse;
import com.agrorental.operator.dto.OperatorJobEarningsResponse;
import com.agrorental.operator.dto.OperatorJobHistoryResponse;
import com.agrorental.operator.dto.OperatorJobHistorySummaryResponse;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorJobAssignment;
import com.agrorental.operator.entity.OperatorLocation;
import com.agrorental.operator.entity.OperatorReview;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.enums.OperatorAssignmentStatus;
import com.agrorental.operator.mapper.OperatorJobAssignmentMapper;
import com.agrorental.operator.repository.OperatorJobAssignmentRepository;
import com.agrorental.operator.repository.OperatorLocationRepository;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.operator.repository.OperatorReviewRepository;
import com.agrorental.partner.entity.Partner;
import com.agrorental.security.principal.CurrentCaller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service managing Operator Job Assignments, eligibility verification, schedule conflict detection,
 * and authenticated operator task access.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperatorAssignmentService {

    public static final java.util.List<OperatorAssignmentStatus> ACTIVE_ASSIGNMENT_STATUSES = java.util.List.of(
            OperatorAssignmentStatus.ASSIGNED,
            OperatorAssignmentStatus.ACCEPTED,
            OperatorAssignmentStatus.TRAVELING,
            OperatorAssignmentStatus.REACHED,
            OperatorAssignmentStatus.IN_PROGRESS,
            OperatorAssignmentStatus.PAUSED
    );

    private final BookingRepository bookingRepository;
    private final OperatorRepository operatorRepository;
    private final OperatorJobAssignmentRepository assignmentRepository;
    private final OperatorJobAssignmentMapper assignmentMapper;
    private final NotificationService notificationService;
    private final OperatorEarningsService earningsService;
    private final OperatorReviewRepository reviewRepository;
    private final OperatorLocationRepository locationRepository;

    /**
     * Assigns an eligible approved Operator to a confirmed Booking.
     *
     * @param bookingId Target confirmed booking ID
     * @param request Validated assignment request containing operatorId and optional notes
     * @param assignedBy Identity/username of the administrative or partner user performing assignment
     * @return Populated OperatorAssignmentResponse DTO
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OperatorAssignmentResponse assignOperator(Long bookingId, OperatorAssignmentRequest request, String assignedBy) {
        return assignOperator(bookingId, request, assignedBy, null);
    }

    /**
     * Assigns an eligible approved Operator to a confirmed Booking with optional Partner ownership validation.
     *
     * @param bookingId Target confirmed booking ID
     * @param request Validated assignment request containing operatorId and optional notes
     * @param assignedBy Identity/username of the administrative or partner user performing assignment
     * @param requestingPartnerId Optional Partner ID to enforce ownership validation
     * @return Populated OperatorAssignmentResponse DTO
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OperatorAssignmentResponse assignOperator(Long bookingId, OperatorAssignmentRequest request, String assignedBy, Long requestingPartnerId) {
        log.info("Processing operator assignment for booking ID {} with operator ID {} by {} (partnerId: {})",
                bookingId, request != null ? request.getOperatorId() : null, assignedBy, requestingPartnerId);

        if (request == null || request.getOperatorId() == null) {
            throw new BadRequestException("Operator ID is required.");
        }

        // 1. Validate Booking existence and acquire pessimistic lock for concurrency control
        Booking booking = bookingRepository.findByIdWithLock(bookingId)
                .or(() -> bookingRepository.findById(bookingId))
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

        // 2. Validate Partner ownership (fail closed: a booking without a partner belongs to no partner)
        if (requestingPartnerId != null
                && (booking.getPartner() == null || !booking.getPartner().getId().equals(requestingPartnerId))) {
            log.warn("Assignment rejected: Booking {} belongs to partner {}, requesting partner was {}",
                    bookingId, booking.getPartner() != null ? booking.getPartner().getId() : null, requestingPartnerId);
            throw new BadRequestException("Unauthorized: Booking does not belong to the specified partner.");
        }

        // 3. Validate Booking assignable state
        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.OPERATOR_ASSIGNED) {
            log.warn("Assignment rejected: Booking {} has status {}, only CONFIRMED or OPERATOR_ASSIGNED bookings can be assigned",
                    bookingId, booking.getStatus());
            throw new BadRequestException("Booking with status " + booking.getStatus() + " is not eligible for operator assignment");
        }

        // 4. Validate Booking does not already have an active assignment across any active state
        if (assignmentRepository.existsByBookingIdAndAssignmentStatus(bookingId, OperatorAssignmentStatus.ASSIGNED)
                || assignmentRepository.existsByBookingIdAndAssignmentStatusIn(bookingId, ACTIVE_ASSIGNMENT_STATUSES)) {
            log.warn("Assignment rejected: Booking {} already has an active operator assignment", bookingId);
            throw new BadRequestException("Booking is already assigned to an active operator");
        }

        // 5. Validate Operator existence and acquire pessimistic lock to serialize concurrent assignments for this operator
        Operator operator = operatorRepository.findByIdWithLock(request.getOperatorId())
                .or(() -> operatorRepository.findById(request.getOperatorId()))
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found with ID: " + request.getOperatorId()));

        // 6. Validate Operator eligibility: account active + APPROVED (contract v1.1: mobileVerified is
        //    informational only and is not an assignment gate)
        if (!operator.isActive()) {
            log.warn("Assignment rejected: Operator {} is inactive", operator.getId());
            throw new ForbiddenException("Operator account is inactive");
        }

        if (operator.getStatus() != OperatorStatus.APPROVED) {
            log.warn("Assignment rejected: Operator {} status is {}", operator.getId(), operator.getStatus());
            throw new ForbiddenException("Operator is not approved");
        }

        // 7. Validate schedule conflict / overlapping active assignments across all active states
        if (booking.getStartDate() == null || booking.getEndDate() == null) {
            log.warn("Assignment rejected: Booking {} has missing schedule dates (start: {}, end: {})",
                    bookingId, booking.getStartDate(), booking.getEndDate());
            throw new BadRequestException("Booking start date and end date are mandatory for operator assignment.");
        }
        if (booking.getStartDate().isAfter(booking.getEndDate())) {
            log.warn("Assignment rejected: Booking {} start date {} is after end date {}",
                    bookingId, booking.getStartDate(), booking.getEndDate());
            throw new BadRequestException("Booking start date cannot be after end date.");
        }

        boolean hasConflict = assignmentRepository.existsActiveOverlappingAssignment(
                operator.getId(),
                ACTIVE_ASSIGNMENT_STATUSES,
                booking.getStartDate(),
                booking.getEndDate(),
                bookingId
        );

        if (hasConflict) {
            log.warn("Assignment rejected: Operator {} has overlapping assignment for {} to {}",
                    operator.getId(), booking.getStartDate(), booking.getEndDate());
            throw new BadRequestException("Operator already has an active assignment for overlapping dates (" +
                    booking.getStartDate() + " to " + booking.getEndDate() + ")");
        }

        // 8. Create and persist OperatorJobAssignment
        OperatorJobAssignment assignment = OperatorJobAssignment.builder()
                .operator(operator)
                .booking(booking)
                .assignmentStatus(OperatorAssignmentStatus.ASSIGNED)
                .assignedAt(LocalDateTime.now())
                .assignedBy(assignedBy != null ? assignedBy : "SYSTEM")
                .notes(request.getNotes() != null ? request.getNotes().trim() : null)
                .build();

        OperatorJobAssignment savedAssignment = assignmentRepository.save(assignment);

        // 9. Synchronize booking relationship and transition status to OPERATOR_ASSIGNED. Equipment availability is
        //    not touched: CONFIRMED and OPERATOR_ASSIGNED both reserve the machine, so its flag is already right,
        //    and partner-owned UNDER_MAINTENANCE / INACTIVE flags must never be overridden by booking events.
        booking.setOperator(operator);
        booking.setStatus(BookingStatus.OPERATOR_ASSIGNED);
        bookingRepository.save(booking);

        // 10. Dispatch notifications
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
     * Retrieves the booking's current assignment (Admin/Partner visibility): the active one in any state from
     * ASSIGNED through PAUSED, otherwise the completed one. Declined or cancelled assignments are not current,
     * so after a decline the booking shows no operator until it is reassigned.
     *
     * @param bookingId Booking identifier
     * @return OperatorAssignmentResponse DTO or null if there is no current assignment
     */
    @Transactional(readOnly = true)
    public OperatorAssignmentResponse getBookingAssignment(Long bookingId) {
        log.info("Fetching operator assignment for booking ID: {}", bookingId);

        return assignmentRepository.findCurrentAssignmentForBooking(bookingId)
                .map(assignmentMapper::toAssignmentResponse)
                .orElse(null);
    }

    /**
     * Current assignment (including the assigned operator's contact number) for the partner that owns
     * the booking, or an admin. Ownership is checked before any assignment data is read.
     */
    @Transactional(readOnly = true)
    public OperatorAssignmentResponse getBookingAssignmentForCaller(Long bookingId, CurrentCaller caller) {
        if (!caller.isAdmin()) {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));
            if (booking.getPartner() == null || !caller.is(CurrentCaller.Kind.PARTNER, booking.getPartner().getId())) {
                throw new ForbiddenException("Access is denied. You are not authorized to view this booking's operator.");
            }
        }
        return getBookingAssignment(bookingId);
    }

    /**
     * Eligible-operator selection for assignment. Admins get the full view; partners get only what
     * selection needs: search by name/skills, masked mobile number and no home address. Other callers are refused.
     */
    @Transactional(readOnly = true)
    public Page<EligibleOperatorResponse> findEligibleOperatorsForCaller(String search, Pageable pageable, CurrentCaller caller) {
        if (caller.isAdmin()) {
            return findEligibleOperators(search, pageable);
        }
        if (caller.kind() != CurrentCaller.Kind.PARTNER) {
            throw new ForbiddenException("Access is denied. Only partners and administrators can list eligible operators.");
        }
        // Eligible = APPROVED + active (contract v1.1: no mobile-verification filter)
        Page<Operator> operators = (search != null && !search.trim().isEmpty())
                ? operatorRepository.searchAssignableOperatorsByNameOrSkills(OperatorStatus.APPROVED, true, search.trim(), pageable)
                : operatorRepository.findByStatusAndActive(OperatorStatus.APPROVED, true, pageable);
        return operators.map(operator -> {
            EligibleOperatorResponse response = assignmentMapper.toEligibleResponse(operator);
            response.setMobileNumber(maskMobile(response.getMobileNumber()));
            response.setAddress(null);
            return response;
        });
    }

    private static String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 4) {
            return null;
        }
        return "******" + mobile.substring(mobile.length() - 4);
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

        // Eligible = APPROVED + active (contract v1.1: no mobile-verification filter)
        Page<Operator> operators;
        if (search != null && !search.trim().isEmpty()) {
            operators = operatorRepository.searchAssignableOperators(
                    OperatorStatus.APPROVED,
                    true,
                    search.trim(),
                    pageable
            );
        } else {
            operators = operatorRepository.findByStatusAndActive(
                    OperatorStatus.APPROVED,
                    true,
                    pageable
            );
        }

        return operators.map(assignmentMapper::toEligibleResponse);
    }

    // ==========================================
    // PHASE 10: JOB HISTORY & ANALYTICS
    // ==========================================

    /**
     * Retrieves a paginated, filterable archive of historical job assignments for an authenticated Operator.
     */
    @Transactional(readOnly = true)
    public Page<OperatorJobHistoryResponse> getJobHistory(
            Long operatorId,
            LocalDate startDate,
            LocalDate endDate,
            OperatorAssignmentStatus status,
            String equipmentCategory,
            String search,
            Pageable pageable) {

        log.info("Fetching job history for operator ID {} with filters: status={}, startDate={}, endDate={}, category={}, search='{}'",
                operatorId, status, startDate, endDate, equipmentCategory, search);

        Operator operator = operatorRepository.findById(operatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found with ID: " + operatorId));

        if (!operator.isActive()) {
            throw new ForbiddenException("Operator account is inactive");
        }

        String sanitizedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String sanitizedCategory = (equipmentCategory != null && !equipmentCategory.trim().isEmpty() && !equipmentCategory.equalsIgnoreCase("ALL"))
                ? equipmentCategory.trim() : null;

        Page<OperatorJobAssignment> assignments = assignmentRepository.findJobHistory(
                operatorId,
                status,
                startDate,
                endDate,
                sanitizedCategory,
                sanitizedSearch,
                pageable
        );

        BigDecimal hourlyRate = operator.getHourlyRate() != null && operator.getHourlyRate().compareTo(BigDecimal.ZERO) > 0
                ? operator.getHourlyRate().setScale(2, RoundingMode.HALF_UP)
                : OperatorEarningsService.DEFAULT_HOURLY_RATE.setScale(2, RoundingMode.HALF_UP);

        return assignments.map(assignment -> mapToJobHistoryResponse(assignment, operator, hourlyRate));
    }

    /**
     * Calculates aggregate historical metrics (total jobs, completed, work hours, pause time, gross earnings, average rating)
     * for the authenticated operator.
     */
    @Transactional(readOnly = true)
    public OperatorJobHistorySummaryResponse getJobHistorySummary(
            Long operatorId,
            LocalDate startDate,
            LocalDate endDate,
            String equipmentCategory) {

        log.info("Calculating job history summary for operator ID {} with filters: startDate={}, endDate={}, category={}",
                operatorId, startDate, endDate, equipmentCategory);

        Operator operator = operatorRepository.findById(operatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found with ID: " + operatorId));

        if (!operator.isActive()) {
            throw new ForbiddenException("Operator account is inactive");
        }

        String sanitizedCategory = (equipmentCategory != null && !equipmentCategory.trim().isEmpty() && !equipmentCategory.equalsIgnoreCase("ALL"))
                ? equipmentCategory.trim() : null;

        List<OperatorJobAssignment> historicalAssignments = assignmentRepository.findJobHistoryForSummary(
                operatorId,
                startDate,
                endDate,
                sanitizedCategory
        );

        BigDecimal hourlyRate = operator.getHourlyRate() != null && operator.getHourlyRate().compareTo(BigDecimal.ZERO) > 0
                ? operator.getHourlyRate().setScale(2, RoundingMode.HALF_UP)
                : OperatorEarningsService.DEFAULT_HOURLY_RATE.setScale(2, RoundingMode.HALF_UP);

        long totalHistoricalJobs = historicalAssignments.size();
        long completedJobs = 0L;
        long rejectedJobs = 0L;
        long cancelledJobs = 0L;
        long totalWorkMinutes = 0L;
        long totalPausedMinutes = 0L;
        BigDecimal totalGrossEarnings = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        for (OperatorJobAssignment assignment : historicalAssignments) {
            OperatorAssignmentStatus s = assignment.getAssignmentStatus();
            if (s == OperatorAssignmentStatus.COMPLETED) {
                completedJobs++;
                OperatorJobEarningsResponse earnings = earningsService.computeJobEarnings(assignment, hourlyRate);
                totalWorkMinutes += earnings.getNetWorkMinutes();
                totalPausedMinutes += earnings.getPausedMinutes();
                totalGrossEarnings = totalGrossEarnings.add(earnings.getGrossEarnings());
            } else if (s == OperatorAssignmentStatus.REJECTED) {
                rejectedJobs++;
            } else if (s == OperatorAssignmentStatus.CANCELLED) {
                cancelledJobs++;
            }
        }

        BigDecimal totalWorkHours = BigDecimal.valueOf((double) Math.round((totalWorkMinutes / 60.0) * 100.0) / 100.0)
                .setScale(2, RoundingMode.HALF_UP);

        // Fetch rating metrics
        List<OperatorReview> reviews = reviewRepository.findByOperatorId(operatorId);
        long totalReviewsCount = reviews != null ? reviews.size() : 0L;
        Double averageRating = 0.0;
        if (reviews != null && !reviews.isEmpty()) {
            double sum = 0.0;
            for (OperatorReview rev : reviews) {
                if (rev.getRating() != null) {
                    sum += rev.getRating();
                }
            }
            averageRating = (double) Math.round((sum / reviews.size()) * 10.0) / 10.0;
        }

        return OperatorJobHistorySummaryResponse.builder()
                .operatorId(operatorId)
                .totalHistoricalJobs(totalHistoricalJobs)
                .completedJobs(completedJobs)
                .rejectedJobs(rejectedJobs)
                .cancelledJobs(cancelledJobs)
                .totalWorkHours(totalWorkHours)
                .totalPausedMinutes(totalPausedMinutes)
                .totalGrossEarnings(totalGrossEarnings)
                .currency(OperatorEarningsService.CURRENCY_INR)
                .averageRating(averageRating)
                .totalReviewsCount(totalReviewsCount)
                .build();
    }

    /**
     * Helper mapper creating an enriched OperatorJobHistoryResponse DTO.
     */
    private OperatorJobHistoryResponse mapToJobHistoryResponse(
            OperatorJobAssignment assignment,
            Operator operator,
            BigDecimal hourlyRate) {

        Booking booking = assignment.getBooking();
        Equipment equipment = booking != null ? booking.getEquipment() : null;
        Partner partner = booking != null ? booking.getPartner() : null;

        // Financials & Duration
        OperatorJobEarningsResponse earnings = earningsService.computeJobEarnings(assignment, hourlyRate);

        // Review & Customer Rating
        Optional<OperatorReview> reviewOpt = reviewRepository.findByAssignmentId(assignment.getId());
        Integer rating = reviewOpt.map(OperatorReview::getRating).orElse(null);
        String reviewComment = reviewOpt.map(OperatorReview::getComment).orElse(null);
        LocalDateTime reviewSubmittedAt = reviewOpt.map(OperatorReview::getCreatedAt).orElse(null);

        // GPS Latest Coordinates
        Optional<OperatorLocation> latestLoc = locationRepository.findTopByAssignmentIdOrderByRecordedAtDesc(assignment.getId());
        boolean hasGps = latestLoc.isPresent();
        Double latestLat = latestLoc.map(OperatorLocation::getLatitude).orElse(null);
        Double latestLon = latestLoc.map(OperatorLocation::getLongitude).orElse(null);

        return OperatorJobHistoryResponse.builder()
                .assignmentId(assignment.getId())
                .bookingId(booking != null ? booking.getId() : null)
                .operatorId(operator.getId())
                .operatorName(operator.getFullName())
                .equipmentId(equipment != null ? equipment.getId() : null)
                .equipmentName(equipment != null ? equipment.getName() : "Agricultural Machinery")
                .equipmentModel(equipment != null ? equipment.getModel() : "Standard Model")
                .equipmentCategory(equipment != null && equipment.getCategory() != null ? equipment.getCategory().name() : "GENERAL")
                .equipmentRegistrationNumber(equipment != null && equipment.getBrand() != null ? equipment.getBrand() + " " + equipment.getModel() : "N/A")
                .farmerId(booking != null ? booking.getFarmerId() : null)
                .farmerName(booking != null ? "Verified Farmer #" + booking.getFarmerId() : "Verified Farmer")
                .deliveryAddress(booking != null && booking.getDeliveryAddress() != null ? booking.getDeliveryAddress() : "On-site Field Delivery")
                .partnerId(partner != null ? partner.getId() : null)
                .partnerName(partner != null ? partner.getBusinessName() : "Authorized Equipment Partner")
                .bookingStartDate(booking != null ? booking.getStartDate() : null)
                .bookingEndDate(booking != null ? booking.getEndDate() : null)
                .assignmentStatus(assignment.getAssignmentStatus())
                .notes(assignment.getNotes())
                .rejectionReason(assignment.getRejectionReason())
                .pauseReason(assignment.getPauseReason())
                .assignedAt(assignment.getAssignedAt())
                .acceptedAt(assignment.getAcceptedAt())
                .rejectedAt(assignment.getRejectedAt())
                .travelingAt(assignment.getTravelingAt())
                .reachedAt(assignment.getReachedAt())
                .workStartedAt(assignment.getWorkStartedAt())
                .pausedAt(assignment.getPausedAt())
                .resumedAt(assignment.getResumedAt())
                .completedAt(assignment.getCompletedAt())
                .totalElapsedMinutes(earnings.getTotalElapsedMinutes())
                .totalPausedMinutes(earnings.getPausedMinutes())
                .netWorkHours(BigDecimal.valueOf(earnings.getNetWorkHours()).setScale(2, RoundingMode.HALF_UP))
                .hourlyRate(hourlyRate)
                .grossEarnings(earnings.getGrossEarnings())
                .currency(OperatorEarningsService.CURRENCY_INR)
                .isFinalized(assignment.getAssignmentStatus() == OperatorAssignmentStatus.COMPLETED)
                .customerRating(rating)
                .customerReview(reviewComment)
                .reviewSubmittedAt(reviewSubmittedAt)
                .hasGpsData(hasGps)
                .latestLatitude(latestLat)
                .latestLongitude(latestLon)
                .build();
    }
}
