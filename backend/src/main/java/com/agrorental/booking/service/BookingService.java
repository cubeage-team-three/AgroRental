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
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.partner.entity.Partner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    public BookingService(
            BookingRepository bookingRepository,
            EquipmentRepository equipmentRepository,
            OperatorRepository operatorRepository,
            FarmRepository farmRepository,
            NotificationService notificationService,
            BookingMapper bookingMapper) {
        this.bookingRepository = bookingRepository;
        this.equipmentRepository = equipmentRepository;
        this.operatorRepository = operatorRepository;
        this.farmRepository = farmRepository;
        this.notificationService = notificationService;
        this.bookingMapper = bookingMapper;
    }

    /**
     * Creates a new machinery reservation for a farmer.
     *
     * @param request Booking creation payload
     * @return Populated BookingResponse DTO
     */
    @Transactional
    public BookingResponse createBooking(BookingCreateRequest request) {
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
                List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED),
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
     * Cancels an existing booking and restores equipment availability to AVAILABLE.
     *
     * @param id Booking identifier
     * @return Updated BookingResponse DTO
     */
    @Transactional
    public BookingResponse cancelBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + id));

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BadRequestException("Booking cannot be cancelled in its current state: " + booking.getStatus());
        }

        booking.setStatus(BookingStatus.CANCELLED);

        Equipment equipment = booking.getEquipment();
        if (equipment != null) {
            equipment.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
            equipmentRepository.save(equipment);
        }

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
    @Transactional
    public BookingResponse updateBookingStatus(Long id, BookingStatusUpdateRequest request) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with ID: " + id));

        BookingStatus newStatus = request.getStatus();
        booking.setStatus(newStatus);

        if (newStatus == BookingStatus.COMPLETED || newStatus == BookingStatus.CANCELLED || newStatus == BookingStatus.REJECTED) {
            Equipment equipment = booking.getEquipment();
            if (equipment != null) {
                equipment.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
                equipmentRepository.save(equipment);
            }
        } else if (newStatus == BookingStatus.CONFIRMED) {
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
        }

        Booking saved = bookingRepository.save(booking);

        String equipName = saved.getEquipment() != null ? saved.getEquipment().getName() : "Machinery";
        Long partnerId = saved.getPartner() != null ? saved.getPartner().getId() : null;

        if (newStatus == BookingStatus.CONFIRMED) {
            notificationService.sendNotification("FARMER", saved.getFarmerId(), "Booking Confirmed", "Your booking #" + saved.getId() + " for " + equipName + " has been confirmed.", "BOOKING_CONFIRMED", saved.getId());
        } else if (newStatus == BookingStatus.REJECTED) {
            notificationService.sendNotification("FARMER", saved.getFarmerId(), "Booking Declined", "Your booking #" + saved.getId() + " for " + equipName + " was declined.", "BOOKING_REJECTED", saved.getId());
        } else if (newStatus == BookingStatus.COMPLETED) {
            notificationService.sendNotification("FARMER", saved.getFarmerId(), "Job Completed", "Machinery service for booking #" + saved.getId() + " has been marked completed.", "BOOKING_COMPLETED", saved.getId());
            if (partnerId != null) {
                notificationService.sendNotification("PARTNER", partnerId, "Job Completed", "Rental order #" + saved.getId() + " is completed.", "BOOKING_COMPLETED", saved.getId());
            }
        }

        if (request.getOperatorId() != null) {
            notificationService.sendNotification("OPERATOR", request.getOperatorId(), "New Job Assignment", "You have been assigned to job #" + saved.getId() + " (" + equipName + ").", "OPERATOR_ASSIGNED", saved.getId());
            notificationService.sendNotification("FARMER", saved.getFarmerId(), "Operator Assigned", "An operator has been assigned to your booking #" + saved.getId() + ".", "OPERATOR_ASSIGNED", saved.getId());
        }

        return bookingMapper.toResponse(saved);
    }
}
