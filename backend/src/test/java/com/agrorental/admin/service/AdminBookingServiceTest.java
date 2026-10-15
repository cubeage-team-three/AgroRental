package com.agrorental.admin.service;

import com.agrorental.admin.dto.AdminBookingStatsResponse;
import com.agrorental.booking.dto.BookingResponse;
import com.agrorental.booking.dto.BookingStatusUpdateRequest;
import com.agrorental.booking.entity.Booking;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.booking.mapper.BookingMapper;
import com.agrorental.booking.repository.BookingRepository;
import com.agrorental.booking.service.BookingService;
import com.agrorental.payment.entity.PaymentStatus;
import com.agrorental.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminBookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingMapper bookingMapper;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private AdminBookingService adminBookingService;

    private Booking sampleBooking;
    private BookingResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleBooking = Booking.builder()
                .farmerId(10L)
                .status(BookingStatus.CONFIRMED)
                .totalCost(new BigDecimal("5000.00"))
                .build();
        sampleBooking.setId(101L);

        sampleResponse = BookingResponse.builder()
                .id(101L)
                .farmerId(10L)
                .status(BookingStatus.CONFIRMED)
                .totalCost(new BigDecimal("5000.00"))
                .build();
    }

    @Test
    @DisplayName("getBookings - Queries repository with specification and maps to DTO")
    void getBookings_ReturnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Booking> page = new PageImpl<>(List.of(sampleBooking));

        when(bookingRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(bookingMapper.toResponse(sampleBooking)).thenReturn(sampleResponse);

        Page<BookingResponse> result = adminBookingService.getBookings(
                "tractor", BookingStatus.CONFIRMED, null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(101L, result.getContent().get(0).getId());
        verify(bookingRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("getStats - Aggregates counts and financial amounts correctly")
    void getStats_ReturnsCorrectAggregates() {
        when(bookingRepository.count()).thenReturn(20L);
        when(bookingRepository.countByStatus(BookingStatus.PENDING)).thenReturn(4L);
        when(bookingRepository.countByStatusIn(anyCollection())).thenReturn(10L);
        when(bookingRepository.countByStatus(BookingStatus.COMPLETED)).thenReturn(6L);
        when(paymentRepository.sumAmountByStatus(PaymentStatus.SUCCESS)).thenReturn(new BigDecimal("80000.00"));
        when(bookingRepository.sumTotalCostByStatusNotIn(anyCollection())).thenReturn(new BigDecimal("95000.00"));

        AdminBookingStatsResponse stats = adminBookingService.getStats();

        assertNotNull(stats);
        assertEquals(20L, stats.getTotalBookings());
        assertEquals(4L, stats.getPendingBookings());
        assertEquals(6L, stats.getCompletedBookings());
        assertEquals(new BigDecimal("80000.00"), stats.getTotalRevenue());
        assertEquals(new BigDecimal("95000.00"), stats.getTotalAmount());
    }

    @Test
    @DisplayName("getStats - When repository sum queries return null, amounts default to ZERO")
    void getStats_WhenSumsReturnNull_DefaultsToZero() {
        when(bookingRepository.count()).thenReturn(0L);
        when(bookingRepository.countByStatus(BookingStatus.PENDING)).thenReturn(0L);
        when(bookingRepository.countByStatusIn(anyCollection())).thenReturn(0L);
        when(bookingRepository.countByStatus(BookingStatus.COMPLETED)).thenReturn(0L);
        when(paymentRepository.sumAmountByStatus(PaymentStatus.SUCCESS)).thenReturn(null);
        when(bookingRepository.sumTotalCostByStatusNotIn(anyCollection())).thenReturn(null);

        AdminBookingStatsResponse stats = adminBookingService.getStats();

        assertNotNull(stats);
        assertEquals(0L, stats.getTotalBookings());
        assertEquals(BigDecimal.ZERO, stats.getTotalRevenue());
        assertEquals(BigDecimal.ZERO, stats.getTotalAmount());
    }

    @Test
    @DisplayName("getBookings - Executes query with date filters and numeric ID search")
    void getBookings_WithDateFiltersAndNumericSearch_ExecutesSuccessfully() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Booking> page = new PageImpl<>(List.of(sampleBooking));

        when(bookingRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(bookingMapper.toResponse(sampleBooking)).thenReturn(sampleResponse);

        Page<BookingResponse> result = adminBookingService.getBookings(
                "101",
                BookingStatus.CONFIRMED,
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31),
                pageable
        );

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(bookingRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("getBookings - When repository returns empty page, returns empty BookingResponse page")
    void getBookings_WhenEmpty_ReturnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Booking> emptyPage = new PageImpl<>(List.of());

        when(bookingRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(emptyPage);

        Page<BookingResponse> result = adminBookingService.getBookings(
                null, null, null, null, pageable
        );

        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    @DisplayName("getBookingById - Delegates to bookingService")
    void getBookingById_DelegatesToBookingService() {
        when(bookingService.getBookingById(101L)).thenReturn(sampleResponse);

        BookingResponse result = adminBookingService.getBookingById(101L);

        assertEquals(101L, result.getId());
        verify(bookingService).getBookingById(101L);
    }

    @Test
    @DisplayName("updateBookingStatus - Delegates to bookingService")
    void updateBookingStatus_DelegatesToBookingService() {
        BookingStatusUpdateRequest req = new BookingStatusUpdateRequest();
        req.setStatus(BookingStatus.COMPLETED);
        when(bookingService.updateBookingStatus(101L, req)).thenReturn(sampleResponse);

        BookingResponse result = adminBookingService.updateBookingStatus(101L, req);

        assertNotNull(result);
        verify(bookingService).updateBookingStatus(101L, req);
    }

    @Test
    @DisplayName("cancelBooking - Delegates to bookingService")
    void cancelBooking_DelegatesToBookingService() {
        when(bookingService.cancelBooking(101L)).thenReturn(sampleResponse);

        BookingResponse result = adminBookingService.cancelBooking(101L);

        assertNotNull(result);
        verify(bookingService).cancelBooking(101L);
    }
}
