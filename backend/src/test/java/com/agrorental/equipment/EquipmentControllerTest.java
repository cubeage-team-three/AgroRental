package com.agrorental.equipment;

import com.agrorental.common.exception.GlobalExceptionHandler;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.equipment.controller.EquipmentController;
import com.agrorental.equipment.dto.*;
import com.agrorental.equipment.enums.AvailabilityStatus;
import com.agrorental.equipment.service.EquipmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EquipmentController Standalone MockMvc Unit Tests")
class EquipmentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private EquipmentService equipmentService;

    @InjectMocks
    private EquipmentController equipmentController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(equipmentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/equipment/{id} - Should return 200 OK when equipment exists")
    void shouldGetEquipmentById() throws Exception {
        EquipmentResponse response = EquipmentResponse.builder()
                .id(10L)
                .name("Mahindra 575 DI")
                .build();

        when(equipmentService.getEquipmentById(10L)).thenReturn(response);

        mockMvc.perform(get("/api/equipment/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10));
    }

    @Test
    @DisplayName("GET /api/equipment/{id} - Should return 404 NOT FOUND when equipment does not exist")
    void shouldReturn404WhenEquipmentNotFound() throws Exception {
        when(equipmentService.getEquipmentById(99L)).thenThrow(new ResourceNotFoundException("Equipment not found with ID: 99"));

        mockMvc.perform(get("/api/equipment/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Equipment not found with ID: 99"));
    }

    @Test
    @DisplayName("GET /api/equipment/available - Should return 200 OK with discoverable equipment list")
    void shouldGetDiscoverableEquipment() throws Exception {
        EquipmentSummaryResponse summary = EquipmentSummaryResponse.builder()
                .id(10L)
                .name("Mahindra 575 DI")
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .build();

        when(equipmentService.getDiscoverableEquipment()).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/equipment/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(10));
    }

    @Test
    @DisplayName("GET /api/equipment/search - Should return 200 OK with search results")
    void shouldSearchEquipment() throws Exception {
        EquipmentSummaryResponse summary = EquipmentSummaryResponse.builder()
                .id(10L)
                .name("Mahindra 575 DI")
                .build();

        when(equipmentService.searchEquipment(any(EquipmentSearchRequest.class))).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/equipment/search")
                        .param("category", "TRACTOR")
                        .param("minPrice", "500")
                        .param("maxPrice", "2000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(10));
    }

    @Test
    @DisplayName("DELETE /api/equipment/{id} - Should return 204 NO CONTENT")
    void shouldDeleteEquipmentAndReturn204() throws Exception {
        doNothing().when(equipmentService).deleteEquipment(10L);

        mockMvc.perform(delete("/api/equipment/10"))
                .andExpect(status().isNoContent());
    }
}
