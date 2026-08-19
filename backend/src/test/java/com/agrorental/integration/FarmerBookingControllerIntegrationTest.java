package com.agrorental.integration;

import com.agrorental.booking.dto.BookingResponse;
import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.booking.service.BookingService;
import com.agrorental.common.exception.GlobalExceptionHandler;
import com.agrorental.farmer.controller.FarmerBookingController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Farmer Booking Integration Controller Tests")
class FarmerBookingControllerIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private FarmerBookingController farmerBookingController;

    private BookingResponse mockResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(farmerBookingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockResponse = BookingResponse.builder()
                .id(1L)
                .farmerId(10L)
                .equipmentId(5L)
                .equipmentName("John Deere 5050D Tractor")
                .equipmentCategory("TRACTOR")
                .partnerId(2L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .totalCost(BigDecimal.valueOf(3600))
                .status(BookingStatus.PENDING)
                .deliveryAddress("Sunrise Agro Farm, Pune")
                .notes("Work Type: Ploughing")
                .build();
    }

    @Test
    void shouldCreateFarmerBooking() throws Exception {
        when(bookingService.createBooking(any())).thenReturn(mockResponse);

        String payload = """
            {
              "equipmentId": 5,
              "farmerId": 10,
              "startDate": "%s",
              "endDate": "%s",
              "deliveryAddress": "Sunrise Agro Farm, Pune",
              "notes": "Work Type: Ploughing"
            }
            """.formatted(LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

        mockMvc.perform(post("/api/farmers/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.equipmentName").value("John Deere 5050D Tractor"));
    }

    @Test
    void shouldGetFarmerBookings() throws Exception {
        when(bookingService.getBookingsByFarmer(10L)).thenReturn(List.of(mockResponse));

        mockMvc.perform(get("/api/farmers/bookings?farmerId=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1));
    }

    @Test
    void shouldCancelFarmerBooking() throws Exception {
        mockResponse.setStatus(BookingStatus.CANCELLED);
        when(bookingService.cancelBooking(1L)).thenReturn(mockResponse);

        mockMvc.perform(put("/api/farmers/bookings/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }
}
