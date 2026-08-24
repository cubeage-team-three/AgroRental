package com.agrorental.security;

import com.agrorental.common.exception.GlobalExceptionHandler;
import com.agrorental.operator.controller.OperatorJobLifecycleController;
import com.agrorental.operator.dto.OperatorAssignedJobResponse;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.enums.OperatorAssignmentStatus;
import com.agrorental.admin.repository.AdminRepository;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.operator.service.OperatorJobLifecycleService;
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

import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Operator Job Lifecycle Security & JWT Filter Integration Tests")
class OperatorJobLifecycleSecurityIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private JwtService jwtService;

    @Mock
    private OperatorRepository operatorRepository;

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private OperatorJobLifecycleService lifecycleService;

    @InjectMocks
    private OperatorJobLifecycleController lifecycleController;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService, operatorRepository, adminRepository);

        HandlerMethodArgumentResolver principalResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().equals(OperatorPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest,
                                          WebDataBinderFactory binderFactory) {
                var auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getPrincipal() instanceof OperatorPrincipal) {
                    return auth.getPrincipal();
                }
                return null;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(lifecycleController)
                .addFilter(jwtAuthenticationFilter)
                .setCustomArgumentResolvers(principalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Anonymous request to /api/operators/jobs/500/accept without token returns 401 Unauthorized")
    void testAnonymousAccessReturns401() throws Exception {
        mockMvc.perform(patch("/api/operators/jobs/500/accept")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("Valid Operator JWT accessing /api/operators/jobs/500/accept returns 200 OK")
    void testValidOperatorJwtCanAcceptJob() throws Exception {
        Operator operator = Operator.builder()
                .fullName("Rajesh Shinde")
                .mobileNumber("9876543210")
                .status(OperatorStatus.APPROVED)
                .mobileVerified(true)
                .build();
        operator.setId(1L);
        operator.setActive(true);

        when(jwtService.validateToken("valid.jwt.token")).thenReturn(true);
        when(jwtService.extractUserId("valid.jwt.token")).thenReturn(1L);
        when(jwtService.extractRole("valid.jwt.token")).thenReturn("OPERATOR");
        when(jwtService.extractMobileNumber("valid.jwt.token")).thenReturn("9876543210");
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));

        OperatorAssignedJobResponse response = OperatorAssignedJobResponse.builder()
                .assignmentId(500L)
                .assignmentStatus(OperatorAssignmentStatus.ACCEPTED)
                .build();

        when(lifecycleService.acceptJob(500L, 1L)).thenReturn(response);

        mockMvc.perform(patch("/api/operators/jobs/500/accept")
                        .header("Authorization", "Bearer valid.jwt.token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.assignmentStatus", is("ACCEPTED")));
    }

    @Test
    @DisplayName("Tampered JWT accessing lifecycle endpoint returns 401 Unauthorized")
    void testTamperedJwtReturns401() throws Exception {
        when(jwtService.validateToken("tampered.token")).thenReturn(false);

        mockMvc.perform(patch("/api/operators/jobs/500/accept")
                        .header("Authorization", "Bearer tampered.token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("Inactive operator token accessing lifecycle endpoint returns 401 Unauthorized")
    void testInactiveOperatorTokenReturns401() throws Exception {
        Operator inactiveOperator = Operator.builder()
                .fullName("Rajesh Shinde")
                .mobileNumber("9876543210")
                .status(OperatorStatus.APPROVED)
                .mobileVerified(true)
                .build();
        inactiveOperator.setId(1L);
        inactiveOperator.setActive(false);

        when(jwtService.validateToken("token.inactive")).thenReturn(true);
        when(jwtService.extractUserId("token.inactive")).thenReturn(1L);
        when(jwtService.extractRole("token.inactive")).thenReturn("OPERATOR");
        when(jwtService.extractMobileNumber("token.inactive")).thenReturn("9876543210");
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(inactiveOperator));

        mockMvc.perform(patch("/api/operators/jobs/500/accept")
                        .header("Authorization", "Bearer token.inactive")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }
}
