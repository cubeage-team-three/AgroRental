package com.agrorental.operator;

import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.GlobalExceptionHandler;
import com.agrorental.operator.controller.AdminOperatorController;
import com.agrorental.operator.dto.OperatorDetailResponse;
import com.agrorental.operator.dto.OperatorSummaryResponse;
import com.agrorental.operator.dto.OperatorVerificationRequest;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.service.AdminOperatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminOperatorController MockMvc Unit Tests")
class AdminOperatorControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdminOperatorService adminOperatorService;

    @InjectMocks
    private AdminOperatorController adminOperatorController;

    private OperatorSummaryResponse summaryResponse;
    private OperatorDetailResponse detailResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(adminOperatorController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        summaryResponse = OperatorSummaryResponse.builder()
                .id(1L)
                .fullName("Suresh Shinde")
                .mobileNumber("9876543210")
                .email("suresh@example.com")
                .experience(5)
                .skills("Tractor")
                .status(OperatorStatus.PENDING)
                .mobileVerified(true)
                .documentsCount(1)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        detailResponse = OperatorDetailResponse.builder()
                .id(1L)
                .fullName("Suresh Shinde")
                .mobileNumber("9876543210")
                .email("suresh@example.com")
                .maskedAadhaarNumber("XXXX-XXXX-9012")
                .maskedDrivingLicenseNumber("DL-XXXX-2345")
                .status(OperatorStatus.APPROVED)
                .mobileVerified(true)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("GET /api/admin/operators - Should return 200 OK with paginated list")
    void shouldGetOperatorsPaginated() throws Exception {
        Page<OperatorSummaryResponse> page = new PageImpl<>(Collections.singletonList(summaryResponse), org.springframework.data.domain.PageRequest.of(0, 10), 1);
        when(adminOperatorService.getOperators(any(), any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/operators?status=PENDING&page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].fullName").value("Suresh Shinde"))
                .andExpect(jsonPath("$.data.content[0].mobileVerified").value(true));
    }

    @Test
    @DisplayName("GET /api/admin/operators/{id} - Should return 200 OK with operator detail")
    void shouldGetOperatorDetail() throws Exception {
        when(adminOperatorService.getOperatorDetail(1L)).thenReturn(detailResponse);

        mockMvc.perform(get("/api/admin/operators/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.maskedAadhaarNumber").value("XXXX-XXXX-9012"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    @DisplayName("PATCH /api/admin/operators/{id}/verify - Should return 200 OK on approval")
    void shouldVerifyOperator() throws Exception {
        when(adminOperatorService.verifyOperator(eq(1L), any(OperatorVerificationRequest.class)))
                .thenReturn(detailResponse);

        String jsonPayload = """
                {
                    "status": "APPROVED"
                }
                """;

        mockMvc.perform(patch("/api/admin/operators/1/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    @DisplayName("PATCH /api/admin/operators/{id}/verify - Should return 400 Bad Request on invalid verification")
    void shouldReturn400OnInvalidVerification() throws Exception {
        when(adminOperatorService.verifyOperator(eq(1L), any(OperatorVerificationRequest.class)))
                .thenThrow(new BadRequestException("Operator mobile number must be verified before approval."));

        String jsonPayload = """
                {
                    "status": "APPROVED"
                }
                """;

        mockMvc.perform(patch("/api/admin/operators/1/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Operator mobile number must be verified before approval."));
    }
}
