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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

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

    @Test
    @DisplayName("POST /api/equipment - Should create equipment for partner")
    void shouldCreateEquipment() throws Exception {
        EquipmentResponse response = EquipmentResponse.builder()
                .id(100L)
                .name("John Deere 5310 4WD Tractor")
                .partnerId(1L)
                .build();

        when(equipmentService.createEquipment(any())).thenReturn(response);

        PartnerPrincipal principal = PartnerPrincipal.builder().id(1L).role("PARTNER").build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_PARTNER"))));

        String jsonPayload = """
                {
                  "name": "John Deere 5310 4WD Tractor",
                  "category": "TRACTOR",
                  "brand": "John Deere",
                  "model": "5310 4WD",
                  "manufacturingYear": 2026,
                  "capacity": "55 HP",
                  "rentalPrice": 2000,
                  "fuelType": "DIESEL",
                  "description": "Standard tractor listing",
                  "partnerId": 1,
                  "locationAddress": "Shirur MIDC Road, Pune, Maharashtra 412210",
                  "latitude": 18.5204,
                  "longitude": 73.8567,
                  "images": [
                    {
                      "imageUrl": "https://images.unsplash.com/photo-1592878904946-b3cd8ae243d0",
                      "isPrimary": true,
                      "displayOrder": 1
                    }
                  ]
                }
                """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/equipment")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(100))
                .andExpect(jsonPath("$.data.partnerId").value(1));

        verify(equipmentService).createEquipment(any());
    }

    @Test
    @DisplayName("PATCH /api/equipment/{id}/enable - Admin should enable equipment successfully")
    void shouldAllowAdminToEnableEquipment() throws Exception {
        EquipmentResponse response = EquipmentResponse.builder().id(10L).isDisabled(false).build();
        when(equipmentService.enableEquipment(10L)).thenReturn(response);

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "ADMIN_1", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        mockMvc.perform(patch("/api/equipment/10/enable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isDisabled").value(false));

        verify(equipmentService).enableEquipment(10L);
    }

    @Test
    @DisplayName("PATCH /api/equipment/{id}/enable - Partner should enable own equipment")
    void shouldAllowPartnerToEnableOwnEquipment() throws Exception {
        EquipmentResponse response = EquipmentResponse.builder().id(10L).isDisabled(false).build();
        when(equipmentService.enableEquipment(10L, 1L)).thenReturn(response);

        PartnerPrincipal principal = PartnerPrincipal.builder().id(1L).role("PARTNER").build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_PARTNER"))));

        mockMvc.perform(patch("/api/equipment/10/enable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isDisabled").value(false));

        verify(equipmentService).enableEquipment(10L, 1L);
    }

    @Test
    @DisplayName("PATCH /api/equipment/{id}/enable - Unauthorized request rejected without NPE")
    void shouldRejectEnableEquipmentWhenUnauthenticated() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(patch("/api/equipment/10/enable"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));

        verify(equipmentService, never()).enableEquipment(any());
        verify(equipmentService, never()).enableEquipment(any(), any());
    }

    @Test
    @DisplayName("PATCH /api/equipment/{id}/disable - Admin should disable equipment successfully")
    void shouldAllowAdminToDisableEquipment() throws Exception {
        EquipmentResponse response = EquipmentResponse.builder().id(10L).isDisabled(true).build();
        when(equipmentService.disableEquipment(10L)).thenReturn(response);

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "ADMIN_1", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        mockMvc.perform(patch("/api/equipment/10/disable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isDisabled").value(true));

        verify(equipmentService).disableEquipment(10L);
    }

    @Test
    @DisplayName("PATCH /api/equipment/{id}/disable - Partner should disable own equipment")
    void shouldAllowPartnerToDisableOwnEquipment() throws Exception {
        EquipmentResponse response = EquipmentResponse.builder().id(10L).isDisabled(true).build();
        when(equipmentService.disableEquipment(10L, 1L)).thenReturn(response);

        PartnerPrincipal principal = PartnerPrincipal.builder().id(1L).role("PARTNER").build();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_PARTNER"))));

        mockMvc.perform(patch("/api/equipment/10/disable"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isDisabled").value(true));

        verify(equipmentService).disableEquipment(10L, 1L);
    }

    @Test
    @DisplayName("PATCH /api/equipment/{id}/disable - Unauthorized request rejected without NPE")
    void shouldRejectDisableEquipmentWhenUnauthenticated() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(patch("/api/equipment/10/disable"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));

        verify(equipmentService, never()).disableEquipment(any());
        verify(equipmentService, never()).disableEquipment(any(), any());
    }

    @Test
    @DisplayName("DELETE /api/equipment/{id} - Admin should delete equipment successfully")
    void shouldAllowAdminToDeleteEquipment() throws Exception {
        doNothing().when(equipmentService).deleteEquipment(10L);

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "ADMIN_1", null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        mockMvc.perform(delete("/api/equipment/10"))
                .andExpect(status().isNoContent());

        verify(equipmentService).deleteEquipment(10L);
    }

    @Test
    @DisplayName("DELETE /api/equipment/{id} - Unauthorized request rejected without NPE")
    void shouldRejectDeleteEquipmentWhenUnauthenticated() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(delete("/api/equipment/10"))
                .andExpect(status().isUnauthorized());

        verify(equipmentService, never()).deleteEquipment(any());
        verify(equipmentService, never()).deleteEquipment(any(), any());
    }
}
