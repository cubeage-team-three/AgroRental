package com.agrorental.operator;

import com.agrorental.common.exception.GlobalExceptionHandler;
import com.agrorental.operator.controller.OperatorLocationController;
import com.agrorental.operator.dto.OperatorLocationResponse;
import com.agrorental.operator.dto.OperatorLocationUpdateRequest;
import com.agrorental.operator.service.OperatorLocationService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("OperatorLocationController Standalone MockMvc Tests")
class OperatorLocationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OperatorLocationService locationService;

    @InjectMocks
    private OperatorLocationController locationController;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private OperatorPrincipal testPrincipal;
    private OperatorLocationResponse mockResponse;

    @BeforeEach
    void setUp() {
        testPrincipal = OperatorPrincipal.builder()
                .id(1L)
                .mobileNumber("9876543210")
                .fullName("Ramesh Shinde")
                .role("OPERATOR")
                .build();

        mockResponse = OperatorLocationResponse.builder()
                .id(500L)
                .assignmentId(100L)
                .operatorId(1L)
                .latitude(18.5204)
                .longitude(73.8567)
                .accuracy(10.0)
                .speed(12.5)
                .heading(90.0)
                .trackingActive(true)
                .recordedAt(LocalDateTime.now())
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

        mockMvc = MockMvcBuilders.standaloneSetup(locationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(principalResolver)
                .build();
    }

    @Test
    @DisplayName("PATCH /api/operators/jobs/{id}/location/start returns 200 OK")
    void startTracking_success() throws Exception {
        when(locationService.startTracking(100L, 1L)).thenReturn(mockResponse);

        mockMvc.perform(patch("/api/operators/jobs/100/location/start")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.assignmentId", is(100)))
                .andExpect(jsonPath("$.data.trackingActive", is(true)));
    }

    @Test
    @DisplayName("PATCH /api/operators/jobs/{id}/location returns 200 OK")
    void updateLocation_success() throws Exception {
        OperatorLocationUpdateRequest request = OperatorLocationUpdateRequest.builder()
                .latitude(18.5204)
                .longitude(73.8567)
                .accuracy(5.0)
                .speed(15.0)
                .heading(90.0)
                .build();

        when(locationService.updateLocation(eq(100L), eq(1L), any(OperatorLocationUpdateRequest.class)))
                .thenReturn(mockResponse);

        mockMvc.perform(patch("/api/operators/jobs/100/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.latitude", is(18.5204)))
                .andExpect(jsonPath("$.data.longitude", is(73.8567)));
    }

    @Test
    @DisplayName("GET /api/operators/jobs/{id}/location returns 200 OK")
    void getLatestLocation_success() throws Exception {
        when(locationService.getLatestLocation(100L, 1L)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/operators/jobs/100/location")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.assignmentId", is(100)))
                .andExpect(jsonPath("$.data.latitude", is(18.5204)));
    }

    @Test
    @DisplayName("PATCH /api/operators/jobs/{id}/location/stop returns 200 OK")
    void stopTracking_success() throws Exception {
        mockResponse.setTrackingActive(false);
        when(locationService.stopTracking(100L, 1L)).thenReturn(mockResponse);

        mockMvc.perform(patch("/api/operators/jobs/100/location/stop")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.trackingActive", is(false)));
    }

    @Test
    @DisplayName("PATCH /api/operators/jobs/{id}/location with missing latitude returns 400 Bad Request")
    void updateLocation_validationFailure_missingCoordinates() throws Exception {
        OperatorLocationUpdateRequest request = OperatorLocationUpdateRequest.builder()
                .longitude(73.8567)
                .build();

        mockMvc.perform(patch("/api/operators/jobs/100/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("PATCH /api/operators/jobs/{id}/location with latitude > 90 returns 400 Bad Request")
    void updateLocation_validationFailure_outOfBoundsLatitude() throws Exception {
        OperatorLocationUpdateRequest request = OperatorLocationUpdateRequest.builder()
                .latitude(95.0)
                .longitude(73.8567)
                .build();

        mockMvc.perform(patch("/api/operators/jobs/100/location")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }
}
