package com.agrorental.booking;

import com.agrorental.booking.dto.BookingCreateRequest;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.booking.mapper.BookingMapper;
import com.agrorental.booking.repository.BookingRepository;
import com.agrorental.booking.service.BookingService;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.enums.AvailabilityStatus;
import com.agrorental.equipment.repository.EquipmentRepository;
import com.agrorental.farmer.repository.FarmRepository;
import com.agrorental.notification.service.NotificationService;
import com.agrorental.operator.repository.OperatorRepository;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingDateOverlapTest Unit Tests")
class BookingDateOverlapTest {

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

    @BeforeEach
    void setUp() {
        testEquipment = new Equipment();
        testEquipment.setId(1L);
        testEquipment.setName("Mahindra 575 DI");
        testEquipment.setRentalPrice(BigDecimal.valueOf(1800));
        testEquipment.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        testEquipment.setIsDisabled(false);
    }

    @Test
    @DisplayName("Should reject booking creation when date range overlaps active reservation")
    void testCreateBooking_DateOverlapRejection() {
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = LocalDate.now().plusDays(3);

        BookingCreateRequest request = new BookingCreateRequest();
        request.setEquipmentId(1L);
        request.setFarmerId(10L);
        request.setStartDate(start);
        request.setEndDate(end);

        when(equipmentRepository.findById(1L)).thenReturn(Optional.of(testEquipment));
        when(bookingRepository.existsOverlappingBooking(
                eq(1L),
                eq(List.of(BookingStatus.PENDING, BookingStatus.ACCEPTED, BookingStatus.CONFIRMED, BookingStatus.OPERATOR_ASSIGNED, BookingStatus.ON_THE_WAY, BookingStatus.WORK_STARTED)),
                eq(start),
                eq(end)
        )).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> bookingService.createBooking(request));
        assertTrue(ex.getMessage().contains("already reserved for the selected date range"));
    }
}
