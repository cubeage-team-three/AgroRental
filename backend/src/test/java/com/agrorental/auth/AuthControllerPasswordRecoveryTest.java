package com.agrorental.auth;

import com.agrorental.auth.controller.AuthController;
import com.agrorental.auth.dto.ForgotPasswordRequest;
import com.agrorental.auth.dto.ForgotPasswordResponse;
import com.agrorental.auth.dto.ResetPasswordRequest;
import com.agrorental.auth.service.AuthService;
import com.agrorental.auth.service.PasswordPolicy;
import com.agrorental.auth.service.PasswordResetRateLimitedException;
import com.agrorental.auth.service.PasswordResetService;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.GlobalExceptionHandler;
import com.agrorental.user.service.UserRegistrationService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer wiring of the password recovery endpoints (service mocked). The service behaviour itself
 * (non-enumeration, hashing, expiry, single use, rate limits, dev-token gate) is covered against a real
 * database by PasswordResetFlowIntegrationTest.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Password Recovery Endpoint MockMvc Tests")
class AuthControllerPasswordRecoveryTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @Mock
    private UserRegistrationService userRegistrationService;

    @Mock
    private PasswordResetService passwordResetService;

    @InjectMocks
    private AuthController authController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static ForgotPasswordResponse unavailable() {
        return ForgotPasswordResponse.builder()
                .message(PasswordResetService.DELIVERY_UNAVAILABLE_MESSAGE)
                .deliveryConfigured(false)
                .build();
    }

    @Test
    @DisplayName("POST /api/auth/forgot-password - message and structured data come from the service; client address is passed for rate limiting")
    void testForgotPasswordReturnsServiceMessage() throws Exception {
        when(passwordResetService.requestReset(any(ForgotPasswordRequest.class), anyString())).thenReturn(unavailable());

        mockMvc.perform(post("/api/auth/forgot-password")
                        .with(request -> {
                            request.setRemoteAddr("10.1.2.3");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ForgotPasswordRequest.builder()
                                .role("PARTNER").identifier("9822001122").build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is(PasswordResetService.DELIVERY_UNAVAILABLE_MESSAGE)))
                .andExpect(jsonPath("$.data.message", is(PasswordResetService.DELIVERY_UNAVAILABLE_MESSAGE)))
                .andExpect(jsonPath("$.data.deliveryConfigured", is(false)))
                .andExpect(jsonPath("$.data.devResetToken").doesNotExist())
                .andExpect(jsonPath("$.data.devNotice").doesNotExist());

        ArgumentCaptor<ForgotPasswordRequest> captor = ArgumentCaptor.forClass(ForgotPasswordRequest.class);
        verify(passwordResetService).requestReset(captor.capture(), eq("10.1.2.3"));
        assertThat(captor.getValue().getRole()).isEqualTo("PARTNER");
        assertThat(captor.getValue().getAccountIdentifier()).isEqualTo("9822001122");
    }

    @Test
    @DisplayName("POST /api/auth/forgot-password - legacy identifier aliases (email / mobileOrEmail) are still accepted")
    void testForgotPasswordLegacyAliases() throws Exception {
        when(passwordResetService.requestReset(any(ForgotPasswordRequest.class), anyString())).thenReturn(unavailable());

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"FARMER\",\"email\":\"farmer@agrorent.in\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"OPERATOR\",\"mobileOrEmail\":\"9833445566\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/auth/forgot-password - Role ADMIN is rejected with 400 Bad Request")
    void testForgotPasswordAdminRejected() throws Exception {
        when(passwordResetService.requestReset(any(ForgotPasswordRequest.class), anyString()))
                .thenThrow(new BadRequestException(PasswordResetService.INVALID_ROLE_MESSAGE));

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ForgotPasswordRequest.builder()
                                .role("ADMIN").email("admin@agrorent.in").build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Invalid role specified for password recovery")));
    }

    @Test
    @DisplayName("POST /api/auth/forgot-password - Missing identifier returns 400 without reaching the service")
    void testForgotPasswordMissingIdentifier() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ForgotPasswordRequest.builder().role("FARMER").build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
        verifyNoInteractions(passwordResetService);
    }

    @Test
    @DisplayName("POST /api/auth/forgot-password - rate limit maps to 429 with a fixed generic body and Retry-After")
    void testForgotPasswordRateLimited() throws Exception {
        when(passwordResetService.requestReset(any(ForgotPasswordRequest.class), anyString()))
                .thenThrow(new PasswordResetRateLimitedException(900));

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"FARMER\",\"identifier\":\"9800000001\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "900"))
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is(PasswordResetRateLimitedException.MESSAGE)))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/auth/reset-password - Valid token returns 200 OK")
    void testResetPasswordSuccess() throws Exception {
        when(passwordResetService.resetPassword(any(ResetPasswordRequest.class)))
                .thenReturn(PasswordResetService.RESET_SUCCESS_MESSAGE);

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ResetPasswordRequest.builder()
                                .token("valid-token-1234567890ABCDEFGHIJ")
                                .newPassword("NewSecurePassword1")
                                .confirmPassword("NewSecurePassword1")
                                .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Password reset successfully")));
    }

    @Test
    @DisplayName("POST /api/auth/reset-password - Invalid token returns 400 Bad Request")
    void testResetPasswordInvalidToken() throws Exception {
        when(passwordResetService.resetPassword(any(ResetPasswordRequest.class)))
                .thenThrow(new BadRequestException(PasswordResetService.INVALID_TOKEN_MESSAGE));

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ResetPasswordRequest.builder()
                                .token("bad-token")
                                .newPassword("NewSecurePassword1")
                                .confirmPassword("NewSecurePassword1")
                                .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is(PasswordResetService.INVALID_TOKEN_MESSAGE)));
    }

    @Test
    @DisplayName("POST /api/auth/reset-password - Weak password surfaces the password policy message")
    void testResetPasswordWeakPassword() throws Exception {
        when(passwordResetService.resetPassword(any(ResetPasswordRequest.class)))
                .thenThrow(new BadRequestException(PasswordPolicy.REQUIREMENTS_MESSAGE));

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ResetPasswordRequest.builder()
                                .token("valid-token")
                                .newPassword("123")
                                .confirmPassword("123")
                                .build())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is(PasswordPolicy.REQUIREMENTS_MESSAGE)));
    }

    @Test
    @DisplayName("POST /api/auth/reset-password - Missing new/confirm password fails validation before the service")
    void testResetPasswordMissingFields() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
        verify(passwordResetService, never()).resetPassword(any());
    }
}
