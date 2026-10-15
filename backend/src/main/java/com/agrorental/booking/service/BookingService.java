package com.agrorental.booking.service;

import com.agrorental.booking.dto.BookingCreateRequest;
import com.agrorental.booking.dto.BookingResponse;
import com.agrorental.booking.dto.BookingStatusUpdateRequest;
import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.booking.mapper.BookingMapper;
import com.agrorental.booking.repository.BookingRepository;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.ForbiddenException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.payment.service.RefundService;
import com.agrorental.security.principal.CurrentCaller;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.enums.AvailabilityStatus;
import com.agrorental.equipment.repository.EquipmentRepository;
import com.agrorental.equipment.service.EquipmentAvailabilityService;
import com.agrorental.farmer.entity.Farm;
import com.agrorental.farmer.repository.FarmRepository;
import com.agrorental.notification.service.NotificationService;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorJobAssignment;
import com.agrorental.operator.enums.OperatorAssignmentStatus;
import com.agrorental.operator.dto.OperatorAssignmentRequest;
import com.agrorental.operator.service.OperatorAssignmentService;
import com.agrorental.operator.repository.OperatorJobAssignmentRepository;
import com.agrorental.operator.repository.OperatorLocationRepository;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.partner.entity.Partner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service implementation managing equipment rental reservations, state synchronization, and lifecycle rules.
 */
@Service
@Transactional(readOnly = true)
public class BookingService {

    private final BookingRepository bookingRepository;
    private final EquipmentRepository equipmentRepository;
    private final OperatorRepository operatorRepository;
    private final FarmRepository farmRepository;
    private final NotificationService notificationService;
    private final BookingMapper bookingMapper;
    private final OperatorJobAssignmentRepository operatorJobAssignmentRepository;
    private final OperatorLocationRepository operatorLocationRepository;
    private final OperatorAssignmentService operatorAssignmentService;
    private RefundService refundService;
    /** Owner of equipment availability writes; a local instance is used when no bean is wired (plain unit tests). */
    private EquipmentAvailabilityService equipmentAvailabilityService;

    /** Longest single booking (inclusive days); longer requests would block a machine for years. */
    static final long MAX_BOOKING_DAYS = 365;
    /** Largest total the bookings.total_cost column (DECIMAL(12,2)) can store. */
    static final BigDecimal MAX_BOOKING_TOTAL = new BigDecimal("9999999999.99");
    /** bookings.delivery_address is VARCHAR(255). */
    private static final int MAX_DELIVERY_ADDRESS_LENGTH = 255;
    /** Upper bound for a rejection reason (stored as TEXT; kept well below its byte limit). */
    static final int MAX_REJECTION_REASON_LENGTH = 2000;
    /** notifications.message is VARCHAR(500); the reason quoted in a notification is shortened to this. */
    private static final int MAX_NOTIFICATION_MESSAGE_LENGTH = 500;
    private static final int MAX_QUOTED_REASON_LENGTH = 300;

    /** Required collaborator: Spring always injects it; a missing bean fails application startup. */
    @Autowired
    public void setRefundService(@Lazy RefundService refundService) {
        this.refundService = refundService;
    }

    @Autowired(required = false)
    public void setEquipmentAvailabilityService(EquipmentAvailabilityService equipmentAvailabilityService) {
        if (equipmentAvailabilityService != null) {
            this.equipmentAvailabilityService = equipmentAvailabilityService;
        }
    }

    /** Re-derives the equipment flag from its reserving bookings (see {@link EquipmentAvailabilityService}). */
    private void refreshEquipmentAvailability(Booking booking) {
        equipmentAvailabilityService.refreshAfterBookingChange(booking);
    }

    private static String abbreviate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 1) + "…";
    }

    private static void validateRejectionReasonLength(String reason) {
        if (reason != null && reason.trim().length() > MAX_REJECTION_REASON_LENGTH) {
            throw new BadRequestException("Rejection reason cannot exceed " + MAX_REJECTION_REASON_LENGTH + " characters.");
        }
    }

    /**
     * BIZ-20: a paid booking that is cancelled or rejected gets its full refund initiated in the same
     * transaction that holds the booking row lock (booking -> payment lock order).
     */
    private void initiateRefundIfPaid(Booking booking, String reason, String actor) {
        if (refundService != null) {
            refundService.initiateFullRefundIfPaid(booking.getId(), reason, actor);
        }
    }

    private static String currentActor(String fallback) {
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getName() != null ? auth.getName() : fallback;
    }

    public BookingService(
            BookingRepository bookingRepository,
            EquipmentRepository equipmentRepository,
            OperatorRepository operatorRepository,
            FarmRepository farmRepository,
            NotificationService notificationService,
            BookingMapper bookingMapper) {
        this(bookingRepository, equipmentRepository, operatorRepository, farmRepository, notificationService, bookingMapper, null, null, null);
    }

    public BookingService(
            BookingRepository bookingRepository,
            EquipmentRepository equipmentRepository,
            OperatorRepository operatorRepository,
            FarmRepository farmRepository,
            NotificationService notificationService,
            BookingMapper bookingMapper,
            OperatorJobAssignmentRepository operatorJobAssignmentRepository,
            OperatorLocationRepository operatorLocationRepository) {
        this(bookingRepository, equipmentRepository, operatorRepository, farmRepository, notificationService, bookingMapper, operatorJobAssignmentRepository, operatorLocationRepository, null);
    }

    @Autowired
    public BookingService(
            BookingRepository bookingRepository,
            EquipmentRepository equipmentRepository,
            OperatorRepository operatorRepository,
            FarmRepository farmRepository,
            NotificationService notificationService,
            BookingMapper bookingMapper,
            OperatorJobAssignmentRepository operatorJobAssignmentRepository,
            OperatorLocationRepository operatorLocationRepository,
            @Lazy OperatorAssignmentService operatorAssignmentService) {
        this.bookingRepository = bookingRepository;
        this.equipmentRepository = equipmentRepository;
        this.operatorRepository = operatorRepository;
        this.farmRepository = farmRepository;
        this.notificationService = notificationService;
        this.bookingMapper = bookingMapper;
        this.operatorJobAssignmentRepository = operatorJobAssignmentRepository;
        this.operatorLocationRepository = operatorLocationRepository;
        this.operatorAssignmentService = operatorAssignmentService;
        this.equipmentAvailabilityService = new EquipmentAvailabilityService(bookingRepository, equipmentRepository);
    }

    /**
     * Creates a new machinery reservation for a farmer.
     *
     * @param request Booking creation payload
     * @return Populated BookingResponse DTO
     */
    /**
     * Statuses that hold the equipment for their date range. A new booking may not overlap any of them.
     */
    private static final List<BookingStatus> RESERVING_STATUSES = BookingStatus.RESERVING_STATUSES;

    /**
     * Creates a booking. The equipment row is locked (SELECT ... FOR UPDATE) before the overlap check, and the
     * transaction runs at READ COMMITTED so the check sees bookings committed by a transaction that held the
     * lock before us; together this prevents two concurrent requests from reserving overlapping dates.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookingResponse createBooking(BookingCreateRequest request) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || (auth.getPrincipal() instanceof String && "anonymousUser".equals(auth.getPrincipal()))) {
            throw new org.springframework.security.access.AccessDeniedException("Authentication is required to create a booking.");
        }
        if (!(auth.getPrincipal() instanceof com.agrorental.security.principal.FarmerPrincipal farmerPrincipal)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Access is denied. Only farmers are permitted to create bookings.");
        }

        if (request.getFarmerId() != null && !request.getFarmerId().equals(farmerPrincipal.getId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Access is denied. You cannot create a booking for another farmer.");
        }
        request.setFarmerId(farmerPrincipal.getId());

        if (request.getFarmerId() == null) {
            throw new BadRequestException("Farmer ID is required");
        }

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date cannot be before start date");
        }

        long requestedDays = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        if (requestedDays > MAX_BOOKING_DAYS) {
            throw new BadRequestException("A booking can cover at most " + MAX_BOOKING_DAYS + " days");
        }

        Equipment equipment = equipmentRepository.findByIdForUpdate(request.getEquipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found with ID: " + request.getEquipmentId()));

        if (Boolean.TRUE.equals(equipment.getIsDisabled()) ||
                equipment.getAvailabilityStatus() == AvailabilityStatus.INACTIVE ||
                equipment.getAvailabilityStatus() == AvailabilityStatus.UNDER_MAINTENANCE) {
            throw new BadRequestException("Equipment is currently unavailable for rental");
        }
        // A rejected or deactivated partner cannot accept the booking; it would only block these dates
        Partner owner = equipment.getPartner();
        if (owner != null && (!owner.isActive() || owner.getVerificationStatus() == Partner.VerificationStatus.REJECTED)) {
            throw new BadRequestException("Equipment is currently unavailable for rental");
        }

        // The partner's offered window (the same bounds the date-range search applies)
        if ((equipment.getAvailableFromDate() != null && request.getStartDate().isBefore(equipment.getAvailableFromDate()))
                || (equipment.getAvailableToDate() != null && request.getEndDate().isAfter(equipment.getAvailableToDate()))) {
            throw new BadRequestException("Equipment is offered for rental only from "
                    + (equipment.getAvailableFromDate() != null ? equipment.getAvailableFromDate() : "today")
                    + " to " + (equipment.getAvailableToDate() != null ? equipment.getAvailableToDate() : "any later date"));
        }

        boolean hasOverlap = bookingRepository.existsOverlappingBooking(
                equipment.getId(),
                RESERVING_STATUSES,
                request.getStartDate(),
                request.getEndDate()
        );

        if (hasOverlap) {
            throw new BadRequestException("Equipment is already reserved for the selected date range");
        }

        Farm farm = null;
        if (request.getFarmId() != null) {
            farm = farmRepository.findById(request.getFarmId())
                    .orElseThrow(() -> new ResourceNotFoundException("Farm not found with ID: " + request.getFarmId()));

            if (!farm.getFarmerId().equals(request.getFarmerId())) {
                throw new BadRequestException("Farmer is not authorized to use this farm");
            }
        }

        long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate()) + 1;
        if (days < 1) {
            days = 1;
        }

        BigDecimal totalCost = equipment.getRentalPrice().multiply(BigDecimal.valueOf(days));
        if (totalCost.compareTo(MAX_BOOKING_TOTAL) > 0) {
            throw new BadRequestException("The booking total exceeds the maximum supported amount. Please book fewer days.");
        }
        Partner partner = equipment.getPartner();

        Booking booking = bookingMapper.toEntity(request, equipment, partner, farm, totalCost);
        if (farm != null && (booking.getDeliveryAddress() == null || booking.getDeliveryAddress().isBlank())) {
            String farmAddress = String.format("%s, %s, %s, %s", farm.getFarmName(), farm.getVillage(), farm.getTaluka(), farm.getDistrict());
            booking.setDeliveryAddress(abbreviate(farmAddress, MAX_DELIVERY_ADDRESS_LENGTH));
        }

        Booking savedBooking = bookingRepository.save(booking);

        // The new reservation holds the equipment: re-derive its flag (BOOKED unless under maintenance/inactive)
        refreshEquipmentAvailability(savedBooking != null ? savedBooking : booking);

        if (partner != null && partner.getId() != null) {
            notificationService.sendNotification(
                    "PARTNER",
                    partner.getId(),
                    "New Booking Request",
                    "New booking request #" + savedBooking.getId() + " received for " + equipment.getName() + ".",
                    "BOOKING_CREATED",
                    savedBooking.getId()
            );
        }

        return bookingMapper.toResponse(savedBooking);
    }

    /**
     * Retrieves a booking reservation by its primary key.
     *
     * @param id Booking identifier
     * @return Populated BookingResponse DTO
     */
    public BookingResponse getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + id));
        return bookingMapper.toResponse(booking);
    }

    /**
     * Retrieves all bookings created by a specific farmer.
     *
     * @param farmerId Farmer identifier
     * @return List of BookingResponse DTOs
     */
    public List<BookingResponse> getBookingsByFarmer(Long farmerId) {
        return bookingRepository.findByFarmerId(farmerId).stream()
                .map(bookingMapper::toResponse)
                .toList();
    }

    /**
     * Retrieves all booking requests for equipment belonging to a specific partner.
     *
     * @param partnerId Partner identifier
     * @return List of BookingResponse DTOs
     */
    public List<BookingResponse> getBookingsByPartner(Long partnerId) {
        return bookingRepository.findByPartnerId(partnerId).stream()
                .map(bookingMapper::toResponse)
                .toList();
    }

    /**
     * Retrieves all booking requests assigned to a specific operator.
     *
     * @param operatorId Operator identifier
     * @return List of BookingResponse DTOs
     */
    public List<BookingResponse> getBookingsByOperator(Long operatorId) {
        return bookingRepository.findByOperatorId(operatorId).stream()
                .map(bookingMapper::toResponse)
                .toList();
    }

    /**
     * Booking detail for an authenticated caller: the owning farmer, the owning partner, the currently
     * assigned operator, or an admin. Everyone else is refused before any booking data is mapped.
     */
    public BookingResponse getBookingForCaller(Long id, CurrentCaller caller) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + id));
        boolean allowed = caller.isAdmin()
                || caller.is(CurrentCaller.Kind.FARMER, booking.getFarmerId())
                || (booking.getPartner() != null && caller.is(CurrentCaller.Kind.PARTNER, booking.getPartner().getId()))
                || (booking.getOperator() != null && caller.is(CurrentCaller.Kind.OPERATOR, booking.getOperator().getId()));
        if (!allowed) {
            throw new ForbiddenException("Access is denied. You are not authorized to view this booking.");
        }
        return bookingMapper.toResponse(booking);
    }

    /** An operator may list only its own bookings (admins may list any operator's). */
    public List<BookingResponse> getBookingsForOperator(Long operatorId, CurrentCaller caller) {
        if (!caller.isAdmin() && !caller.is(CurrentCaller.Kind.OPERATOR, operatorId)) {
            throw new ForbiddenException("Access is denied. You can only view your own bookings.");
        }
        return getBookingsByOperator(operatorId);
    }

    /**
     * Partner accepts an eligible booking request.
     *
     * @param id Booking identifier
     * @param partnerId Optional partner identifier for authorization
     * @return Updated BookingResponse
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookingResponse acceptBooking(Long id, Long partnerId) {
        Booking booking = bookingRepository.findByIdWithLock(id)
                .or(() -> bookingRepository.findById(id))
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + id));

        // Fail closed: a booking without a partner belongs to no partner
        if (partnerId != null && (booking.getPartner() == null || !booking.getPartner().getId().equals(partnerId))) {
            throw new BadRequestException("Unauthorized: Booking does not belong to the specified partner.");
        }

        // Only a booking awaiting the partner's decision can be accepted; later states must not move backwards
        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.ACCEPTED) {
            throw new BadRequestException("Cannot accept booking in current status: " + booking.getStatus());
        }

        // Serialize with other reservations of the same equipment before the overlap check
        if (booking.getEquipment() != null) {
            equipmentRepository.findByIdForUpdate(booking.getEquipment().getId());
        }

        // Check double-booking protection for active statuses (an ACCEPTED booking must not conflict with itself)
        boolean hasOverlap = bookingRepository.existsOverlappingBookingExcluding(
                booking.getEquipment().getId(),
                List.of(BookingStatus.CONFIRMED, BookingStatus.ACCEPTED, BookingStatus.OPERATOR_ASSIGNED, BookingStatus.ON_THE_WAY, BookingStatus.WORK_STARTED),
                booking.getStartDate(),
                booking.getEndDate(),
                booking.getId()
        );

        if (hasOverlap) {
            throw new BadRequestException("Equipment is already reserved/confirmed for overlapping dates.");
        }

        booking.setStatus(BookingStatus.CONFIRMED);

        refreshEquipmentAvailability(booking);

        Booking saved = bookingRepository.save(booking);

        String equipName = saved.getEquipment() != null ? saved.getEquipment().getName() : "Machinery";
        notificationService.sendNotification(
                "FARMER",
                saved.getFarmerId(),
                "Booking Confirmed",
                "Your booking #" + saved.getId() + " for " + equipName + " has been accepted and confirmed.",
                "BOOKING_CONFIRMED",
                saved.getId()
        );

        return bookingMapper.toResponse(saved);
    }

    /**
     * Partner rejects an eligible booking request with a mandatory reason.
     *
     * @param id Booking identifier
     * @param partnerId Optional partner identifier for authorization
     * @param rejectionReason Mandatory reason explanation
     * @return Updated BookingResponse
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookingResponse rejectBooking(Long id, Long partnerId, String rejectionReason) {
        Booking booking = bookingRepository.findByIdWithLock(id)
                .or(() -> bookingRepository.findById(id))
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + id));

        // Fail closed: a booking without a partner belongs to no partner
        if (partnerId != null && (booking.getPartner() == null || !booking.getPartner().getId().equals(partnerId))) {
            throw new BadRequestException("Unauthorized: Booking does not belong to the specified partner.");
        }

        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            throw new BadRequestException("A rejection reason is mandatory when declining a booking.");
        }
        validateRejectionReasonLength(rejectionReason);

        // A booking can only be declined while it is still awaiting the partner's decision
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Only pending bookings can be rejected. Current status: " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.REJECTED);
        booking.setRejectionReason(rejectionReason.trim());
        initiateRefundIfPaid(booking, "Booking #" + booking.getId() + " rejected by partner: " + rejectionReason.trim(),
                partnerId != null ? "PARTNER_" + partnerId : currentActor("PARTNER"));

        // Frees the equipment only if no other booking still holds it
        refreshEquipmentAvailability(booking);

        cleanupOperatorAssignments(booking);

        Booking saved = bookingRepository.save(booking);

        String equipName = saved.getEquipment() != null ? saved.getEquipment().getName() : "Machinery";
        notificationService.sendNotification(
                "FARMER",
                saved.getFarmerId(),
                "Booking Declined",
                abbreviate("Your booking #" + saved.getId() + " for " + equipName + " was declined. Reason: "
                        + abbreviate(rejectionReason.trim(), MAX_QUOTED_REASON_LENGTH), MAX_NOTIFICATION_MESSAGE_LENGTH),
                "BOOKING_REJECTED",
                saved.getId()
        );

        return bookingMapper.toResponse(saved);
    }

    /**
     * Partner assigns a qualified operator to a booking.
     *
     * @param bookingId Booking identifier
     * @param partnerId Optional partner identifier for authorization
     * @param operatorId Operator identifier
     * @return Updated BookingResponse
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookingResponse assignOperator(Long bookingId, Long partnerId, Long operatorId) {
        if (operatorAssignmentService != null) {
            OperatorAssignmentRequest request = OperatorAssignmentRequest.builder()
                    .operatorId(operatorId)
                    .notes(partnerId != null ? "Assigned by partner " + partnerId : "Assigned by system")
                    .build();
            String assignedBy = partnerId != null ? "PARTNER_" + partnerId : "ADMIN/PARTNER";
            operatorAssignmentService.assignOperator(bookingId, request, assignedBy, partnerId);
            return getBookingById(bookingId);
        }

        // Fallback for tests running without OperatorAssignmentService
        Booking booking = bookingRepository.findByIdWithLock(bookingId)
                .or(() -> bookingRepository.findById(bookingId))
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + bookingId));

        if (partnerId != null && booking.getPartner() != null && !booking.getPartner().getId().equals(partnerId)) {
            throw new BadRequestException("Unauthorized: Booking does not belong to the specified partner.");
        }

        if (operatorId == null) {
            throw new BadRequestException("Operator ID is required.");
        }

        Operator operator = operatorRepository.findById(operatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found with ID: " + operatorId));

        // Check if operator has conflicting overlapping assignments
        List<Booking> operatorBookings = bookingRepository.findByOperatorId(operatorId);
        boolean hasConflict = operatorBookings.stream()
                .filter(b -> !b.getId().equals(bookingId))
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.ACCEPTED || b.getStatus() == BookingStatus.OPERATOR_ASSIGNED || b.getStatus() == BookingStatus.ON_THE_WAY || b.getStatus() == BookingStatus.WORK_STARTED)
                .anyMatch(b -> !b.getStartDate().isAfter(booking.getEndDate()) && !b.getEndDate().isBefore(booking.getStartDate()));

        if (hasConflict) {
            throw new BadRequestException("Operator already has an active assignment for overlapping dates (" +
                    booking.getStartDate() + " to " + booking.getEndDate() + ")");
        }

        booking.setOperator(operator);
        booking.setStatus(BookingStatus.OPERATOR_ASSIGNED);

        if (operatorJobAssignmentRepository != null) {
            OperatorJobAssignment assignment = OperatorJobAssignment.builder()
                    .operator(operator)
                    .booking(booking)
                    .assignmentStatus(OperatorAssignmentStatus.ASSIGNED)
                    .assignedAt(LocalDateTime.now())
                    .assignedBy(partnerId != null ? "PARTNER_" + partnerId : "SYSTEM")
                    .build();
            operatorJobAssignmentRepository.save(assignment);
        }

        Booking saved = bookingRepository.save(booking);

        String equipName = saved.getEquipment() != null ? saved.getEquipment().getName() : "Machinery";

        notificationService.sendNotification(
                "OPERATOR",
                operatorId,
                "New Job Assignment",
                "You have been assigned to job #" + saved.getId() + " (" + equipName + ") from " + saved.getStartDate() + " to " + saved.getEndDate() + ".",
                "OPERATOR_ASSIGNED",
                saved.getId()
        );

        notificationService.sendNotification(
                "FARMER",
                saved.getFarmerId(),
                "Operator Assigned",
                "Certified operator " + operator.getFullName() + " has been assigned to your booking #" + saved.getId() + ".",
                "OPERATOR_ASSIGNED",
                saved.getId()
        );

        if (saved.getPartner() != null && saved.getPartner().getId() != null) {
            notificationService.sendNotification(
                    "PARTNER",
                    saved.getPartner().getId(),
                    "Operator Assigned",
                    "Operator " + operator.getFullName() + " has been assigned to your booking #" + saved.getId() + ".",
                    "OPERATOR_ASSIGNED",
                    saved.getId()
            );
        }

        return bookingMapper.toResponse(saved);
    }

    /**
     * Cancels an existing booking and restores equipment availability to AVAILABLE.
     *
     * @param id Booking identifier
     * @return Updated BookingResponse DTO
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookingResponse cancelBooking(Long id) {
        return cancelBooking(id, null);
    }

    /**
     * Cancels an existing booking with ownership validation.
     *
     * @param id Booking identifier
     * @param requestingFarmerId Farmer ID attempting cancellation (optional)
     * @return Updated BookingResponse DTO
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookingResponse cancelBooking(Long id, Long requestingFarmerId) {
        Booking booking = bookingRepository.findByIdWithLock(id)
                .or(() -> bookingRepository.findById(id))
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + id));

        if (requestingFarmerId != null && !booking.getFarmerId().equals(requestingFarmerId)) {
            throw new org.springframework.security.access.AccessDeniedException("Access is denied. You do not have permission to cancel another farmer's booking.");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.COMPLETED
                || booking.getStatus() == BookingStatus.REJECTED) {
            throw new BadRequestException("Booking cannot be cancelled in its current state: " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.CANCELLED);
        initiateRefundIfPaid(booking, "Booking #" + booking.getId() + " cancelled",
                requestingFarmerId != null ? "FARMER_" + requestingFarmerId : currentActor("ADMIN"));

        // Frees the equipment only if no other booking still holds it
        refreshEquipmentAvailability(booking);

        cleanupOperatorAssignments(booking);

        Booking saved = bookingRepository.save(booking);

        notificationService.sendNotification(
                "FARMER",
                saved.getFarmerId(),
                "Booking Cancelled",
                "Your booking #" + saved.getId() + " has been cancelled.",
                "BOOKING_CANCELLED",
                saved.getId()
        );
        if (saved.getPartner() != null && saved.getPartner().getId() != null) {
            notificationService.sendNotification(
                    "PARTNER",
                    saved.getPartner().getId(),
                    "Booking Cancelled",
                    "Booking #" + saved.getId() + " was cancelled.",
                    "BOOKING_CANCELLED",
                    saved.getId()
            );
        }

        return bookingMapper.toResponse(saved);
    }

    /**
     * Updates the status of a booking or assigns an operator.
     *
     * @param id Booking identifier
     * @param request Status update payload
     * @return Updated BookingResponse DTO
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookingResponse updateBookingStatus(Long id, BookingStatusUpdateRequest request) {
        Booking booking = bookingRepository.findByIdWithLock(id)
                .or(() -> bookingRepository.findById(id))
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + id));

        BookingStatus newStatus = request.getStatus();
        validateRejectionReasonLength(request.getRejectionReason());

        // Admin overrides must not bypass the rejection rule or double-booking protection
        if (newStatus == BookingStatus.REJECTED && booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Only pending bookings can be rejected. Current status: " + booking.getStatus());
        }
        // BIZ-20: a cancelled/rejected booking with a refund obligation stays closed (no reactivation or completion)
        if ((booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.REJECTED)
                && newStatus != BookingStatus.CANCELLED && newStatus != BookingStatus.REJECTED
                && refundService != null && refundService.hasRefundForBooking(booking.getId())) {
            throw new BadRequestException("This booking has a refund on record and cannot be reactivated.");
        }
        if (RESERVING_STATUSES.contains(newStatus) && !RESERVING_STATUSES.contains(booking.getStatus())
                && booking.getEquipment() != null) {
            equipmentRepository.findByIdForUpdate(booking.getEquipment().getId());
            if (bookingRepository.existsOverlappingBooking(booking.getEquipment().getId(), RESERVING_STATUSES,
                    booking.getStartDate(), booking.getEndDate())) {
                throw new BadRequestException("Equipment is already reserved for the selected date range");
            }
        }

        // If target status is OPERATOR_ASSIGNED with an operator, route through canonical assignment service
        if (newStatus == BookingStatus.OPERATOR_ASSIGNED && request.getOperatorId() != null) {
            if (operatorAssignmentService != null) {
                OperatorAssignmentRequest assignRequest = OperatorAssignmentRequest.builder()
                        .operatorId(request.getOperatorId())
                        .notes("Assigned via status update")
                        .build();
                operatorAssignmentService.assignOperator(id, assignRequest, "ADMIN", null);
                if (request.getRejectionReason() != null && !request.getRejectionReason().isBlank()) {
                    Booking reloaded = bookingRepository.findById(id).orElse(booking);
                    reloaded.setRejectionReason(request.getRejectionReason().trim());
                    bookingRepository.save(reloaded);
                }
                return getBookingById(id);
            }
        }

        // Operators are attached only through the canonical assignment above (approval, eligibility, overlap and
        // ownership checks plus the job row); any other status carrying an operatorId would bypass all of them
        if (request.getOperatorId() != null) {
            throw new BadRequestException("An operator can only be assigned with status OPERATOR_ASSIGNED.");
        }
        if (newStatus == BookingStatus.OPERATOR_ASSIGNED) {
            throw new BadRequestException("Operator ID is required when updating status to OPERATOR_ASSIGNED");
        }
        // While an operator job is active the booking status follows the operator; only cancellation (which closes
        // the job) may override it. A forced COMPLETED would leave the job open or erase the operator's earnings.
        if (newStatus != BookingStatus.CANCELLED && newStatus != BookingStatus.REJECTED && operatorJobAssignmentRepository != null
                && operatorJobAssignmentRepository.existsByBookingIdAndAssignmentStatusIn(
                        id, com.agrorental.operator.service.OperatorAssignmentService.ACTIVE_ASSIGNMENT_STATUSES)) {
            throw new BadRequestException("Booking #" + id + " has an active operator job; its status follows the operator. "
                    + "Cancel the booking instead.");
        }

        BookingStatus previousStatus = booking.getStatus();
        booking.setStatus(newStatus);

        if (request.getRejectionReason() != null && !request.getRejectionReason().isBlank()) {
            booking.setRejectionReason(request.getRejectionReason().trim());
        }

        if ((newStatus == BookingStatus.CANCELLED || newStatus == BookingStatus.REJECTED) && previousStatus != newStatus) {
            initiateRefundIfPaid(booking, "Booking #" + booking.getId() + " set to " + newStatus + " by administrator",
                    currentActor("ADMIN"));
        }

        // Any status change re-derives the equipment flag from all of its bookings (never a blind toggle)
        refreshEquipmentAvailability(booking);
        if (newStatus == BookingStatus.CANCELLED || newStatus == BookingStatus.REJECTED) {
            cleanupOperatorAssignments(booking);
        }

        Booking saved = bookingRepository.save(booking);

        String equipName = saved.getEquipment() != null ? saved.getEquipment().getName() : "Machinery";
        Long partnerId = saved.getPartner() != null ? saved.getPartner().getId() : null;

        if (newStatus == BookingStatus.CONFIRMED || newStatus == BookingStatus.ACCEPTED) {
            notificationService.sendNotification("FARMER", saved.getFarmerId(), "Booking Confirmed", "Your booking #" + saved.getId() + " for " + equipName + " has been confirmed.", "BOOKING_CONFIRMED", saved.getId());
        } else if (newStatus == BookingStatus.REJECTED) {
            String reasonText = saved.getRejectionReason() != null
                    ? " Reason: " + abbreviate(saved.getRejectionReason(), MAX_QUOTED_REASON_LENGTH) : "";
            notificationService.sendNotification("FARMER", saved.getFarmerId(), "Booking Declined", abbreviate("Your booking #" + saved.getId() + " for " + equipName + " was declined." + reasonText, MAX_NOTIFICATION_MESSAGE_LENGTH), "BOOKING_REJECTED", saved.getId());
        } else if (newStatus == BookingStatus.CANCELLED && previousStatus != BookingStatus.CANCELLED) {
            // Same notices as the cancel endpoints: the farmer and the partner must learn about an admin cancellation
            notificationService.sendNotification("FARMER", saved.getFarmerId(), "Booking Cancelled", "Your booking #" + saved.getId() + " has been cancelled.", "BOOKING_CANCELLED", saved.getId());
            if (partnerId != null) {
                notificationService.sendNotification("PARTNER", partnerId, "Booking Cancelled", "Booking #" + saved.getId() + " was cancelled.", "BOOKING_CANCELLED", saved.getId());
            }
        } else if (newStatus == BookingStatus.COMPLETED) {
            notificationService.sendNotification("FARMER", saved.getFarmerId(), "Job Completed", "Machinery service for booking #" + saved.getId() + " has been marked completed.", "BOOKING_COMPLETED", saved.getId());
            if (partnerId != null) {
                notificationService.sendNotification("PARTNER", partnerId, "Job Completed", "Rental order #" + saved.getId() + " is completed.", "BOOKING_COMPLETED", saved.getId());
            }
        }

        return bookingMapper.toResponse(saved);
    }

    /**
     * Cleans up operator job assignments and stops GPS tracking when a booking is cancelled or rejected.
     * Prevents active assignments from dangling on operator dashboards, deactivates tracking,
     * dispatches cancellation notifications, and preserves terminal assignments (COMPLETED, REJECTED, CANCELLED).
     *
     * @param booking Target booking being cancelled or rejected
     */
    private void cleanupOperatorAssignments(Booking booking) {
        if (booking == null || booking.getId() == null) {
            return;
        }

        java.util.Set<Long> notifiedOperatorIds = new java.util.HashSet<>();

        if (operatorJobAssignmentRepository != null) {
            List<OperatorJobAssignment> assignments = operatorJobAssignmentRepository.findAllByBookingId(booking.getId());
            if (assignments != null) {
                for (OperatorJobAssignment assignment : assignments) {
                    OperatorAssignmentStatus currentStatus = assignment.getAssignmentStatus();
                    // Clean up active assignments only; never mutate terminal assignments (COMPLETED, REJECTED, CANCELLED)
                    if (currentStatus == OperatorAssignmentStatus.ASSIGNED
                            || currentStatus == OperatorAssignmentStatus.ACCEPTED
                            || currentStatus == OperatorAssignmentStatus.TRAVELING
                            || currentStatus == OperatorAssignmentStatus.REACHED
                            || currentStatus == OperatorAssignmentStatus.IN_PROGRESS
                            || currentStatus == OperatorAssignmentStatus.PAUSED) {

                        assignment.setAssignmentStatus(OperatorAssignmentStatus.CANCELLED);
                        operatorJobAssignmentRepository.save(assignment);

                        if (operatorLocationRepository != null) {
                            try {
                                operatorLocationRepository.deactivateTrackingForAssignment(assignment.getId());
                            } catch (Exception e) {
                                // Fallback to maintain transaction integrity
                            }
                        }

                        if (assignment.getOperator() != null && assignment.getOperator().getId() != null) {
                            Long opId = assignment.getOperator().getId();
                            notifiedOperatorIds.add(opId);
                            try {
                                notificationService.sendNotification(
                                        "OPERATOR",
                                        opId,
                                        "Job Assignment Cancelled",
                                        "Job assignment for booking #" + booking.getId() + " has been cancelled.",
                                        "JOB_CANCELLED",
                                        booking.getId()
                                );
                            } catch (Exception e) {
                                // Fallback to maintain transaction integrity
                            }
                        }
                    }
                }
            }
        }

        // If booking held direct operator reference and wasn't notified yet, notify them
        if (booking.getOperator() != null && booking.getOperator().getId() != null) {
            Long directOpId = booking.getOperator().getId();
            if (!notifiedOperatorIds.contains(directOpId)) {
                try {
                    notificationService.sendNotification(
                            "OPERATOR",
                            directOpId,
                            "Job Assignment Cancelled",
                            "Job assignment for booking #" + booking.getId() + " has been cancelled.",
                            "JOB_CANCELLED",
                            booking.getId()
                    );
                } catch (Exception e) {
                    // Fallback to maintain transaction integrity
                }
            }
        }
    }
}
