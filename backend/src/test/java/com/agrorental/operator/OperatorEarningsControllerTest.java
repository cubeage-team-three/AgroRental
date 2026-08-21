package com.agrorental.operator;

import com.agrorental.common.exception.ForbiddenException;
import com.agrorental.common.exception.GlobalExceptionHandler;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.operator.controller.OperatorEarningsController;
import com.agrorental.operator.dto.OperatorEarningsHistoryResponse;
import com.agrorental.operator.dto.OperatorEarningsSummaryResponse;
import com.agrorental.operator.dto.OperatorJobEarningsResponse;
import com.agrorental.operator.enums.OperatorAssignmentStatus;
import com.agrorental.operator.service.OperatorEarningsService;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("OperatorEarningsController Standalone MockMvc Tests")
class OperatorEarningsControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OperatorEarningsService earningsService;

    @InjectMocks
    private OperatorEarningsController earningsController;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private OperatorPrincipal testPrincipal;
    private OperatorJobEarningsResponse mockJobEarnings;
    private OperatorEarningsSummaryResponse mockSummary;

    @BeforeEach
    void setUp() {
        testPrincipal = OperatorPrincipal.builder()
                .id(1L)
                .mobileNumber("9876543210")
                .fullName("Ramesh Shinde")
                .role("OPERATOR")
                .build();

        mockJobEarnings = OperatorJobEarningsResponse.builder()
                .assignmentId(100L)
                .bookingId(200L)
                .assignmentStatus(OperatorAssignmentStatus.COMPLETED)
                .hourlyRate(new BigDecimal("500.00"))
                .totalElapsedMinutes(360L)
                .pausedMinutes(60L)
                .netWorkMinutes(300L)
                .netWorkHours(5.0)
                .grossEarnings(new BigDecimal("2500.00"))
                .currency("INR")
                .isFinalized(true)
                .isEstimated(false)
                .workStartedAt(LocalDateTime.of(2026, 8, 20, 10, 0))
                .completedAt(LocalDateTime.of(2026, 8, 20, 16, 0))
                .build();

        mockSummary = OperatorEarningsSummaryResponse.builder()
                .totalCompletedJobs(5L)
                .totalWorkMinutes(1500L)
                .totalWorkHours(25.0)
                .totalPausedMinutes(180L)
                .totalGrossEarnings(new BigDecimal("12500.00"))
                .averageEarningsPerJob(new BigDecimal("2500.00"))
                .hourlyRate(new BigDecimal("500.00"))
                .currency("INR")
                .build();

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
                return testPrincipal;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(earningsController)
                .setCustomArgumentResolvers(principalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/operators/jobs/{id}/earnings returns 200 OK with job earnings")
    void getJobEarnings_success() throws Exception {
        when(earningsService.getJobEarnings(100L, 1L)).thenReturn(mockJobEarnings);

        mockMvc.perform(get("/api/operators/jobs/100/earnings")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.assignmentId", is(100)))
                .andExpect(jsonPath("$.data.hourlyRate", is(500.0)))
                .andExpect(jsonPath("$.data.totalElapsedMinutes", is(360)))
                .andExpect(jsonPath("$.data.pausedMinutes", is(60)))
                .andExpect(jsonPath("$.data.netWorkMinutes", is(300)))
                .andExpect(jsonPath("$.data.netWorkHours", is(5.0)))
                .andExpect(jsonPath("$.data.grossEarnings", is(2500.0)))
                .andExpect(jsonPath("$.data.currency", is("INR")))
                .andExpect(jsonPath("$.data.isFinalized", is(true)));
    }

    @Test
    @DisplayName("GET /api/operators/jobs/{id}/earnings returns 403 on IDOR cross-operator access")
    void getJobEarnings_crossOperator_returns403() throws Exception {
        when(earningsService.getJobEarnings(100L, 1L))
                .thenThrow(new ForbiddenException("Access denied: You do not have permission to view earnings for this assignment"));

        mockMvc.perform(get("/api/operators/jobs/100/earnings")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Access denied: You do not have permission to view earnings for this assignment")));
    }

    @Test
    @DisplayName("GET /api/operators/jobs/{id}/earnings returns 404 when assignment not found")
    void getJobEarnings_notFound_returns404() throws Exception {
        when(earningsService.getJobEarnings(999L, 1L))
                .thenThrow(new ResourceNotFoundException("Job assignment not found with ID: 999"));

        mockMvc.perform(get("/api/operators/jobs/999/earnings")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Job assignment not found with ID: 999")));
    }

    @Test
    @DisplayName("GET /api/operators/earnings/summary returns 200 OK with aggregated statistics")
    void getEarningsSummary_success() throws Exception {
        when(earningsService.getEarningsSummary(1L)).thenReturn(mockSummary);

        mockMvc.perform(get("/api/operators/earnings/summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalCompletedJobs", is(5)))
                .andExpect(jsonPath("$.data.totalWorkMinutes", is(1500)))
                .andExpect(jsonPath("$.data.totalWorkHours", is(25.0)))
                .andExpect(jsonPath("$.data.totalPausedMinutes", is(180)))
                .andExpect(jsonPath("$.data.totalGrossEarnings", is(12500.0)))
                .andExpect(jsonPath("$.data.averageEarningsPerJob", is(2500.0)))
                .andExpect(jsonPath("$.data.hourlyRate", is(500.0)))
                .andExpect(jsonPath("$.data.currency", is("INR")));
    }

    @Test
    @DisplayName("GET /api/operators/earnings/history returns 200 OK with paginated records")
    void getEarningsHistory_success() throws Exception {
        OperatorEarningsHistoryResponse item = OperatorEarningsHistoryResponse.builder()
                .assignmentId(100L)
                .bookingId(200L)
                .equipmentName("John Deere 5310 4WD Tractor")
                .equipmentCategory("TRACTOR")
                .deliveryAddress("Shirur Farm Yard, Pune")
                .completedAt(LocalDateTime.of(2026, 8, 20, 16, 0))
                .netWorkMinutes(300L)
                .netWorkHours(5.0)
                .hourlyRate(new BigDecimal("500.00"))
                .grossEarnings(new BigDecimal("2500.00"))
                .currency("INR")
                .build();

        Page<OperatorEarningsHistoryResponse> historyPage = new PageImpl<>(List.of(item), PageRequest.of(0, 10), 1);
        when(earningsService.getEarningsHistory(eq(1L), any(Pageable.class))).thenReturn(historyPage);

        mockMvc.perform(get("/api/operators/earnings/history?page=0&size=10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content[0].assignmentId", is(100)))
                .andExpect(jsonPath("$.data.content[0].bookingId", is(200)))
                .andExpect(jsonPath("$.data.content[0].equipmentName", is("John Deere 5310 4WD Tractor")))
                .andExpect(jsonPath("$.data.content[0].netWorkMinutes", is(300)))
                .andExpect(jsonPath("$.data.content[0].grossEarnings", is(2500.0)));
    }
}
