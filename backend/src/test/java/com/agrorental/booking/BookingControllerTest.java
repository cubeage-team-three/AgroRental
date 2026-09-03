package com.agrorental.booking;

import com.agrorental.booking.controller.BookingController;
import com.agrorental.booking.dto.BookingResponse;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.booking.service.BookingService;
import com.agrorental.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BookingControllerTest {

    private MockMvc mockMvc;
    private BookingService bookingService;
    private BookingResponse sampleResponse;

    @BeforeEach
    void setUp() {
        bookingService = mock(BookingService.class);
        BookingController bookingController = new BookingController(bookingService);
        mockMvc = MockMvcBuilders
                .standaloneSetup(bookingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver())
                .build();

        sampleResponse = BookingResponse.builder()
                .id(100L)
                .farmerId(10L)
                .equipmentId(1L)
                .equipmentName("Mahindra 575 DI Tractor")
                .partnerId(5L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .totalCost(new BigDecimal("4500.00"))
                .status(BookingStatus.CONFIRMED)
                .build();
    }

    @Test
    @DisplayName("POST /api/bookings - Returns HTTP 201 Created and response payload")
    void createBooking_Returns201Created() throws Exception {
        String jsonPayload = """
            {
              "equipmentId": 1,
              "farmerId": 10,
              "startDate": "%s",
              "endDate": "%s",
              "deliveryAddress": "Farm Location 5"
            }
            """.formatted(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

        when(bookingService.createBooking(any())).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(100))
                .andExpect(jsonPath("$.data.equipmentName").value("Mahindra 575 DI Tractor"));
    }

    @Test
    @DisplayName("GET /api/bookings/{id} - Returns HTTP 200 OK")
    void getBookingById_Returns200OK() throws Exception {
        when(bookingService.getBookingById(100L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/bookings/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(100));
    }

    @Test
    @DisplayName("GET /api/bookings/farmer/{farmerId} - Returns HTTP 200 OK")
    void getBookingsByFarmer_Returns200OK() throws Exception {
        when(bookingService.getBookingsByFarmer(10L)).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/bookings/farmer/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(100));
    }

    @Test
    @DisplayName("PATCH /api/bookings/{id}/cancel - Returns HTTP 200 OK")
    void cancelBooking_Returns200OK() throws Exception {
        sampleResponse.setStatus(BookingStatus.CANCELLED);
        when(bookingService.cancelBooking(org.mockito.ArgumentMatchers.eq(100L), org.mockito.ArgumentMatchers.nullable(Long.class))).thenReturn(sampleResponse);

        mockMvc.perform(patch("/api/bookings/100/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }
}
