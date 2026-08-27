package com.agrorental.booking;

import com.agrorental.booking.dto.BookingResponse;
import com.agrorental.booking.dto.BookingStatusUpdateRequest;
import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.booking.mapper.BookingMapper;
import com.agrorental.booking.repository.BookingRepository;
import com.agrorental.booking.service.BookingService;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.equipment.enums.AvailabilityStatus;
import com.agrorental.equipment.repository.EquipmentRepository;
import com.agrorental.notification.service.NotificationService;
import com.agrorental.operator.repository.OperatorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Booking Lifecycle & ON_THE_WAY Status Integration Unit Tests")
class BookingStatusLifecycleTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private EquipmentRepository equipmentRepository;

    @Mock
    private OperatorRepository operatorRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private BookingMapper bookingMapper;

    @InjectMocks
    private BookingService bookingService;

    private Booking sampleBooking;
    private Equipment sampleEquipment;

    @BeforeEach
    void setUp() {
        sampleEquipment = Equipment.builder()
                .name("Mahindra 575 DI Tractor")
                .availabilityStatus(AvailabilityStatus.BOOKED)
                .build();
        sampleEquipment.setId(10L);

        sampleBooking = Booking.builder()
                .farmerId(100L)
                .equipment(sampleEquipment)
                .status(BookingStatus.OPERATOR_ASSIGNED)
                .build();
        sampleBooking.setId(1001L);
    }

    @Test
    @DisplayName("Verify BookingStatus enum contains ON_THE_WAY and all required lifecycle states")
    void testBookingStatusEnumContainsRequiredLifecycleStates() {
        List<String> statuses = Arrays.stream(BookingStatus.values())
                .map(Enum::name)
                .toList();

        assertTrue(statuses.contains("PENDING"));
        assertTrue(statuses.contains("ACCEPTED"));
        assertTrue(statuses.contains("CONFIRMED"));
        assertTrue(statuses.contains("OPERATOR_ASSIGNED"));
        assertTrue(statuses.contains("ON_THE_WAY"));
        assertTrue(statuses.contains("WORK_STARTED"));
        assertTrue(statuses.contains("COMPLETED"));
        assertTrue(statuses.contains("CANCELLED"));
        assertTrue(statuses.contains("REJECTED"));
    }

    @Test
    @DisplayName("BookingService updates status to ON_THE_WAY and keeps equipment BOOKED")
    void testUpdateBookingStatusToOnTheWay() {
        when(bookingRepository.findById(1001L)).thenReturn(Optional.of(sampleBooking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));

        BookingResponse mockResponse = BookingResponse.builder()
                .id(1001L)
                .status(BookingStatus.ON_THE_WAY)
                .build();
        when(bookingMapper.toResponse(any(Booking.class))).thenReturn(mockResponse);

        BookingStatusUpdateRequest updateReq = new BookingStatusUpdateRequest();
        updateReq.setStatus(BookingStatus.ON_THE_WAY);

        BookingResponse response = bookingService.updateBookingStatus(1001L, updateReq);

        assertNotNull(response);
        assertEquals(BookingStatus.ON_THE_WAY, response.getStatus());
        assertEquals(BookingStatus.ON_THE_WAY, sampleBooking.getStatus());
        assertEquals(AvailabilityStatus.BOOKED, sampleEquipment.getAvailabilityStatus());
        verify(equipmentRepository, times(1)).save(sampleEquipment);
    }
}
