package com.agrorental.booking;

import com.agrorental.booking.dto.BookingCreateRequest;
import com.agrorental.booking.dto.BookingResponse;
import com.agrorental.booking.dto.BookingStatusUpdateRequest;
import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.booking.mapper.BookingMapper;
import com.agrorental.booking.repository.BookingRepository;
import com.agrorental.booking.service.BookingService;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.enums.AvailabilityStatus;
import com.agrorental.equipment.repository.EquipmentRepository;
import com.agrorental.farmer.entity.Farm;
import com.agrorental.farmer.repository.FarmRepository;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.partner.entity.Partner;
import com.agrorental.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private OperatorRepository operatorRepository;

    @Mock
    private FarmRepository farmRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private BookingMapper bookingMapper;

    @InjectMocks
    private BookingService bookingService;

    private Equipment testEquipment;
    private Partner testPartner;
    private Booking testBooking;
    private Farm testFarm;
    private BookingCreateRequest createRequest;

    @BeforeEach
    void setUp() {
        testPartner = Partner.builder()
                .fullName("Ramesh Patel")
                .mobileNumber("9876543210")
                .build();
        testPartner.setId(10L);

        testEquipment = Equipment.builder()
                .name("Mahindra 575 DI Tractor")
                .rentalPrice(new BigDecimal("1500.00"))
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .partner(testPartner)
                .isDisabled(false)
                .build();
        testEquipment.setId(1L);

        testFarm = Farm.builder()
                .farmerId(100L)
                .farmName("Golden Acre Farm")
                .village("Khed")
                .taluka("Pune")
                .district("Pune")
                .state("Maharashtra")
                .farmArea(new BigDecimal("5.5"))
                .build();
        testFarm.setId(20L);

        createRequest = BookingCreateRequest.builder()
                .equipmentId(1L)
                .farmerId(100L)
                .farmId(20L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .deliveryAddress("Village Farm Plot 4")
                .build();

        testBooking = Booking.builder()
                .farmerId(100L)
                .farm(testFarm)
                .equipment(testEquipment)
                .partner(testPartner)
                .startDate(createRequest.getStartDate())
                .endDate(createRequest.getEndDate())
                .totalCost(new BigDecimal("4500.00"))
                .status(BookingStatus.CONFIRMED)
                .build();
        testBooking.setId(500L);
    }

    @Test
    @DisplayName("createBooking: Successfully creates reservation with Farm and sets equipment status to BOOKED")
    void createBooking_Success() {
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(testEquipment));
        when(bookingRepository.existsOverlappingBooking(anyLong(), anyList(), any(), any())).thenReturn(false);
        when(farmRepository.findById(20L)).thenReturn(Optional.of(testFarm));
        when(bookingMapper.toEntity(any(), any(), any(), any(), any())).thenReturn(testBooking);
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        when(bookingMapper.toResponse(any(Booking.class))).thenReturn(
                BookingResponse.builder()
                        .id(500L)
                        .farmerId(100L)
                        .farmId(20L)
                        .farmName("Golden Acre Farm")
                        .equipmentId(1L)
                        .partnerId(10L)
                        .totalCost(new BigDecimal("4500.00"))
                        .status(BookingStatus.CONFIRMED)
                        .build()
        );

        BookingResponse response = bookingService.createBooking(createRequest);

        assertNotNull(response);
        assertEquals(500L, response.getId());
        assertEquals(20L, response.getFarmId());
        assertEquals("Golden Acre Farm", response.getFarmName());
        assertEquals(AvailabilityStatus.BOOKED, testEquipment.getAvailabilityStatus());
        verify(equipmentRepository).save(testEquipment);
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    @DisplayName("createBooking: Throws ResourceNotFoundException when provided farmId does not exist")
    void createBooking_FarmNotFound_ThrowsException() {
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(testEquipment));
        when(bookingRepository.existsOverlappingBooking(anyLong(), anyList(), any(), any())).thenReturn(false);
        when(farmRepository.findById(20L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> bookingService.createBooking(createRequest));

        assertTrue(exception.getMessage().contains("Farm not found with ID: 20"));
    }

    @Test
    @DisplayName("createBooking: Throws BadRequestException when farm belongs to a different farmer")
    void createBooking_FarmOwnershipMismatch_ThrowsException() {
        testFarm.setFarmerId(999L); // Different farmer
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(testEquipment));
        when(bookingRepository.existsOverlappingBooking(anyLong(), anyList(), any(), any())).thenReturn(false);
        when(farmRepository.findById(20L)).thenReturn(Optional.of(testFarm));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> bookingService.createBooking(createRequest));

        assertTrue(exception.getMessage().contains("Farmer is not authorized to use this farm"));
    }

    @Test
    @DisplayName("createBooking: Throws BadRequestException when end date is before start date")
    void createBooking_InvalidDateRange_ThrowsException() {
        createRequest.setStartDate(LocalDate.now().plusDays(5));
        createRequest.setEndDate(LocalDate.now().plusDays(2));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> bookingService.createBooking(createRequest));

        assertTrue(exception.getMessage().contains("End date cannot be before start date"));
    }

    @Test
    @DisplayName("createBooking: Throws BadRequestException when overlapping reservation exists")
    void createBooking_OverlapConflict_ThrowsException() {
        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(testEquipment));
        when(bookingRepository.existsOverlappingBooking(anyLong(), anyList(), any(), any())).thenReturn(true);

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> bookingService.createBooking(createRequest));

        assertTrue(exception.getMessage().contains("already reserved"));
    }

    @Test
    @DisplayName("cancelBooking: Restores equipment availability status to AVAILABLE")
    void cancelBooking_Success() {
        testEquipment.setAvailabilityStatus(AvailabilityStatus.BOOKED);
        when(bookingRepository.findById(500L)).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        when(bookingMapper.toResponse(any(Booking.class))).thenReturn(
                BookingResponse.builder().id(500L).status(BookingStatus.CANCELLED).build()
        );

        BookingResponse response = bookingService.cancelBooking(500L);

        assertNotNull(response);
        assertEquals(BookingStatus.CANCELLED, testBooking.getStatus());
        assertEquals(AvailabilityStatus.AVAILABLE, testEquipment.getAvailabilityStatus());
        verify(equipmentRepository).save(testEquipment);
    }

    @Test
    @DisplayName("updateBookingStatus: Restores equipment status to AVAILABLE when completed")
    void updateBookingStatus_Complete_RestoresEquipmentStatus() {
        testEquipment.setAvailabilityStatus(AvailabilityStatus.BOOKED);
        when(bookingRepository.findById(500L)).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        when(bookingMapper.toResponse(any(Booking.class))).thenReturn(
                BookingResponse.builder().id(500L).status(BookingStatus.COMPLETED).build()
        );

        BookingStatusUpdateRequest updateRequest = BookingStatusUpdateRequest.builder()
                .status(BookingStatus.COMPLETED)
                .build();

        BookingResponse response = bookingService.updateBookingStatus(500L, updateRequest);

        assertNotNull(response);
        assertEquals(AvailabilityStatus.AVAILABLE, testEquipment.getAvailabilityStatus());
        verify(equipmentRepository).save(testEquipment);
    }
}
