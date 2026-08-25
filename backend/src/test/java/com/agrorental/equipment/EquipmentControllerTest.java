package com.agrorental.equipment;

import com.agrorental.common.exception.GlobalExceptionHandler;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.equipment.controller.EquipmentController;
import com.agrorental.equipment.dto.EquipmentResponse;
import com.agrorental.equipment.dto.EquipmentSearchRequest;
import com.agrorental.equipment.dto.EquipmentSummaryResponse;
import com.agrorental.equipment.enums.AvailabilityStatus;
import com.agrorental.equipment.service.EquipmentService;
import com.agrorental.security.principal.PartnerPrincipal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
        mockMvc = MockMvcBuilders
                .standaloneSetup(equipmentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/equipment/{id} - Should return 200 OK")
    void shouldGetEquipmentById() throws Exception {
        EquipmentResponse response = EquipmentResponse.builder()
                .id(10L)
                .name("Mahindra 575 DI")
                .build();

        when(equipmentService.getEquipmentById(10L)).thenReturn(response);

        mockMvc.perform(get("/api/equipment/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.name").value("Mahindra 575 DI"));
    }

    @Test
    @DisplayName("GET /api/equipment/{id} - Should return 404")
    void shouldReturn404WhenEquipmentNotFound() throws Exception {
        when(equipmentService.getEquipmentById(99L))
                .thenThrow(new ResourceNotFoundException("Equipment not found with ID: 99"));

        mockMvc.perform(get("/api/equipment/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Equipment not found with ID: 99"));
    }

    @Test
    @DisplayName("GET /api/equipment/available - Should return equipment")
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
                .andExpect(jsonPath("$.data[0].id").value(10))
                .andExpect(jsonPath("$.data[0].name").value("Mahindra 575 DI"));
    }

    @Test
    @DisplayName("GET /api/equipment/search - Should return search results")
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
                .andExpect(jsonPath("$.data[0].id").value(10))
                .andExpect(jsonPath("$.data[0].name").value("Mahindra 575 DI"));
    }

    @Test
    @DisplayName("DELETE /api/equipment/{id} - Should delete equipment")
    void shouldDeleteEquipment() throws Exception {
        doNothing().when(equipmentService).deleteEquipment(10L, 1L);

        PartnerPrincipal principal = PartnerPrincipal.builder().id(1L).role("PARTNER").build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_PARTNER"))));

        mockMvc.perform(delete("/api/equipment/10"))
                .andExpect(status().isNoContent());

        verify(equipmentService).deleteEquipment(10L, 1L);
    }
}
