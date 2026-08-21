package com.agrorental.operator;

import com.agrorental.common.exception.GlobalExceptionHandler;
import com.agrorental.operator.controller.OperatorJobLifecycleController;
import com.agrorental.operator.dto.OperatorAssignedJobResponse;
import com.agrorental.operator.dto.OperatorJobCompletionRequest;
import com.agrorental.operator.dto.OperatorJobPauseRequest;
import com.agrorental.operator.dto.OperatorJobRejectionRequest;
import com.agrorental.operator.enums.OperatorAssignmentStatus;
import com.agrorental.operator.service.OperatorJobLifecycleService;
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
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("OperatorJobLifecycleController MockMvc Standalone Tests")
class OperatorJobLifecycleControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private OperatorJobLifecycleService lifecycleService;

    @InjectMocks
    private OperatorJobLifecycleController lifecycleController;

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

        mockMvc = MockMvcBuilders.standaloneSetup(lifecycleController)
                .setCustomArgumentResolvers(principalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("PATCH /api/operators/jobs/{id}/accept should accept job and return 200 OK")
    void testAcceptJobEndpoint() throws Exception {
        OperatorAssignedJobResponse response = OperatorAssignedJobResponse.builder()
                .assignmentId(500L)
                .assignmentStatus(OperatorAssignmentStatus.ACCEPTED)
                .acceptedAt(LocalDateTime.now())
                .build();

        when(lifecycleService.acceptJob(500L, 1L)).thenReturn(response);

        mockMvc.perform(patch("/api/operators/jobs/500/accept")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.assignmentStatus", is("ACCEPTED")));
    }

    @Test
    @DisplayName("PATCH /api/operators/jobs/{id}/reject should reject job with reason")
    void testRejectJobEndpoint() throws Exception {
        OperatorJobRejectionRequest request = OperatorJobRejectionRequest.builder()
                .rejectionReason("Prior schedule conflict")
                .build();

        OperatorAssignedJobResponse response = OperatorAssignedJobResponse.builder()
                .assignmentId(500L)
                .assignmentStatus(OperatorAssignmentStatus.REJECTED)
                .rejectedAt(LocalDateTime.now())
                .rejectionReason("Prior schedule conflict")
                .build();

        when(lifecycleService.rejectJob(eq(500L), eq(1L), any(OperatorJobRejectionRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/api/operators/jobs/500/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.assignmentStatus", is("REJECTED")));
    }

    @Test
    @DisplayName("PATCH /api/operators/jobs/{id}/start-travel should mark traveling")
    void testStartTravelEndpoint() throws Exception {
        OperatorAssignedJobResponse response = OperatorAssignedJobResponse.builder()
                .assignmentId(500L)
                .assignmentStatus(OperatorAssignmentStatus.TRAVELING)
                .travelingAt(LocalDateTime.now())
                .build();

        when(lifecycleService.startTravel(500L, 1L)).thenReturn(response);

        mockMvc.perform(patch("/api/operators/jobs/500/start-travel")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.assignmentStatus", is("TRAVELING")));
    }

    @Test
    @DisplayName("PATCH /api/operators/jobs/{id}/reached should mark reached location")
    void testMarkReachedEndpoint() throws Exception {
        OperatorAssignedJobResponse response = OperatorAssignedJobResponse.builder()
                .assignmentId(500L)
                .assignmentStatus(OperatorAssignmentStatus.REACHED)
                .reachedAt(LocalDateTime.now())
                .build();

        when(lifecycleService.markReached(500L, 1L)).thenReturn(response);

        mockMvc.perform(patch("/api/operators/jobs/500/reached")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.assignmentStatus", is("REACHED")));
    }

    @Test
    @DisplayName("PATCH /api/operators/jobs/{id}/start-work should mark in-progress")
    void testStartWorkEndpoint() throws Exception {
        OperatorAssignedJobResponse response = OperatorAssignedJobResponse.builder()
                .assignmentId(500L)
                .assignmentStatus(OperatorAssignmentStatus.IN_PROGRESS)
                .workStartedAt(LocalDateTime.now())
                .build();

        when(lifecycleService.startWork(500L, 1L)).thenReturn(response);

        mockMvc.perform(patch("/api/operators/jobs/500/start-work")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.assignmentStatus", is("IN_PROGRESS")));
    }

    @Test
    @DisplayName("PATCH /api/operators/jobs/{id}/pause should pause work with reason")
    void testPauseWorkEndpoint() throws Exception {
        OperatorJobPauseRequest request = OperatorJobPauseRequest.builder()
                .pauseReason("Heavy rain")
                .build();

        OperatorAssignedJobResponse response = OperatorAssignedJobResponse.builder()
                .assignmentId(500L)
                .assignmentStatus(OperatorAssignmentStatus.PAUSED)
                .pausedAt(LocalDateTime.now())
                .pauseReason("Heavy rain")
                .build();

        when(lifecycleService.pauseWork(eq(500L), eq(1L), any(OperatorJobPauseRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/api/operators/jobs/500/pause")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.assignmentStatus", is("PAUSED")));
    }

    @Test
    @DisplayName("PATCH /api/operators/jobs/{id}/resume should resume work")
    void testResumeWorkEndpoint() throws Exception {
        OperatorAssignedJobResponse response = OperatorAssignedJobResponse.builder()
                .assignmentId(500L)
                .assignmentStatus(OperatorAssignmentStatus.IN_PROGRESS)
                .resumedAt(LocalDateTime.now())
                .build();

        when(lifecycleService.resumeWork(500L, 1L)).thenReturn(response);

        mockMvc.perform(patch("/api/operators/jobs/500/resume")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.assignmentStatus", is("IN_PROGRESS")));
    }

    @Test
    @DisplayName("PATCH /api/operators/jobs/{id}/complete should complete work")
    void testCompleteWorkEndpoint() throws Exception {
        OperatorJobCompletionRequest request = OperatorJobCompletionRequest.builder()
                .completionNotes("5 acres ploughed")
                .build();

        OperatorAssignedJobResponse response = OperatorAssignedJobResponse.builder()
                .assignmentId(500L)
                .assignmentStatus(OperatorAssignmentStatus.COMPLETED)
                .completedAt(LocalDateTime.now())
                .completionNotes("5 acres ploughed")
                .build();

        when(lifecycleService.completeWork(eq(500L), eq(1L), any())).thenReturn(response);

        mockMvc.perform(patch("/api/operators/jobs/500/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.assignmentStatus", is("COMPLETED")));
    }
}
