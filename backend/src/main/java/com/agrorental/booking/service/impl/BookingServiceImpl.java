package com.agrorental.booking.service.impl;

import com.agrorental.booking.dto.BookingRequestDto;
import com.agrorental.booking.dto.BookingResponseDto;
import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.constant.BookingStatus;
import com.agrorental.booking.exception.EquipmentNotAvailableException;
import com.agrorental.booking.repository.BookingRepository;
import com.agrorental.booking.service.BookingService;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.enums.AvailabilityStatus;
import com.agrorental.equipment.repository.EquipmentRepository;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.partner.entity.Partner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final Set<BookingStatus> LOCKED_STATUSES =
            Set.of(BookingStatus.COMPLETED, BookingStatus.CANCELLED);

    private final BookingRepository bookingRepository;
    private final FarmerRepository farmerRepository;
    private final EquipmentRepository equipmentRepository;
    private final OperatorRepository operatorRepository;

    @Override
    @Transactional
    public BookingResponseDto createBooking(BookingRequestDto request) {

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date cannot be before start date");
        }

        Farmer farmer = farmerRepository.findById(request.getFarmerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Farmer not found with id: " + request.getFarmerId()));

        Equipment equipment = equipmentRepository.findById(request.getEquipmentId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Equipment not found with id: " + request.getEquipmentId()));

        Operator operator = null;
        if (request.getOperatorId() != null) {
            operator = operatorRepository.findById(request.getOperatorId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Operator not found with id: " + request.getOperatorId()));
        }

        // The partner is always the equipment's own owner — never taken from
        // client input — so a caller can't book equipment under a partner it
        // doesn't actually belong to.
        Partner partner = equipment.getPartner();

        assertEquipmentAvailable(equipment, request.getStartDate(), request.getEndDate());

        BigDecimal totalCost = calculateTotalCost(equipment, request.getTotalAcreage());

        Booking booking = Booking.builder()
                .farmer(farmer)
                .equipment(equipment)
                .partner(partner)
                .operator(operator)
                .bookingDate(LocalDate.now())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .totalAcreage(request.getTotalAcreage())
                .totalCost(totalCost)
                .status(BookingStatus.PENDING)
                .build();

        Booking saved = bookingRepository.save(booking);

        markEquipmentBooked(equipment);

        return toResponseDto(saved);
    }

    /**
     * Two checks: the equipment's own state flag, and a date-range overlap
     * query against existing (non-cancelled) bookings for that equipment.
     * Both are required — a machine can be flagged AVAILABLE in general but
     * still have a future booking that overlaps the requested window.
     */
    private void assertEquipmentAvailable(Equipment equipment, LocalDate startDate, LocalDate endDate) {
        if (equipment.getAvailabilityStatus() != AvailabilityStatus.AVAILABLE
                || Boolean.TRUE.equals(equipment.getIsDisabled())) {
            throw new EquipmentNotAvailableException(
                    "Equipment '" + equipment.getName() + "' is not currently available for booking");
        }

        boolean overlapping = bookingRepository.existsOverlappingBooking(
                equipment.getId(), startDate, endDate);

        if (overlapping) {
            throw new EquipmentNotAvailableException(
                    "Equipment '" + equipment.getName()
                            + "' is already booked for an overlapping date range");
        }
    }

    /**
     * Equipment.rentalPrice is priced per acre (matches the platform's
     * acre-based pricing model), not per day — see the class-level note on
     * BookingServiceImpl for why this differs from a dailyRate * days
     * formula.
     */
    private BigDecimal calculateTotalCost(Equipment equipment, BigDecimal totalAcreage) {
        return equipment.getRentalPrice().multiply(totalAcreage);
    }

    private void markEquipmentBooked(Equipment equipment) {
        equipment.setAvailabilityStatus(AvailabilityStatus.BOOKED);
        equipmentRepository.save(equipment);
    }

    private void releaseEquipment(Equipment equipment) {
        equipment.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        equipmentRepository.save(equipment);
    }

    @Override
    public BookingResponseDto getBookingById(Long id) {
        return toResponseDto(findBookingOrThrow(id));
    }

    @Override
    public List<BookingResponseDto> getAllBookings() {
        return bookingRepository.findAll().stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    public List<BookingResponseDto> getBookingsByFarmer(Long farmerId) {
        return bookingRepository.findAllByFarmerId(farmerId).stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    public List<BookingResponseDto> getBookingsByEquipment(Long equipmentId) {
        return bookingRepository.findAllByEquipmentId(equipmentId).stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public BookingResponseDto updateBookingStatus(Long id, BookingStatus status) {
        Booking booking = findBookingOrThrow(id);

        if (LOCKED_STATUSES.contains(booking.getStatus())) {
            throw new BadRequestException(
                    "Booking is already " + booking.getStatus()
                            + " and its status cannot be changed further");
        }

        booking.setStatus(status);

        if (LOCKED_STATUSES.contains(status)) {
            releaseEquipment(booking.getEquipment());
        }

        return toResponseDto(bookingRepository.save(booking));
    }

    private Booking findBookingOrThrow(Long id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + id));
    }

    private BookingResponseDto toResponseDto(Booking booking) {
        Equipment equipment = booking.getEquipment();
        Farmer farmer = booking.getFarmer();
        Partner partner = booking.getPartner();
        Operator operator = booking.getOperator();

        return BookingResponseDto.builder()
                .id(booking.getId())
                .farmerId(farmer.getId())
                .farmerName(farmer.getFullName())
                .equipmentId(equipment.getId())
                .equipmentName(equipment.getName())
                .partnerId(partner.getId())
                .partnerName(partner.getFullName())
                .operatorId(operator != null ? operator.getId() : null)
                .operatorName(operator != null ? operator.getFullName() : null)
                .bookingDate(booking.getBookingDate())
                .startDate(booking.getStartDate())
                .endDate(booking.getEndDate())
                .totalAcreage(booking.getTotalAcreage())
                .totalCost(booking.getTotalCost())
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }
}
