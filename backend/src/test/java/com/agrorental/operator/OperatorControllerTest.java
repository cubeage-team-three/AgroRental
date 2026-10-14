package com.agrorental.operator;

import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.GlobalExceptionHandler;
import com.agrorental.operator.controller.OperatorController;
import com.agrorental.operator.dto.OperatorRegistrationRequest;
import com.agrorental.operator.dto.OperatorResponse;
import com.agrorental.operator.dto.OperatorSummaryResponse;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.mapper.OperatorMapper;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("OperatorController Standalone MockMvc Unit Tests")
class OperatorControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OperatorService operatorService;

    @Mock
    private OperatorMapper operatorMapper;

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
    @Test
    @DisplayName("GET /api/operators - Should return list of OperatorSummaryResponse")
    void shouldGetAllOperators() throws Exception {
        Operator operator = new Operator();
        operator.setId(1L);
        OperatorSummaryResponse summary = OperatorSummaryResponse.builder().id(1L).fullName("Suresh").build();

        when(operatorService.getAllOperators()).thenReturn(List.of(operator));
        when(operatorMapper.toSummaryResponseList(anyList())).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/operators"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].fullName").value("Suresh"));
    }

    @Test
    @DisplayName("GET /api/operators/available - Should return list of OperatorSummaryResponse")
    void shouldGetAvailableOperators() throws Exception {
        Operator operator = new Operator();
        operator.setId(2L);
        OperatorSummaryResponse summary = OperatorSummaryResponse.builder().id(2L).fullName("Ramesh").build();

        when(operatorService.getAvailableOperators(any(), any(), any())).thenReturn(List.of(operator));
        when(operatorMapper.toSummaryResponseList(anyList())).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/operators/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(2))
                .andExpect(jsonPath("$.data[0].fullName").value("Ramesh"));
    }

    @Test
    @DisplayName("GET /api/operators/partner/{id} - Should return list of OperatorSummaryResponse")
    void shouldGetOperatorsByPartner() throws Exception {
        Operator operator = new Operator();
        operator.setId(3L);
        OperatorSummaryResponse summary = OperatorSummaryResponse.builder().id(3L).fullName("Mahesh").build();

        when(operatorService.getOperatorsByPartner(10L)).thenReturn(List.of(operator));
        when(operatorMapper.toSummaryResponseList(anyList())).thenReturn(List.of(summary));

        mockMvc.perform(get("/api/operators/partner/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(3))
                .andExpect(jsonPath("$.data[0].fullName").value("Mahesh"));
    }
}
