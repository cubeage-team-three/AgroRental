package com.agrorental.operator;

import tools.jackson.databind.ObjectMapper;
import com.agrorental.common.exception.ForbiddenException;
import com.agrorental.common.exception.GlobalExceptionHandler;
import com.agrorental.common.exception.UnauthorizedException;
import com.agrorental.operator.controller.OperatorAuthController;
import com.agrorental.operator.dto.AuthenticatedOperatorResponse;
import com.agrorental.operator.dto.OperatorLoginRequest;
import com.agrorental.operator.dto.OperatorLoginResponse;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.service.OperatorAuthService;
import com.agrorental.security.principal.OperatorPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.core.MethodParameter;

import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("OperatorAuthController MockMvc Standalone Unit Tests")
class OperatorAuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private OperatorAuthService operatorAuthService;

    @InjectMocks
    private OperatorAuthController operatorAuthController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // Custom argument resolver to inject OperatorPrincipal when testing standalone MockMvc
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
                var auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getPrincipal() instanceof OperatorPrincipal) {
                    return auth.getPrincipal();
                }
                return null;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(operatorAuthController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(principalResolver)
                .build();
    }

    @Test
    @DisplayName("POST /api/operators/login - Success (200 OK)")
    void testLoginSuccess() throws Exception {
        OperatorLoginRequest request = OperatorLoginRequest.builder()
                .mobileNumber("9876543210")
                .password("Password@123")
                .build();

        AuthenticatedOperatorResponse authOperator = AuthenticatedOperatorResponse.builder()
                .id(1L)
                .fullName("Rajesh Shinde")
                .mobileNumber("9876543210")
                .email("rajesh@agrorental.com")
                .status(OperatorStatus.APPROVED)
                .mobileVerified(true)
                .active(true)
                .role("OPERATOR")
                .build();

        OperatorLoginResponse response = OperatorLoginResponse.builder()
                .accessToken("mocked.jwt.token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .operator(authOperator)
                .build();

        when(operatorAuthService.login(any(OperatorLoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/operators/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Operator login successful")))
                .andExpect(jsonPath("$.data.accessToken", is("mocked.jwt.token")))
                .andExpect(jsonPath("$.data.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.data.operator.fullName", is("Rajesh Shinde")))
                .andExpect(jsonPath("$.data.operator.role", is("OPERATOR")));
    }

    @Test
    @DisplayName("POST /api/operators/login - Wrong Password (401 Unauthorized)")
    void testLoginInvalidCredentials() throws Exception {
        OperatorLoginRequest request = OperatorLoginRequest.builder()
                .mobileNumber("9876543210")
                .password("WrongPassword")
                .build();

        when(operatorAuthService.login(any(OperatorLoginRequest.class)))
                .thenThrow(new UnauthorizedException("Invalid mobile number or password"));

        mockMvc.perform(post("/api/operators/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Invalid mobile number or password")));
    }

    @Test
    @DisplayName("POST /api/operators/login - Pending Approval (403 Forbidden)")
    void testLoginPendingApproval() throws Exception {
        OperatorLoginRequest request = OperatorLoginRequest.builder()
                .mobileNumber("9876543210")
                .password("Password@123")
                .build();

        when(operatorAuthService.login(any(OperatorLoginRequest.class)))
                .thenThrow(new ForbiddenException("Operator account is pending admin approval"));

        mockMvc.perform(post("/api/operators/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Operator account is pending admin approval")));
    }

    @Test
    @DisplayName("POST /api/operators/login - Validation Error on Invalid Mobile (400 Bad Request)")
    void testLoginValidationFailure() throws Exception {
        OperatorLoginRequest request = OperatorLoginRequest.builder()
                .mobileNumber("123") // Invalid format
                .password("")
                .build();

        mockMvc.perform(post("/api/operators/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("GET /api/operators/me - Success with Principal (200 OK)")
    void testGetMeSuccess() throws Exception {
        OperatorPrincipal principal = OperatorPrincipal.builder()
                .id(1L)
                .mobileNumber("9876543210")
                .fullName("Rajesh Shinde")
                .role("OPERATOR")
                .build();

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_OPERATOR"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        AuthenticatedOperatorResponse authOperator = AuthenticatedOperatorResponse.builder()
                .id(1L)
                .fullName("Rajesh Shinde")
                .mobileNumber("9876543210")
                .email("rajesh@agrorental.com")
                .status(OperatorStatus.APPROVED)
                .mobileVerified(true)
                .active(true)
                .role("OPERATOR")
                .build();

        when(operatorAuthService.getCurrentOperator(1L)).thenReturn(authOperator);

        mockMvc.perform(get("/api/operators/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.fullName", is("Rajesh Shinde")))
                .andExpect(jsonPath("$.data.role", is("OPERATOR")));

        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("GET /api/operators/me - Unauthorized without Principal (401 Unauthorized)")
    void testGetMeUnauthorized() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/operators/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Full authentication is required to access this resource")));
    }
}
