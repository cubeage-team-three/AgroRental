package com.agrorental.operator;

import com.agrorental.common.exception.GlobalExceptionHandler;
import com.agrorental.operator.controller.OperatorDashboardController;
import com.agrorental.operator.dto.OperatorDashboardActiveJobResponse;
import com.agrorental.operator.dto.OperatorDashboardMetricsResponse;
import com.agrorental.operator.enums.OperatorAssignmentStatus;
import com.agrorental.operator.service.OperatorDashboardService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDate;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("OperatorDashboardController MockMvc Standalone Tests")
class OperatorDashboardControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OperatorDashboardService dashboardService;

    @InjectMocks
    private OperatorDashboardController dashboardController;

    private OperatorPrincipal testPrincipal;

    @BeforeEach
    void setUp() {
        testPrincipal = OperatorPrincipal.builder()
                .id(1L)
                .mobileNumber("9876543210")
                .fullName("Rajesh Shinde")
                .role("OPERATOR")
                .build();

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
                return testPrincipal;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(dashboardController)
                .setCustomArgumentResolvers(principalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/operators/dashboard/metrics should return 200 OK with aggregated metrics")
    void testGetDashboardMetricsSuccess() throws Exception {
        OperatorDashboardActiveJobResponse activeJob = OperatorDashboardActiveJobResponse.builder()
                .assignmentId(500L)
                .bookingId(100L)
                .status(OperatorAssignmentStatus.IN_PROGRESS)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(2))
                .equipmentName("Mahindra Tractor")
                .build();

        OperatorDashboardMetricsResponse response = OperatorDashboardMetricsResponse.builder()
                .totalJobs(10L)
                .assignedJobs(2L)
                .acceptedJobs(1L)
                .travelingJobs(1L)
                .reachedJobs(0L)
                .inProgressJobs(1L)
                .pausedJobs(0L)
                .completedJobs(5L)
                .rejectedJobs(1L)
                .cancelledJobs(0L)
                .todayJobs(2L)
                .upcomingJobs(1L)
                .completionRate(50.0)
                .acceptanceRate(88.9)
                .activeJob(activeJob)
                .build();

        when(dashboardService.getDashboardMetrics(1L)).thenReturn(response);

        mockMvc.perform(get("/api/operators/dashboard/metrics")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalJobs", is(10)))
                .andExpect(jsonPath("$.data.completedJobs", is(5)))
                .andExpect(jsonPath("$.data.completionRate", is(50.0)))
                .andExpect(jsonPath("$.data.activeJob.assignmentId", is(500)))
                .andExpect(jsonPath("$.data.activeJob.equipmentName", is("Mahindra Tractor")));
    }
}
