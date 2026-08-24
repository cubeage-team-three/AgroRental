package com.agrorental.security;

import com.agrorental.common.exception.GlobalExceptionHandler;
import com.agrorental.operator.controller.OperatorAssignmentController;
import com.agrorental.operator.dto.OperatorAssignedJobResponse;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.enums.OperatorAssignmentStatus;
import com.agrorental.admin.repository.AdminRepository;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.operator.service.OperatorAssignmentService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Operator Assignment Security & JWT Filter Integration Tests")
class OperatorAssignmentSecurityIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private JwtService jwtService;

    @Mock
    private OperatorRepository operatorRepository;

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private OperatorAssignmentService assignmentService;

    @InjectMocks
    private OperatorAssignmentController assignmentController;

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

        mockMvc = MockMvcBuilders.standaloneSetup(assignmentController)
                .addFilter(jwtAuthenticationFilter)
                .setCustomArgumentResolvers(new org.springframework.data.web.PageableHandlerMethodArgumentResolver(), principalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Anonymous request to /api/operators/jobs/assigned without Bearer header returns 401 Unauthorized")
    void testAnonymousAccessToJobsReturns401() throws Exception {
        mockMvc.perform(get("/api/operators/jobs/assigned")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Full authentication is required to access this resource")));
    }

    @Test
    @DisplayName("Valid Operator JWT accessing /api/operators/jobs/assigned populates SecurityContext and returns 200 OK")
    void testValidOperatorJwtCanAccessAssignedJobs() throws Exception {
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

        OperatorAssignedJobResponse job = OperatorAssignedJobResponse.builder()
                .assignmentId(500L)
                .bookingId(100L)
                .assignmentStatus(OperatorAssignmentStatus.ASSIGNED)
                .equipmentName("Mahindra 575 DI")
                .build();

        Page<OperatorAssignedJobResponse> page = new PageImpl<>(List.of(job), org.springframework.data.domain.PageRequest.of(0, 10), 1);
        when(assignmentService.getAssignedJobs(eq(1L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/operators/jobs/assigned")
                        .header("Authorization", "Bearer valid.jwt.token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content[0].assignmentId", is(500)));
    }

    @Test
    @DisplayName("Tampered JWT accessing /api/operators/jobs/assigned returns 401 Unauthorized")
    void testTamperedJwtGets401() throws Exception {
        when(jwtService.validateToken("tampered.token")).thenReturn(false);

        mockMvc.perform(get("/api/operators/jobs/assigned")
                        .header("Authorization", "Bearer tampered.token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("Inactive operator token accessing /api/operators/jobs/assigned is rejected with 401 Unauthorized")
    void testInactiveOperatorTokenRejected() throws Exception {
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

        mockMvc.perform(get("/api/operators/jobs/assigned")
                        .header("Authorization", "Bearer token.inactive")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }
}
