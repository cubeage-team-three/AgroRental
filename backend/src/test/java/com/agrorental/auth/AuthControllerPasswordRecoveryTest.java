package com.agrorental.auth;

import com.agrorental.auth.controller.AuthController;
import com.agrorental.auth.dto.ForgotPasswordRequest;
import com.agrorental.auth.dto.ResetPasswordRequest;
import com.agrorental.auth.service.AuthService;
import com.agrorental.auth.service.PasswordResetService;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.GlobalExceptionHandler;
import com.agrorental.user.service.UserRegistrationService;
import tools.jackson.databind.ObjectMapper;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    @Test
    @DisplayName("POST /api/auth/forgot-password - Role FARMER returns 200 and generic message")
    void testForgotPasswordFarmerSuccess() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .role("FARMER")
                .email("farmer@agrorent.in")
                .build();

        when(passwordResetService.processForgotPassword(any(ForgotPasswordRequest.class)))
                .thenReturn("If an account with that identifier exists, password reset instructions have been sent.");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("If an account with that identifier exists")));
    }

    @Test
    @DisplayName("POST /api/auth/forgot-password - Role PARTNER returns 200 and generic message")
    void testForgotPasswordPartnerSuccess() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .role("PARTNER")
                .mobileOrEmail("9822001122")
                .build();

        when(passwordResetService.processForgotPassword(any(ForgotPasswordRequest.class)))
                .thenReturn("If an account with that identifier exists, password reset instructions have been sent.");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("If an account with that identifier exists")));
    }

    @Test
    @DisplayName("POST /api/auth/forgot-password - Role OPERATOR returns 200 and generic message")
    void testForgotPasswordOperatorSuccess() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .role("OPERATOR")
                .identifier("9833445566")
                .build();

        when(passwordResetService.processForgotPassword(any(ForgotPasswordRequest.class)))
                .thenReturn("If an account with that identifier exists, password reset instructions have been sent.");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("If an account with that identifier exists")));
    }

    @Test
    @DisplayName("POST /api/auth/forgot-password - Role ADMIN returns 400 Bad Request")
    void testForgotPasswordAdminRejected() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .role("ADMIN")
                .email("admin@agrorent.in")
                .build();

        when(passwordResetService.processForgotPassword(any(ForgotPasswordRequest.class)))
                .thenThrow(new BadRequestException("Invalid role specified for password recovery. Allowed roles: FARMER, PARTNER, OPERATOR."));

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Invalid role specified for password recovery")));
    }

    @Test
    @DisplayName("POST /api/auth/forgot-password - Missing identifier returns 400 Bad Request")
    void testForgotPasswordMissingIdentifier() throws Exception {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .role("FARMER")
                .build();

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("POST /api/auth/reset-password - Valid token returns 200 OK")
    void testResetPasswordSuccess() throws Exception {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("valid-token-1234")
                .newPassword("NewSecurePassword1")
                .confirmPassword("NewSecurePassword1")
                .build();

        when(passwordResetService.processResetPassword(any(ResetPasswordRequest.class)))
                .thenReturn("Password reset successfully. You can now log in with your new password.");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("Password reset successfully")));
    }

    @Test
    @DisplayName("POST /api/auth/reset-password - Invalid token returns 400 Bad Request")
    void testResetPasswordInvalidToken() throws Exception {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("bad-token")
                .newPassword("NewSecurePassword1")
                .confirmPassword("NewSecurePassword1")
                .build();

        when(passwordResetService.processResetPassword(any(ResetPasswordRequest.class)))
                .thenThrow(new BadRequestException("Invalid or expired password reset token."));

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Invalid or expired password reset token")));
    }

    @Test
    @DisplayName("POST /api/auth/reset-password - Short password returns 400 Bad Request")
    void testResetPasswordShortPassword() throws Exception {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("valid-token")
                .newPassword("123")
                .confirmPassword("123")
                .build();

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }
}
