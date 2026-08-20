package com.agrorental.operator;

import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.GlobalExceptionHandler;
import com.agrorental.operator.dto.OperatorOtpResponse;
import com.agrorental.operator.dto.OperatorOtpSendRequest;
import com.agrorental.operator.dto.OperatorOtpVerifyRequest;
import com.agrorental.operator.otp.OperatorOtpController;
import com.agrorental.operator.otp.OperatorOtpService;
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
@DisplayName("OperatorOtpController MockMvc Unit Tests")
class OperatorOtpControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OperatorOtpService operatorOtpService;

    @InjectMocks
    private OperatorOtpController operatorOtpController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(operatorOtpController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/operators/otp/send - Should return 200 OK with safe OTP send response")
    void shouldSendOtpSuccessfully() throws Exception {
        OperatorOtpResponse response = OperatorOtpResponse.builder()
                .mobileNumber("9876543210")
                .verified(false)
                .message("OTP sent successfully to ******3210")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attemptsRemaining(3)
                .devMockOtp("123456")
                .build();

        when(operatorOtpService.sendOtp(any(OperatorOtpSendRequest.class))).thenReturn(response);

        String jsonPayload = """
                {
                    "mobileNumber": "9876543210"
                }
                """;

        mockMvc.perform(post("/api/operators/otp/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.mobileNumber").value("9876543210"))
                .andExpect(jsonPath("$.data.verified").value(false))
                .andExpect(jsonPath("$.data.otpHash").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/operators/otp/verify - Should return 200 OK when OTP is verified")
    void shouldVerifyOtpSuccessfully() throws Exception {
        OperatorOtpResponse response = OperatorOtpResponse.builder()
                .mobileNumber("9876543210")
                .verified(true)
                .message("Mobile number verified successfully.")
                .build();

        when(operatorOtpService.verifyOtp(any(OperatorOtpVerifyRequest.class))).thenReturn(response);

        String jsonPayload = """
                {
                    "mobileNumber": "9876543210",
                    "otp": "123456"
                }
                """;

        mockMvc.perform(post("/api/operators/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.verified").value(true));
    }

    @Test
    @DisplayName("POST /api/operators/otp/verify - Should return 400 Bad Request on invalid OTP")
    void shouldReturn400OnInvalidOtp() throws Exception {
        when(operatorOtpService.verifyOtp(any(OperatorOtpVerifyRequest.class)))
                .thenThrow(new BadRequestException("Invalid OTP. 2 attempts remaining."));

        String jsonPayload = """
                {
                    "mobileNumber": "9876543210",
                    "otp": "999999"
                }
                """;

        mockMvc.perform(post("/api/operators/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid OTP. 2 attempts remaining."));
    }

    @Test
    @DisplayName("POST /api/operators/otp/verify - Should return 400 on malformed input (e.g. 4-digit OTP)")
    void shouldReturn400OnValidationFailure() throws Exception {
        String jsonPayload = """
                {
                    "mobileNumber": "123",
                    "otp": "1234"
                }
                """;

        mockMvc.perform(post("/api/operators/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data.mobileNumber").exists())
                .andExpect(jsonPath("$.data.otp").exists());
    }
}
