package com.agrorental.security;

import com.agrorental.common.exception.GlobalExceptionHandler;
import com.agrorental.farmer.controller.FarmController;
import com.agrorental.farmer.dto.FarmCreateRequest;
import com.agrorental.farmer.dto.FarmResponse;
import com.agrorental.farmer.entity.Farm;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.farmer.service.FarmService;
import com.agrorental.security.jwt.JwtAuthenticationFilter;
import com.agrorental.security.jwt.JwtService;
import com.agrorental.security.principal.FarmerPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Farm Management Security & Ownership Authorization Tests")
class FarmSecurityIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private JwtService jwtService;

    @Mock
    private FarmerRepository farmerRepository;

    @Mock
    private FarmService farmService;

    @InjectMocks
    private FarmController farmController;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService, null, null, null, null, farmerRepository);

        HandlerMethodArgumentResolver principalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().equals(FarmerPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest,
                                          WebDataBinderFactory binderFactory) {
                var auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getPrincipal() instanceof FarmerPrincipal) {
                    return auth.getPrincipal();
                }
                return null;
            }
        };

        HandlerMethodArgumentResolver authResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().equals(org.springframework.security.core.Authentication.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest,
                                          WebDataBinderFactory binderFactory) {
                return SecurityContextHolder.getContext().getAuthentication();
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(farmController)
                .addFilter(jwtAuthenticationFilter)
                .setCustomArgumentResolvers(principalResolver, authResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Farmer A creates farm -> Saved under authenticated Farmer A ID (10L)")
    void testFarmerCreatesFarmUnderAuthenticatedId() throws Exception {
        Farmer farmerA = Farmer.builder().fullName("Ramesh Kumar").mobileNumber("9876543210").accountStatus("ACTIVE").build();
        farmerA.setId(10L);
        farmerA.setActive(true);

        when(jwtService.validateToken("token.farmerA")).thenReturn(true);
        when(jwtService.extractUserId("token.farmerA")).thenReturn(10L);
        when(jwtService.extractRole("token.farmerA")).thenReturn("FARMER");
        when(jwtService.extractMobileNumber("token.farmerA")).thenReturn("9876543210");
        when(farmerRepository.findById(10L)).thenReturn(Optional.of(farmerA));

        FarmResponse response = FarmResponse.builder()
                .id(101L)
                .farmerId(10L)
                .farmName("Sunrise Plots")
                .village("Khed")
                .taluka("Khed")
                .district("Pune")
                .state("Maharashtra")
                .farmArea(new BigDecimal("15.5"))
                .build();

        when(farmService.createFarm(eq(10L), any(FarmCreateRequest.class))).thenReturn(response);

        String payload = "{\"farmName\":\"Sunrise Plots\",\"village\":\"Khed\",\"taluka\":\"Khed\",\"district\":\"Pune\",\"state\":\"Maharashtra\",\"farmArea\":15.5}";

        mockMvc.perform(post("/api/farmers/farms")
                        .header("Authorization", "Bearer token.farmerA")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.farmerId", is(10)));
    }

    @Test
    @DisplayName("Farmer A lists farms -> Returns farms owned by Farmer A (10L)")
    void testFarmerListsFarms() throws Exception {
        Farmer farmerA = Farmer.builder().fullName("Ramesh Kumar").mobileNumber("9876543210").accountStatus("ACTIVE").build();
        farmerA.setId(10L);
        farmerA.setActive(true);

        when(jwtService.validateToken("token.farmerA")).thenReturn(true);
        when(jwtService.extractUserId("token.farmerA")).thenReturn(10L);
        when(jwtService.extractRole("token.farmerA")).thenReturn("FARMER");
        when(jwtService.extractMobileNumber("token.farmerA")).thenReturn("9876543210");
        when(farmerRepository.findById(10L)).thenReturn(Optional.of(farmerA));

        FarmResponse response = FarmResponse.builder().id(101L).farmerId(10L).farmName("Sunrise Plots").build();
        when(farmService.getFarmsByFarmerId(10L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/farmers/farms")
                        .header("Authorization", "Bearer token.farmerA")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    @Test
    @DisplayName("Farmer A attempting to update Farmer B's farm -> Rejected with 403 Forbidden")
    void testFarmerUpdatingOtherFarmerFarmRejected() throws Exception {
        Farmer farmerA = Farmer.builder().fullName("Ramesh Kumar").mobileNumber("9876543210").accountStatus("ACTIVE").build();
        farmerA.setId(10L);
        farmerA.setActive(true);

        when(jwtService.validateToken("token.farmerA")).thenReturn(true);
        when(jwtService.extractUserId("token.farmerA")).thenReturn(10L);
        when(jwtService.extractRole("token.farmerA")).thenReturn("FARMER");
        when(jwtService.extractMobileNumber("token.farmerA")).thenReturn("9876543210");
        when(farmerRepository.findById(10L)).thenReturn(Optional.of(farmerA));

        when(farmService.updateFarm(eq(999L), eq(10L), any(FarmCreateRequest.class)))
                .thenThrow(new AccessDeniedException("Access is denied. You do not have permission to modify another farmer's farm."));

        String payload = "{\"farmName\":\"Hacked Farm\",\"village\":\"Khed\",\"taluka\":\"Khed\",\"district\":\"Pune\",\"state\":\"Maharashtra\",\"farmArea\":20.0}";

        mockMvc.perform(put("/api/farmers/farms/999")
                        .header("Authorization", "Bearer token.farmerA")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("Farmer A attempting to delete Farmer B's farm -> Rejected with 403 Forbidden")
    void testFarmerDeletingOtherFarmerFarmRejected() throws Exception {
        Farmer farmerA = Farmer.builder().fullName("Ramesh Kumar").mobileNumber("9876543210").accountStatus("ACTIVE").build();
        farmerA.setId(10L);
        farmerA.setActive(true);

        when(jwtService.validateToken("token.farmerA")).thenReturn(true);
        when(jwtService.extractUserId("token.farmerA")).thenReturn(10L);
        when(jwtService.extractRole("token.farmerA")).thenReturn("FARMER");
        when(jwtService.extractMobileNumber("token.farmerA")).thenReturn("9876543210");
        when(farmerRepository.findById(10L)).thenReturn(Optional.of(farmerA));

        doThrow(new AccessDeniedException("Access is denied. You do not have permission to delete another farmer's farm."))
                .when(farmService).deleteFarm(999L, 10L);

        mockMvc.perform(delete("/api/farmers/farms/999")
                        .header("Authorization", "Bearer token.farmerA")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)));
    }
}
