package com.agrorental.admin.controller;

import com.agrorental.admin.dto.AdminBookingStatsResponse;
import com.agrorental.admin.service.AdminBookingService;
import com.agrorental.booking.dto.BookingResponse;
import com.agrorental.booking.dto.BookingStatusUpdateRequest;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminBookingControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdminBookingService adminBookingService;

    @InjectMocks
    private AdminBookingController adminBookingController;

    private BookingResponse sampleBooking;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminBookingController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sampleBooking = BookingResponse.builder()
                .id(101L)
                .farmerId(10L)
                .farmerName("Ramesh Patil")
                .farmerMobile("9876543210")
                .equipmentId(5L)
                .equipmentName("John Deere 5050 D")
                .partnerId(2L)
                .partnerName("Suresh Agro")
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(4))
                .totalCost(new BigDecimal("12000.00"))
                .status(BookingStatus.CONFIRMED)
                .paymentStatus("SUCCESS")
                .build();
    }

    @Test
    @DisplayName("AdminBookingController should have @PreAuthorize restricting access to ROLE_ADMIN")
    void shouldHavePreAuthorizeAnnotation() {
        PreAuthorize annotation = AdminBookingController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation, "AdminBookingController is missing @PreAuthorize annotation");
        assertEquals("hasRole('ADMIN')", annotation.value(), "Authorization must be restricted to hasRole('ADMIN')");
    }

    @Test
    @DisplayName("GET /api/admin/bookings - Returns HTTP 200 OK with paginated list")
    void getBookings_Returns200OK() throws Exception {
        Page<BookingResponse> page = new PageImpl<>(List.of(sampleBooking), org.springframework.data.domain.PageRequest.of(0, 10), 1);
        when(adminBookingService.getBookings(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/bookings")
                        .param("page", "0")
                        .param("size", "10")
                        .param("search", "John"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(101))
                .andExpect(jsonPath("$.data.content[0].farmerName").value("Ramesh Patil"))
                .andExpect(jsonPath("$.data.content[0].equipmentName").value("John Deere 5050 D"))
                .andExpect(jsonPath("$.data.content[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.content[0].paymentStatus").value("SUCCESS"));

        verify(adminBookingService).getBookings(eq("John"), eq(null), eq(null), eq(null), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /api/admin/bookings - With status, startDate and endDate filters")
    void getBookings_WithFilters_ReturnsFilteredList() throws Exception {
        Page<BookingResponse> page = new PageImpl<>(List.of(sampleBooking), org.springframework.data.domain.PageRequest.of(0, 10), 1);
        when(adminBookingService.getBookings(eq("tractor"), eq(BookingStatus.CONFIRMED), eq(LocalDate.of(2026, 10, 1)), eq(LocalDate.of(2026, 10, 31)), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/admin/bookings")
                        .param("search", "tractor")
                        .param("status", "CONFIRMED")
                        .param("startDate", "2026-10-01")
                        .param("endDate", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(101));

        verify(adminBookingService).getBookings(
                eq("tractor"),
                eq(BookingStatus.CONFIRMED),
                eq(LocalDate.of(2026, 10, 1)),
                eq(LocalDate.of(2026, 10, 31)),
                any(Pageable.class)
        );
    }

    @Test
    @DisplayName("GET /api/admin/bookings/stats - Returns HTTP 200 OK with aggregate stats")
    void getStats_Returns200OK() throws Exception {
        AdminBookingStatsResponse stats = AdminBookingStatsResponse.builder()
                .totalBookings(50)
                .pendingBookings(5)
                .activeBookings(15)
                .completedBookings(25)
                .cancelledBookings(5)
                .totalRevenue(new BigDecimal("250000.00"))
                .totalAmount(new BigDecimal("300000.00"))
                .build();

        when(adminBookingService.getStats()).thenReturn(stats);

        mockMvc.perform(get("/api/admin/bookings/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalBookings").value(50))
                .andExpect(jsonPath("$.data.activeBookings").value(15))
                .andExpect(jsonPath("$.data.completedBookings").value(25))
                .andExpect(jsonPath("$.data.totalRevenue").value(250000.00));
    }

    @Test
    @DisplayName("GET /api/admin/bookings/{id} - Returns HTTP 200 OK with booking detail")
    void getBookingById_Returns200OK() throws Exception {
        when(adminBookingService.getBookingById(101L)).thenReturn(sampleBooking);

        mockMvc.perform(get("/api/admin/bookings/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(101))
                .andExpect(jsonPath("$.data.farmerName").value("Ramesh Patil"));
    }

    @Test
    @DisplayName("PATCH /api/admin/bookings/{id}/status - Updates status and returns HTTP 200 OK")
    void updateBookingStatus_Returns200OK() throws Exception {
        sampleBooking.setStatus(BookingStatus.COMPLETED);
        when(adminBookingService.updateBookingStatus(eq(101L), any(BookingStatusUpdateRequest.class)))
                .thenReturn(sampleBooking);

        String payload = """
                {
                  "status": "COMPLETED"
                }
                """;

        mockMvc.perform(patch("/api/admin/bookings/101/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("PATCH /api/admin/bookings/{id}/cancel - Cancels booking and returns HTTP 200 OK")
    void cancelBooking_Returns200OK() throws Exception {
        sampleBooking.setStatus(BookingStatus.CANCELLED);
        when(adminBookingService.cancelBooking(101L)).thenReturn(sampleBooking);

        mockMvc.perform(patch("/api/admin/bookings/101/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }
}
