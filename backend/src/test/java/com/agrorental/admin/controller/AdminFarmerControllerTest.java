package com.agrorental.admin.controller;

import com.agrorental.farmer.service.FarmerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import com.agrorental.farmer.dto.FarmerResponse;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.data.web.PageableHandlerMethodArgumentResolver;

@ExtendWith(MockitoExtension.class)
class AdminFarmerControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FarmerService farmerService;

    @InjectMocks
    private AdminFarmerController adminFarmerController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminFarmerController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("AdminFarmerController should have @PreAuthorize restricting access to ROLE_ADMIN")
    void shouldHavePreAuthorizeAnnotation() {
        PreAuthorize annotation = AdminFarmerController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation, "AdminFarmerController is missing @PreAuthorize annotation");
        assertEquals("hasRole('ADMIN')", annotation.value(), "Authorization must be restricted to hasRole('ADMIN')");
    }

    @Test
    @DisplayName("Should list farmers without status filter")
    void shouldListFarmersWithoutStatusFilter() throws Exception {
        Page<FarmerResponse> mockPage = new PageImpl<>(List.of(
                FarmerResponse.builder().farmerId(1L).accountStatus("ACTIVE").build()
        ), PageRequest.of(0, 10), 1);
        
        when(farmerService.getFarmers(eq(null), eq(null), any(Pageable.class))).thenReturn(mockPage);

        mockMvc.perform(get("/api/admin/farmers"))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].farmerId").value(1))
                .andExpect(jsonPath("$.data.content[0].accountStatus").value("ACTIVE"));
        
        verify(farmerService).getFarmers(eq(null), eq(null), any(Pageable.class));
    }

    @Test
    @DisplayName("Should list farmers with ACTIVE status filter")
    void shouldListFarmersWithActiveStatusFilter() throws Exception {
        Page<FarmerResponse> mockPage = new PageImpl<>(List.of(
                FarmerResponse.builder().farmerId(2L).accountStatus("ACTIVE").build()
        ), PageRequest.of(0, 10), 1);
        
        when(farmerService.getFarmers(eq(null), eq("ACTIVE"), any(Pageable.class))).thenReturn(mockPage);

        mockMvc.perform(get("/api/admin/farmers").param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].accountStatus").value("ACTIVE"));
        
        verify(farmerService).getFarmers(eq(null), eq("ACTIVE"), any(Pageable.class));
    }

    @Test
    @DisplayName("Should combine search and status filters")
    void shouldCombineSearchAndStatusFilters() throws Exception {
        Page<FarmerResponse> mockPage = new PageImpl<>(List.of(
                FarmerResponse.builder().farmerId(3L).accountStatus("PENDING").build()
        ), PageRequest.of(0, 10), 1);
        
        when(farmerService.getFarmers(eq("John"), eq("PENDING"), any(Pageable.class))).thenReturn(mockPage);

        mockMvc.perform(get("/api/admin/farmers")
                        .param("search", "John")
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].accountStatus").value("PENDING"));
        
        verify(farmerService).getFarmers(eq("John"), eq("PENDING"), any(Pageable.class));
    }

    @Test
    @DisplayName("Should list farmer with an address in the response")
    void shouldListFarmerWithAddress() throws Exception {
        Page<FarmerResponse> mockPage = new PageImpl<>(List.of(
                FarmerResponse.builder().farmerId(4L).address("123 Farm Lane").build()
        ), PageRequest.of(0, 10), 1);
        
        when(farmerService.getFarmers(eq(null), eq(null), any(Pageable.class))).thenReturn(mockPage);

        mockMvc.perform(get("/api/admin/farmers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].farmerId").value(4))
                .andExpect(jsonPath("$.data.content[0].address").value("123 Farm Lane"));
    }

    @Test
    @DisplayName("Should handle missing or null address in the response safely")
    void shouldListFarmerWithNullAddress() throws Exception {
        Page<FarmerResponse> mockPage = new PageImpl<>(List.of(
                FarmerResponse.builder().farmerId(5L).address(null).build()
        ), PageRequest.of(0, 10), 1);
        
        when(farmerService.getFarmers(eq(null), eq(null), any(Pageable.class))).thenReturn(mockPage);

        mockMvc.perform(get("/api/admin/farmers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].farmerId").value(5))
                .andExpect(jsonPath("$.data.content[0].address").doesNotExist());
    }

    @Test
    @DisplayName("Should successfully reject KYC")
    void shouldRejectKyc() throws Exception {
        FarmerResponse response = FarmerResponse.builder()
                .farmerId(6L)
                .accountStatus("REJECTED")
                .build();
        when(farmerService.rejectFarmerKyc(6L)).thenReturn(response);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/admin/farmers/6/reject-kyc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accountStatus").value("REJECTED"))
                .andExpect(jsonPath("$.message").value("Farmer KYC rejected successfully"));
    }
}
