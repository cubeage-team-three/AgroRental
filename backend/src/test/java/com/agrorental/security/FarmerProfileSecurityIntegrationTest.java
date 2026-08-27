package com.agrorental.security;

import com.agrorental.common.exception.GlobalExceptionHandler;
import com.agrorental.farmer.controller.FarmerProfileController;
import com.agrorental.farmer.dto.FarmerProfileResponse;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.farmer.service.FarmerProfileService;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Farmer Profile Security & IDOR Authorization Tests")
class FarmerProfileSecurityIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private JwtService jwtService;

    @Mock
    private FarmerRepository farmerRepository;

    @Mock
    private FarmerProfileService farmerProfileService;

    @InjectMocks
    private FarmerProfileController farmerProfileController;

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

        mockMvc = MockMvcBuilders.standaloneSetup(farmerProfileController)
                .addFilter(jwtAuthenticationFilter)
                .setCustomArgumentResolvers(principalResolver, authResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Farmer A requesting own profile /api/farmers/profile/1 -> 200 OK Allowed")
    void testFarmerAccessingOwnProfileAllowed() throws Exception {
        Farmer farmerA = Farmer.builder()
                .fullName("Ramesh Kumar")
                .mobileNumber("9876543210")
                .accountStatus("ACTIVE")
                .build();
        farmerA.setId(1L);
        farmerA.setActive(true);

        when(jwtService.validateToken("token.farmerA")).thenReturn(true);
        when(jwtService.extractUserId("token.farmerA")).thenReturn(1L);
        when(jwtService.extractRole("token.farmerA")).thenReturn("FARMER");
        when(jwtService.extractMobileNumber("token.farmerA")).thenReturn("9876543210");
        when(farmerRepository.findById(1L)).thenReturn(Optional.of(farmerA));

        FarmerProfileResponse profileResponse = FarmerProfileResponse.builder()
                .farmerId(1L)
                .fullName("Ramesh Kumar")
                .mobileNumber("9876543210")
                .accountStatus("ACTIVE")
                .build();

        when(farmerProfileService.getProfile(1L)).thenReturn(profileResponse);

        mockMvc.perform(get("/api/farmers/profile/1")
                        .header("Authorization", "Bearer token.farmerA")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.farmerId", is(1)))
                .andExpect(jsonPath("$.data.fullName", is("Ramesh Kumar")));
    }

    @Test
    @DisplayName("Farmer A attempting to access Farmer B profile /api/farmers/profile/2 -> Rejected (403 Forbidden)")
    void testFarmerAccessingOtherFarmerProfileRejected() throws Exception {
        Farmer farmerA = Farmer.builder()
                .fullName("Ramesh Kumar")
                .mobileNumber("9876543210")
                .accountStatus("ACTIVE")
                .build();
        farmerA.setId(1L);
        farmerA.setActive(true);

        when(jwtService.validateToken("token.farmerA")).thenReturn(true);
        when(jwtService.extractUserId("token.farmerA")).thenReturn(1L);
        when(jwtService.extractRole("token.farmerA")).thenReturn("FARMER");
        when(jwtService.extractMobileNumber("token.farmerA")).thenReturn("9876543210");
        when(farmerRepository.findById(1L)).thenReturn(Optional.of(farmerA));

        mockMvc.perform(get("/api/farmers/profile/2")
                        .header("Authorization", "Bearer token.farmerA")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("Farmer A attempting to update Farmer B profile /api/farmers/profile/2 -> Rejected")
    void testFarmerUpdatingOtherFarmerProfileRejected() throws Exception {
        Farmer farmerA = Farmer.builder()
                .fullName("Ramesh Kumar")
                .mobileNumber("9876543210")
                .accountStatus("ACTIVE")
                .build();
        farmerA.setId(1L);
        farmerA.setActive(true);

        when(jwtService.validateToken("token.farmerA")).thenReturn(true);
        when(jwtService.extractUserId("token.farmerA")).thenReturn(1L);
        when(jwtService.extractRole("token.farmerA")).thenReturn("FARMER");
        when(jwtService.extractMobileNumber("token.farmerA")).thenReturn("9876543210");
        when(farmerRepository.findById(1L)).thenReturn(Optional.of(farmerA));

        String updateReqJson = "{\"fullName\":\"Hacked Name\"}";

        mockMvc.perform(put("/api/farmers/profile/2")
                        .header("Authorization", "Bearer token.farmerA")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateReqJson))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("Farmer A attempting to change Farmer B password /api/farmers/change-password/2 -> Rejected")
    void testFarmerChangingOtherFarmerPasswordRejected() throws Exception {
        Farmer farmerA = Farmer.builder()
                .fullName("Ramesh Kumar")
                .mobileNumber("9876543210")
                .accountStatus("ACTIVE")
                .build();
        farmerA.setId(1L);
        farmerA.setActive(true);

        when(jwtService.validateToken("token.farmerA")).thenReturn(true);
        when(jwtService.extractUserId("token.farmerA")).thenReturn(1L);
        when(jwtService.extractRole("token.farmerA")).thenReturn("FARMER");
        when(jwtService.extractMobileNumber("token.farmerA")).thenReturn("9876543210");
        when(farmerRepository.findById(1L)).thenReturn(Optional.of(farmerA));

        String passwordReqJson = "{\"currentPassword\":\"OldPass123\",\"newPassword\":\"NewPass123\"}";

        mockMvc.perform(put("/api/farmers/change-password/2")
                        .header("Authorization", "Bearer token.farmerA")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(passwordReqJson))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)));
    }
}

