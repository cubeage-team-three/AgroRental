package com.agrorental.farmer;

import com.agrorental.common.exception.GlobalExceptionHandler;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.farmer.controller.FarmController;
import com.agrorental.farmer.dto.FarmCreateRequest;
import com.agrorental.farmer.dto.FarmResponse;
import com.agrorental.farmer.service.FarmService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FarmController Standalone MockMvc Unit Tests")
class FarmControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FarmService farmService;

    @InjectMocks
    private FarmController farmController;

    private FarmResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(farmController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sampleResponse = FarmResponse.builder()
                .id(1L)
                .farmerId(10L)
                .farmName("Green Valley Farm")
                .village("Khed")
                .taluka("Khed")
                .district("Pune")
                .state("Maharashtra")
                .latitude(18.8500)
                .longitude(73.9100)
                .farmArea(new BigDecimal("12.5"))
                .cropType("Wheat")
                .build();
    }

    @Test
    @DisplayName("POST /api/farmers/farms - Should create farm and return HTTP 201")
    void shouldCreateFarm() throws Exception {
        when(farmService.createFarm(any(FarmCreateRequest.class))).thenReturn(sampleResponse);

        String jsonPayload = """
            {
              "farmerId": 10,
              "farmName": "Green Valley Farm",
              "village": "Khed",
              "taluka": "Khed",
              "district": "Pune",
              "state": "Maharashtra",
              "latitude": 18.8500,
              "longitude": 73.9100,
              "farmArea": 12.5,
              "cropType": "Wheat"
            }
            """;

        mockMvc.perform(post("/api/farmers/farms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.farmName").value("Green Valley Farm"));
    }

    @Test
    @DisplayName("GET /api/farmers/farms - Should return farm list and HTTP 200")
    void shouldGetFarms() throws Exception {
        when(farmService.getFarmsByFarmerId(10L)).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/farmers/farms?farmerId=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].farmName").value("Green Valley Farm"));
    }

    @Test
    @DisplayName("GET /api/farmers/farms/{id} - Should return HTTP 404 when not found")
    void shouldReturn404WhenFarmNotFound() throws Exception {
        when(farmService.getFarmById(99L)).thenThrow(new ResourceNotFoundException("Farm not found with ID: 99"));

        mockMvc.perform(get("/api/farmers/farms/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
