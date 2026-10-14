package com.agrorental.auth;

import com.agrorental.auth.controller.AuthController;
import com.agrorental.auth.dto.LoginRequest;
import com.agrorental.auth.dto.LoginResponse;
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

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Explicit Role Selection & Disambiguation Web Tests")
class AuthControllerRoleSelectionTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @Mock
    private UserRegistrationService userRegistrationService;

    @Mock
    private PasswordResetService passwordResetService;

    @InjectMocks
    private AuthController authController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/auth/login - Explicit FARMER login returns 200 and Farmer identity")
    void testFarmerExplicitLoginSuccess() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("farmer@agro.com")
                .password("Password@123")
                .role("FARMER")
                .loginType("PASSWORD")
                .build();

        LoginResponse response = LoginResponse.builder()
                .token("jwt.farmer.token")
                .farmerId(100L)
                .fullName("Ramesh Farmer")
                .email("farmer@agro.com")
                .role("FARMER")
                .accountStatus("ACTIVE")
                .message("Login successful. Welcome back, Ramesh Farmer!")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.role", is("FARMER")))
                .andExpect(jsonPath("$.data.farmerId", is(100)))
                .andExpect(jsonPath("$.data.token", is("jwt.farmer.token")));
    }

    @Test
    @DisplayName("POST /api/auth/login - Explicit PARTNER login returns 200 and Partner identity")
    void testPartnerExplicitLoginSuccess() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("partner@agro.com")
                .password("Password@123")
                .role("PARTNER")
                .loginType("PASSWORD")
                .build();

        LoginResponse response = LoginResponse.builder()
                .token("jwt.partner.token")
                .partnerId(200L)
                .fullName("Suresh Partner")
                .email("partner@agro.com")
                .role("PARTNER")
                .accountStatus("APPROVED")
                .message("Partner login successful. Welcome back, Suresh Partner!")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.role", is("PARTNER")))
                .andExpect(jsonPath("$.data.partnerId", is(200)))
                .andExpect(jsonPath("$.data.token", is("jwt.partner.token")));
    }

    @Test
    @DisplayName("POST /api/auth/login - Explicit OPERATOR login returns 200 and Operator identity")
    void testOperatorExplicitLoginSuccess() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("9876543210")
                .password("Password@123")
                .role("OPERATOR")
                .loginType("PASSWORD")
                .build();

        LoginResponse response = LoginResponse.builder()
                .token("jwt.operator.token")
                .operatorId(300L)
                .fullName("Rohan Operator")
                .mobileNumber("9876543210")
                .role("OPERATOR")
                .accountStatus("APPROVED")
                .message("Operator login successful. Welcome back, Rohan Operator!")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.role", is("OPERATOR")))
                .andExpect(jsonPath("$.data.operatorId", is(300)))
                .andExpect(jsonPath("$.data.token", is("jwt.operator.token")));
    }

    @Test
    @DisplayName("POST /api/auth/login - Explicit ADMIN login returns 200 and Admin identity")
    void testAdminExplicitLoginSuccess() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("admin@agro.com")
                .password("Admin@123")
                .role("ADMIN")
                .loginType("PASSWORD")
                .build();

        LoginResponse response = LoginResponse.builder()
                .token("jwt.admin.token")
                .fullName("Super Admin")
                .email("admin@agro.com")
                .role("ADMIN")
                .accountStatus("ACTIVE")
                .message("Admin login successful.")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.role", is("ADMIN")))
                .andExpect(jsonPath("$.data.token", is("jwt.admin.token")));
    }

    @Test
    @DisplayName("POST /api/auth/login - Invalid role value returns 400 Bad Request")
    void testInvalidRoleRejected() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("farmer@agro.com")
                .password("Password@123")
                .role("UNKNOWN_ROLE")
                .build();

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadRequestException("Invalid role specified. Allowed roles are: FARMER, PARTNER, OPERATOR, ADMIN."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Invalid role specified. Allowed roles are: FARMER, PARTNER, OPERATOR, ADMIN.")));
    }

    @Test
    @DisplayName("POST /api/auth/login - Privilege escalation / role tampering fails safely with 400")
    void testRoleTamperingRejected() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("farmer@agro.com")
                .password("FarmerPassword")
                .role("ADMIN")
                .build();

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadRequestException("Invalid email or password."));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Invalid email or password.")));
    }
}
