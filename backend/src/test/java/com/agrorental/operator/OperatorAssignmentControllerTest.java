package com.agrorental.operator;

import com.agrorental.booking.entity.BookingStatus;
import com.agrorental.common.exception.GlobalExceptionHandler;
import com.agrorental.operator.controller.OperatorAssignmentController;
import com.agrorental.operator.dto.EligibleOperatorResponse;
import com.agrorental.operator.dto.OperatorAssignedJobResponse;
import com.agrorental.operator.dto.OperatorAssignmentRequest;
import com.agrorental.operator.dto.OperatorAssignmentResponse;
import com.agrorental.operator.enums.OperatorAssignmentStatus;
import com.agrorental.operator.service.OperatorAssignmentService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("OperatorAssignmentController MockMvc Standalone Tests")
class OperatorAssignmentControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private OperatorAssignmentService assignmentService;

    @InjectMocks
    private OperatorAssignmentController assignmentController;

    private OperatorPrincipal testPrincipal;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
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

        mockMvc = MockMvcBuilders.standaloneSetup(assignmentController)
                .setCustomArgumentResolvers(new org.springframework.data.web.PageableHandlerMethodArgumentResolver(), principalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/bookings/{bookingId}/operator should assign operator and return 201 Created")
    void testAssignOperatorSuccess() throws Exception {
        OperatorAssignmentRequest request = OperatorAssignmentRequest.builder()
                .operatorId(1L)
                .notes("Assigned by admin")
                .build();

        OperatorAssignmentResponse response = OperatorAssignmentResponse.builder()
                .assignmentId(500L)
                .bookingId(100L)
                .operatorId(1L)
                .operatorName("Rajesh Shinde")
                .operatorMobile("9876543210")
                .assignmentStatus(OperatorAssignmentStatus.ASSIGNED)
                .assignedAt(LocalDateTime.now())
                .assignedBy("ADMIN_USER")
                .notes("Assigned by admin")
                .build();

        when(assignmentService.assignOperator(eq(100L), any(OperatorAssignmentRequest.class), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/bookings/100/operator")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.assignmentId", is(500)))
                .andExpect(jsonPath("$.data.operatorName", is("Rajesh Shinde")))
                .andExpect(jsonPath("$.data.assignmentStatus", is("ASSIGNED")));
    }

    @Test
    @DisplayName("POST /api/bookings/{bookingId}/operator with negative or missing operatorId should return 400 Bad Request")
    void testAssignOperatorValidationFailure() throws Exception {
        OperatorAssignmentRequest invalidRequest = OperatorAssignmentRequest.builder()
                .operatorId(-1L) // Invalid ID
                .build();

        mockMvc.perform(post("/api/bookings/100/operator")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("GET /api/bookings/{bookingId}/operator should return active booking assignment")
    void testGetBookingAssignment() throws Exception {
        OperatorAssignmentResponse response = OperatorAssignmentResponse.builder()
                .assignmentId(500L)
                .bookingId(100L)
                .operatorId(1L)
                .operatorName("Rajesh Shinde")
                .assignmentStatus(OperatorAssignmentStatus.ASSIGNED)
                .build();

        when(assignmentService.getBookingAssignment(100L)).thenReturn(response);

        mockMvc.perform(get("/api/bookings/100/operator")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.bookingId", is(100)))
                .andExpect(jsonPath("$.data.operatorId", is(1)));
    }

    @Test
    @DisplayName("GET /api/operators/eligible should return page of eligible operators")
    void testGetEligibleOperators() throws Exception {
        EligibleOperatorResponse eligible = EligibleOperatorResponse.builder()
                .operatorId(1L)
                .fullName("Rajesh Shinde")
                .mobileNumber("9876543210")
                .skills("Tractor Operation")
                .experience(6)
                .mobileVerified(true)
                .active(true)
                .build();

        Page<EligibleOperatorResponse> page = new PageImpl<>(List.of(eligible), org.springframework.data.domain.PageRequest.of(0, 10), 1);
        when(assignmentService.findEligibleOperators(any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/operators/eligible?search=tractor")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content[0].operatorId", is(1)))
                .andExpect(jsonPath("$.data.content[0].fullName", is("Rajesh Shinde")));
    }

    @Test
    @DisplayName("GET /api/operators/jobs/assigned should return authenticated operator's assigned tasks")
    void testGetAssignedJobsSuccess() throws Exception {
        OperatorAssignedJobResponse job = OperatorAssignedJobResponse.builder()
                .assignmentId(500L)
                .bookingId(100L)
                .assignmentStatus(OperatorAssignmentStatus.ASSIGNED)
                .equipmentName("Mahindra 575 DI")
                .deliveryAddress("Farm Plot 12, Pune")
                .startDate(LocalDate.now().plusDays(2))
                .endDate(LocalDate.now().plusDays(4))
                .totalCost(BigDecimal.valueOf(4500))
                .bookingStatus(BookingStatus.CONFIRMED)
                .build();

        Page<OperatorAssignedJobResponse> page = new PageImpl<>(List.of(job), org.springframework.data.domain.PageRequest.of(0, 10), 1);
        when(assignmentService.getAssignedJobs(eq(1L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/operators/jobs/assigned")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content[0].assignmentId", is(500)))
                .andExpect(jsonPath("$.data.content[0].equipmentName", is("Mahindra 575 DI")));
    }

    @Test
    @DisplayName("GET /api/operators/jobs/{assignmentId} should return assignment detail")
    void testGetAssignedJobDetailSuccess() throws Exception {
        OperatorAssignedJobResponse job = OperatorAssignedJobResponse.builder()
                .assignmentId(500L)
                .bookingId(100L)
                .assignmentStatus(OperatorAssignmentStatus.ASSIGNED)
                .equipmentName("Mahindra 575 DI")
                .build();

        when(assignmentService.getAssignedJob(1L, 500L)).thenReturn(job);

        mockMvc.perform(get("/api/operators/jobs/500")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.assignmentId", is(500)))
                .andExpect(jsonPath("$.data.bookingId", is(100)));
    }
}
