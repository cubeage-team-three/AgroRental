package com.agrorental.booking.service;

import com.agrorental.booking.dto.BookingCreateRequest;
import com.agrorental.booking.dto.BookingResponse;
import com.agrorental.booking.dto.BookingStatusUpdateRequest;
import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.booking.mapper.BookingMapper;
import com.agrorental.booking.repository.BookingRepository;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.enums.AvailabilityStatus;
import com.agrorental.equipment.repository.EquipmentRepository;
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
    }

    /**
     * Creates a new machinery reservation for a farmer.
     *
     * @param request Booking creation payload
     * @return Populated BookingResponse DTO
     */
    @Transactional
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

        Equipment equipment = equipmentRepository.findById(request.getEquipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found with ID: " + request.getEquipmentId()));

        if (Boolean.TRUE.equals(equipment.getIsDisabled()) ||
                equipment.getAvailabilityStatus() == AvailabilityStatus.INACTIVE ||
                equipment.getAvailabilityStatus() == AvailabilityStatus.UNDER_MAINTENANCE) {
            throw new BadRequestException("Equipment is currently unavailable for rental");
        }

        boolean hasOverlap = bookingRepository.existsOverlappingBooking(
                equipment.getId(),
                List.of(BookingStatus.PENDING, BookingStatus.ACCEPTED, BookingStatus.CONFIRMED, BookingStatus.OPERATOR_ASSIGNED, BookingStatus.ON_THE_WAY, BookingStatus.WORK_STARTED),
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
        Partner partner = equipment.getPartner();

        Booking booking = bookingMapper.toEntity(request, equipment, partner, farm, totalCost);
        if (farm != null && (booking.getDeliveryAddress() == null || booking.getDeliveryAddress().isBlank())) {
            booking.setDeliveryAddress(String.format("%s, %s, %s, %s", farm.getFarmName(), farm.getVillage(), farm.getTaluka(), farm.getDistrict()));
        }

        // Synchronize equipment operational status to BOOKED
        equipment.setAvailabilityStatus(AvailabilityStatus.BOOKED);
        equipmentRepository.save(equipment);

        Booking savedBooking = bookingRepository.save(booking);

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
     * Partner accepts an eligible booking request.
     *
     * @param id Booking identifier
     * @param partnerId Optional partner identifier for authorization
     * @return Updated BookingResponse
     */
    @Transactional
    public BookingResponse acceptBooking(Long id, Long partnerId) {
        Booking booking = bookingRepository.findByIdWithLock(id)
                .or(() -> bookingRepository.findById(id))
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + id));

        if (partnerId != null && booking.getPartner() != null && !booking.getPartner().getId().equals(partnerId)) {
            throw new BadRequestException("Unauthorized: Booking does not belong to the specified partner.");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.REJECTED || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BadRequestException("Cannot accept booking in current status: " + booking.getStatus());
        }

        // Check double-booking protection for active statuses
        boolean hasOverlap = bookingRepository.existsOverlappingBooking(
                booking.getEquipment().getId(),
                List.of(BookingStatus.CONFIRMED, BookingStatus.ACCEPTED, BookingStatus.OPERATOR_ASSIGNED, BookingStatus.ON_THE_WAY, BookingStatus.WORK_STARTED),
                booking.getStartDate(),
                booking.getEndDate()
        );

        if (hasOverlap) {
            throw new BadRequestException("Equipment is already reserved/confirmed for overlapping dates.");
        }

        booking.setStatus(BookingStatus.CONFIRMED);

        Equipment equipment = booking.getEquipment();
        if (equipment != null) {
            equipment.setAvailabilityStatus(AvailabilityStatus.BOOKED);
            equipmentRepository.save(equipment);
        }

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
    @Transactional
    public BookingResponse rejectBooking(Long id, Long partnerId, String rejectionReason) {
        Booking booking = bookingRepository.findByIdWithLock(id)
                .or(() -> bookingRepository.findById(id))
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + id));

        if (partnerId != null && booking.getPartner() != null && !booking.getPartner().getId().equals(partnerId)) {
            throw new BadRequestException("Unauthorized: Booking does not belong to the specified partner.");
        }

        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            throw new BadRequestException("A rejection reason is mandatory when declining a booking.");
        }

        booking.setStatus(BookingStatus.REJECTED);
        booking.setRejectionReason(rejectionReason.trim());

        Equipment equipment = booking.getEquipment();
        if (equipment != null) {
            equipment.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
            equipmentRepository.save(equipment);
        }

        cleanupOperatorAssignments(booking);

        Booking saved = bookingRepository.save(booking);

        String equipName = saved.getEquipment() != null ? saved.getEquipment().getName() : "Machinery";
        notificationService.sendNotification(
                "FARMER",
                saved.getFarmerId(),
                "Booking Declined",
                "Your booking #" + saved.getId() + " for " + equipName + " was declined. Reason: " + rejectionReason.trim(),
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
    @Transactional
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
    @Transactional
    public BookingResponse cancelBooking(Long id, Long requestingFarmerId) {
        Booking booking = bookingRepository.findByIdWithLock(id)
                .or(() -> bookingRepository.findById(id))
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + id));

        if (requestingFarmerId != null && !booking.getFarmerId().equals(requestingFarmerId)) {
            throw new org.springframework.security.access.AccessDeniedException("Access is denied. You do not have permission to cancel another farmer's booking.");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BadRequestException("Booking cannot be cancelled in its current state: " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.CANCELLED);

        Equipment equipment = booking.getEquipment();
        if (equipment != null) {
            equipment.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
            equipmentRepository.save(equipment);
        }

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

        if (newStatus == BookingStatus.OPERATOR_ASSIGNED && request.getOperatorId() == null && booking.getOperator() == null) {
            throw new BadRequestException("Operator ID is required when updating status to OPERATOR_ASSIGNED");
        }

        booking.setStatus(newStatus);

        if (request.getRejectionReason() != null && !request.getRejectionReason().isBlank()) {
            booking.setRejectionReason(request.getRejectionReason().trim());
        }

        if (newStatus == BookingStatus.COMPLETED || newStatus == BookingStatus.CANCELLED || newStatus == BookingStatus.REJECTED) {
            Equipment equipment = booking.getEquipment();
            if (equipment != null) {
                equipment.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
                equipmentRepository.save(equipment);
            }
            if (newStatus == BookingStatus.CANCELLED || newStatus == BookingStatus.REJECTED) {
                cleanupOperatorAssignments(booking);
            }
        } else if (newStatus == BookingStatus.CONFIRMED || newStatus == BookingStatus.ACCEPTED || newStatus == BookingStatus.OPERATOR_ASSIGNED || newStatus == BookingStatus.ON_THE_WAY || newStatus == BookingStatus.WORK_STARTED) {
            Equipment equipment = booking.getEquipment();
            if (equipment != null) {
                equipment.setAvailabilityStatus(AvailabilityStatus.BOOKED);
                equipmentRepository.save(equipment);
            }
        }

        if (request.getOperatorId() != null) {
            Operator operator = operatorRepository.findById(request.getOperatorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Operator not found with ID: " + request.getOperatorId()));
            booking.setOperator(operator);
            if (newStatus == BookingStatus.OPERATOR_ASSIGNED && operatorJobAssignmentRepository != null) {
                OperatorJobAssignment assignment = OperatorJobAssignment.builder()
                        .operator(operator)
                        .booking(booking)
                        .assignmentStatus(OperatorAssignmentStatus.ASSIGNED)
                        .assignedAt(LocalDateTime.now())
                        .assignedBy("ADMIN")
                        .build();
                operatorJobAssignmentRepository.save(assignment);
            }
        }

        Booking saved = bookingRepository.save(booking);

        String equipName = saved.getEquipment() != null ? saved.getEquipment().getName() : "Machinery";
        Long partnerId = saved.getPartner() != null ? saved.getPartner().getId() : null;

        if (newStatus == BookingStatus.CONFIRMED || newStatus == BookingStatus.ACCEPTED) {
            notificationService.sendNotification("FARMER", saved.getFarmerId(), "Booking Confirmed", "Your booking #" + saved.getId() + " for " + equipName + " has been confirmed.", "BOOKING_CONFIRMED", saved.getId());
        } else if (newStatus == BookingStatus.REJECTED) {
            String reasonText = saved.getRejectionReason() != null ? " Reason: " + saved.getRejectionReason() : "";
            notificationService.sendNotification("FARMER", saved.getFarmerId(), "Booking Declined", "Your booking #" + saved.getId() + " for " + equipName + " was declined." + reasonText, "BOOKING_REJECTED", saved.getId());
        } else if (newStatus == BookingStatus.COMPLETED) {
            notificationService.sendNotification("FARMER", saved.getFarmerId(), "Job Completed", "Machinery service for booking #" + saved.getId() + " has been marked completed.", "BOOKING_COMPLETED", saved.getId());
            if (partnerId != null) {
                notificationService.sendNotification("PARTNER", partnerId, "Job Completed", "Rental order #" + saved.getId() + " is completed.", "BOOKING_COMPLETED", saved.getId());
            }
        }

        if (request.getOperatorId() != null && newStatus != BookingStatus.OPERATOR_ASSIGNED) {
            notificationService.sendNotification("OPERATOR", request.getOperatorId(), "New Job Assignment", "You have been assigned to job #" + saved.getId() + " (" + equipName + ").", "OPERATOR_ASSIGNED", saved.getId());
            notificationService.sendNotification("FARMER", saved.getFarmerId(), "Operator Assigned", "An operator has been assigned to your booking #" + saved.getId() + ".", "OPERATOR_ASSIGNED", saved.getId());
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
