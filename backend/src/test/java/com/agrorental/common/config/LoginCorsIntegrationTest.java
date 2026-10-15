package com.agrorental.common.config;

import com.agrorental.auth.controller.AuthController;
import com.agrorental.auth.service.AuthService;
import com.agrorental.auth.service.PasswordResetService;
import com.agrorental.common.exception.GlobalExceptionHandler;
import com.agrorental.operator.controller.OperatorAuthController;
import com.agrorental.operator.service.OperatorAuthService;
import com.agrorental.user.service.UserRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.filter.CorsFilter;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Login Endpoints CORS Preflight and Handler Routing Tests")
class LoginCorsIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @Mock
    private UserRegistrationService userRegistrationService;

    @Mock
    private PasswordResetService passwordResetService;

    @Mock
    private OperatorAuthService operatorAuthService;

    @Mock
    private Environment environment;

    private CorsProperties corsProperties;
    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"dev"});

        corsProperties = new CorsProperties();
        corsProperties.setEnvironment(environment);
        corsProperties.setAllowedOriginsRaw("http://localhost:5173,http://localhost:5174,http://localhost:3000,http://127.0.0.1:5173,http://127.0.0.1:5174");
        corsProperties.validateCorsConfiguration();

        securityConfig = new SecurityConfig(null, corsProperties);
        CorsFilter corsFilter = new CorsFilter(securityConfig.corsConfigurationSource());

        AuthController authController = new AuthController(authService, userRegistrationService, passwordResetService);
        OperatorAuthController operatorAuthController = new OperatorAuthController(operatorAuthService);

        mockMvc = MockMvcBuilders.standaloneSetup(authController, operatorAuthController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(corsFilter)
                .build();
    }

    @Test
    @DisplayName("OPTIONS /api/auth/login with Origin http://localhost:5174 returns 200 OK and CORS headers")
    void preflightAuthLogin_FromLocalhost5174_Succeeds() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://localhost:5174")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5174"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("POST")));
    }

    @Test
    @DisplayName("OPTIONS /api/operators/login with Origin http://localhost:5174 returns 200 OK and CORS headers")
    void preflightOperatorLogin_FromLocalhost5174_Succeeds() throws Exception {
        mockMvc.perform(options("/api/operators/login")
                        .header("Origin", "http://localhost:5174")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5174"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("POST")));
    }

    @Test
    @DisplayName("OPTIONS /api/auth/login with unapproved origin is rejected with 403 Forbidden without CORS headers")
    void preflightAuthLogin_FromUnapprovedOrigin_IsForbidden() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://evil-attacker.com")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("OPTIONS /api/operators/login with unapproved origin is rejected with 403 Forbidden without CORS headers")
    void preflightOperatorLogin_FromUnapprovedOrigin_IsForbidden() throws Exception {
        mockMvc.perform(options("/api/operators/login")
                        .header("Origin", "http://evil-attacker.com")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("POST /api/auth/login reaches AuthController with Origin http://localhost:5174")
    void postAuthLogin_FromLocalhost5174_ReachesController() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .header("Origin", "http://localhost:5174")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5174"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(jsonPath("$.message").value("Validation Error"));
    }

    @Test
    @DisplayName("POST /api/operators/login reaches OperatorAuthController with Origin http://localhost:5174 (retired: 410 Gone)")
    void postOperatorLogin_FromLocalhost5174_ReachesController() throws Exception {
        mockMvc.perform(post("/api/operators/login")
                        .header("Origin", "http://localhost:5174")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isGone())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5174"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(jsonPath("$.message").value(OperatorAuthController.LOGIN_RETIRED_MESSAGE));
    }

    @Test
    @DisplayName("POST /api/auth/login with unapproved origin is rejected with 403 Forbidden")
    void postAuthLogin_FromUnapprovedOrigin_IsForbidden() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .header("Origin", "http://unapproved-site.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
