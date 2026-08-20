package com.agrorental.operator;

import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.GlobalExceptionHandler;
import com.agrorental.operator.controller.OperatorController;
import com.agrorental.operator.dto.OperatorRegistrationRequest;
import com.agrorental.operator.dto.OperatorResponse;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.service.OperatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("OperatorController Standalone MockMvc Unit Tests")
class OperatorControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OperatorService operatorService;

    @InjectMocks
    private OperatorController operatorController;

    private OperatorResponse testResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(operatorController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        testResponse = OperatorResponse.builder()
                .id(1L)
                .fullName("Suresh Shinde")
                .mobileNumber("9876543210")
                .email("suresh@example.com")
                .address("Village Khed, Pune")
                .experience(5)
                .skills("Tractor & Rotavator")
                .profilePhoto("photo.jpg")
                .status(OperatorStatus.PENDING)
                .partnerId(null)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("POST /api/operators/register - Should return 201 Created and safe OperatorResponse without password")
    void shouldRegisterOperatorSuccessfully() throws Exception {
        when(operatorService.registerOperator(any(OperatorRegistrationRequest.class))).thenReturn(testResponse);

        String jsonPayload = """
                {
                    "fullName": "Suresh Shinde",
                    "mobileNumber": "9876543210",
                    "email": "suresh@example.com",
                    "address": "Village Khed, Pune",
                    "aadhaarNumber": "123456789012",
                    "drivingLicenseNumber": "MH1220200012345",
                    "experience": 5,
                    "skills": "Tractor & Rotavator",
                    "password": "SecurePassword123",
                    "profilePhoto": "photo.jpg"
                }
                """;

        mockMvc.perform(post("/api/operators/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Operator registered successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.fullName").value("Suresh Shinde"))
                .andExpect(jsonPath("$.data.mobileNumber").value("9876543210"))
                .andExpect(jsonPath("$.data.email").value("suresh@example.com"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.aadhaarNumber").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/operators/register - Should return 400 Bad Request when mobile number is duplicate")
    void shouldReturn400WhenMobileDuplicate() throws Exception {
        when(operatorService.registerOperator(any(OperatorRegistrationRequest.class)))
                .thenThrow(new BadRequestException("Mobile number is already registered"));

        String jsonPayload = """
                {
                    "fullName": "Suresh Shinde",
                    "mobileNumber": "9876543210",
                    "email": "suresh@example.com",
                    "address": "Village Khed, Pune",
                    "aadhaarNumber": "123456789012",
                    "drivingLicenseNumber": "MH1220200012345",
                    "experience": 5,
                    "skills": "Tractor & Rotavator",
                    "password": "SecurePassword123"
                }
                """;

        mockMvc.perform(post("/api/operators/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Mobile number is already registered"));
    }

    @Test
    @DisplayName("POST /api/operators/register - Should return 400 Bad Request with validation errors for invalid payload")
    void shouldReturn400OnValidationFailure() throws Exception {
        String invalidPayload = """
                {
                    "fullName": "",
                    "mobileNumber": "12345",
                    "email": "invalid-email",
                    "address": "",
                    "aadhaarNumber": "123",
                    "drivingLicenseNumber": "",
                    "experience": -1,
                    "skills": "",
                    "password": "short"
                }
                """;

        mockMvc.perform(post("/api/operators/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation Error"))
                .andExpect(jsonPath("$.data.fullName").exists())
                .andExpect(jsonPath("$.data.mobileNumber").exists())
                .andExpect(jsonPath("$.data.email").exists())
                .andExpect(jsonPath("$.data.experience").exists())
                .andExpect(jsonPath("$.data.password").exists());
    }
}
