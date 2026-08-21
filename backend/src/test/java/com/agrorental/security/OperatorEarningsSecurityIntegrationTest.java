package com.agrorental.security;

import com.agrorental.common.exception.GlobalExceptionHandler;
import com.agrorental.operator.controller.OperatorEarningsController;
import com.agrorental.operator.dto.OperatorEarningsSummaryResponse;
import com.agrorental.operator.dto.OperatorJobEarningsResponse;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.enums.OperatorAssignmentStatus;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.operator.service.OperatorEarningsService;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Operator Earnings Security & JWT Filter Integration Tests")
class OperatorEarningsSecurityIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private JwtService jwtService;

    @Mock
    private OperatorRepository operatorRepository;

    @Mock
    private OperatorEarningsService earningsService;

    @InjectMocks
    private OperatorEarningsController earningsController;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService, operatorRepository);

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

        mockMvc = MockMvcBuilders.standaloneSetup(earningsController)
                .addFilters(jwtAuthenticationFilter)
                .setCustomArgumentResolvers(principalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Anonymous request to GET /api/operators/jobs/1/earnings returns 401 Unauthorized")
    void anonymousAccess_getJobEarnings_returns401() throws Exception {
        mockMvc.perform(get("/api/operators/jobs/1/earnings")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Full authentication is required to access this resource")));
    }

    @Test
    @DisplayName("Anonymous request to GET /api/operators/earnings/summary returns 401 Unauthorized")
    void anonymousAccess_getSummary_returns401() throws Exception {
        mockMvc.perform(get("/api/operators/earnings/summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Full authentication is required to access this resource")));
    }

    @Test
    @DisplayName("Tampered / Invalid JWT Bearer token returns 401 Unauthorized")
    void invalidJwt_returns401() throws Exception {
        when(jwtService.validateToken("invalid.jwt.token")).thenReturn(false);

        mockMvc.perform(get("/api/operators/earnings/summary")
                        .header("Authorization", "Bearer invalid.jwt.token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Full authentication is required to access this resource")));
    }

    @Test
    @DisplayName("Valid Operator JWT allows GET /api/operators/earnings/summary returning 200 OK")
    void validJwt_getSummary_returns200() throws Exception {
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

        OperatorEarningsSummaryResponse summary = OperatorEarningsSummaryResponse.builder()
                .totalCompletedJobs(3L)
                .totalWorkMinutes(900L)
                .totalWorkHours(15.0)
                .totalGrossEarnings(new BigDecimal("7500.00"))
                .currency("INR")
                .build();
        when(earningsService.getEarningsSummary(1L)).thenReturn(summary);

        mockMvc.perform(get("/api/operators/earnings/summary")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalCompletedJobs", is(3)))
                .andExpect(jsonPath("$.data.totalGrossEarnings", is(7500.0)));
    }
}
