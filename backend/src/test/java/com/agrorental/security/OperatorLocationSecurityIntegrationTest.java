package com.agrorental.security;

import com.agrorental.common.exception.GlobalExceptionHandler;
import com.agrorental.operator.controller.OperatorLocationController;
import com.agrorental.operator.dto.OperatorLocationResponse;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorStatus;
<<<<<<< HEAD
=======
import com.agrorental.admin.repository.AdminRepository;
>>>>>>> origin/development
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.operator.service.OperatorLocationService;
import com.agrorental.security.jwt.JwtAuthenticationFilter;
import com.agrorental.security.jwt.JwtService;
import com.agrorental.security.principal.OperatorPrincipal;
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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Operator Location Security & JWT Filter Integration Tests")
class OperatorLocationSecurityIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private JwtService jwtService;

    @Mock
    private OperatorRepository operatorRepository;

    @Mock
<<<<<<< HEAD
=======
    private AdminRepository adminRepository;

    @Mock
>>>>>>> origin/development
    private OperatorLocationService locationService;

    @InjectMocks
    private OperatorLocationController locationController;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
<<<<<<< HEAD
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService, operatorRepository);
=======
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService, operatorRepository, adminRepository);
>>>>>>> origin/development

        HandlerMethodArgumentResolver principalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().isAssignableFrom(OperatorPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest,
                                          WebDataBinderFactory binderFactory) {
                var auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getPrincipal() instanceof OperatorPrincipal op) {
                    return op;
                }
                return null;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(locationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(principalResolver)
                .addFilters(jwtAuthenticationFilter)
                .build();

        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Anonymous access to location endpoints returns 401 Unauthorized")
    void anonymousAccess_returns401() throws Exception {
        mockMvc.perform(get("/api/operators/jobs/100/location"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Full authentication is required to access this resource")));
    }

    @Test
    @DisplayName("Valid operator JWT token allows access and returns 200 OK")
    void validOperatorJwt_returns200() throws Exception {
        String token = "valid.jwt.token";
        when(jwtService.validateToken(token)).thenReturn(true);
        when(jwtService.extractUserId(token)).thenReturn(1L);
        when(jwtService.extractRole(token)).thenReturn("OPERATOR");
        when(jwtService.extractMobileNumber(token)).thenReturn("9876543210");

        Operator operator = Operator.builder()
                .fullName("Ramesh Shinde")
                .mobileNumber("9876543210")
                .status(OperatorStatus.APPROVED)
                .mobileVerified(true)
                .build();
        operator.setId(1L);
        operator.setActive(true);

        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));

        OperatorLocationResponse response = OperatorLocationResponse.builder()
                .assignmentId(100L)
                .operatorId(1L)
                .latitude(18.5204)
                .longitude(73.8567)
                .trackingActive(true)
                .recordedAt(LocalDateTime.now())
                .build();

        when(locationService.getLatestLocation(100L, 1L)).thenReturn(response);

        mockMvc.perform(get("/api/operators/jobs/100/location")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.latitude", is(18.5204)))
                .andExpect(jsonPath("$.data.trackingActive", is(true)));
    }

    @Test
    @DisplayName("Inactive operator token is rejected by filter returning 401 Unauthorized")
    void inactiveOperator_rejectedByFilter_returns401() throws Exception {
        String token = "valid.jwt.inactive";
        when(jwtService.validateToken(token)).thenReturn(true);
        when(jwtService.extractUserId(token)).thenReturn(1L);
        when(jwtService.extractRole(token)).thenReturn("OPERATOR");

        Operator inactiveOp = Operator.builder()
                .status(OperatorStatus.APPROVED)
                .build();
        inactiveOp.setId(1L);
        inactiveOp.setActive(false);

        when(operatorRepository.findById(1L)).thenReturn(Optional.of(inactiveOp));

        mockMvc.perform(get("/api/operators/jobs/100/location")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("Tampered JWT signature fails validation and returns 401 Unauthorized")
    void tamperedJwt_returns401() throws Exception {
        String token = "bad.tampered.token";
        when(jwtService.validateToken(token)).thenReturn(false);

        mockMvc.perform(patch("/api/operators/jobs/100/location/start")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }
}
