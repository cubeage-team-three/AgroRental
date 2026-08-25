package com.agrorental.security;

import com.agrorental.common.exception.GlobalExceptionHandler;
import com.agrorental.operator.controller.OperatorAuthController;
import com.agrorental.operator.dto.AuthenticatedOperatorResponse;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.admin.repository.AdminRepository;
import com.agrorental.partner.repository.PartnerRepository;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.operator.service.OperatorAuthService;
import com.agrorental.security.jwt.JwtAuthenticationFilter;
import com.agrorental.security.jwt.JwtService;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Operator Security & JWT Authentication Filter Tests")
class OperatorSecurityIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private JwtService jwtService;

    @Mock
    private OperatorRepository operatorRepository;

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private PartnerRepository partnerRepository;

    @Mock
    private FarmerRepository farmerRepository;

    @Mock
    private OperatorAuthService operatorAuthService;

    @InjectMocks
    private OperatorAuthController operatorAuthController;

    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService, operatorRepository, adminRepository, partnerRepository, farmerRepository);

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
                .addFilter(jwtAuthenticationFilter)
                .setCustomArgumentResolvers(principalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Anonymous request to /api/operators/me without Bearer header returns 401 Unauthorized")
    void testAnonymousAccessToOperatorsMeReturns401() throws Exception {
        mockMvc.perform(get("/api/operators/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Full authentication is required to access this resource")));
    }

    @Test
    @DisplayName("Valid Operator JWT accessing /api/operators/me populates SecurityContext and returns 200 OK")
    void testOperatorJwtAccessToOperatorsMeReturns200() throws Exception {
        Operator operator = Operator.builder()
                .fullName("Rajesh Shinde")
                .mobileNumber("9876543210")
                .status(OperatorStatus.APPROVED)
                .mobileVerified(true)
                .build();
        operator.setId(1L);
        operator.setActive(true);

        when(jwtService.validateToken("valid.jwt.token")).thenReturn(true);
        when(jwtService.extractUserId("valid.jwt.token")).thenReturn(1L);
        when(jwtService.extractRole("valid.jwt.token")).thenReturn("OPERATOR");
        when(jwtService.extractMobileNumber("valid.jwt.token")).thenReturn("9876543210");
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(operator));

        AuthenticatedOperatorResponse authResponse = AuthenticatedOperatorResponse.builder()
                .id(1L)
                .fullName("Rajesh Shinde")
                .mobileNumber("9876543210")
                .status(OperatorStatus.APPROVED)
                .mobileVerified(true)
                .active(true)
                .role("OPERATOR")
                .build();

        when(operatorAuthService.getCurrentOperator(1L)).thenReturn(authResponse);

        mockMvc.perform(get("/api/operators/me")
                        .header("Authorization", "Bearer valid.jwt.token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.fullName", is("Rajesh Shinde")))
                .andExpect(jsonPath("$.data.role", is("OPERATOR")));
    }

    @Test
    @DisplayName("Invalid / Tampered JWT header fails authentication and returns 401 Unauthorized")
    void testTamperedTokenReturns401() throws Exception {
        when(jwtService.validateToken("tampered.token")).thenReturn(false);

        mockMvc.perform(get("/api/operators/me")
                        .header("Authorization", "Bearer tampered.token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("Inactive operator token is rejected by filter and returns 401 Unauthorized")
    void testInactiveOperatorTokenRejected() throws Exception {
        Operator inactiveOperator = Operator.builder()
                .fullName("Rajesh Shinde")
                .mobileNumber("9876543210")
                .status(OperatorStatus.APPROVED)
                .mobileVerified(true)
                .build();
        inactiveOperator.setId(1L);
        inactiveOperator.setActive(false); // Inactive

        when(jwtService.validateToken("token.for.inactive")).thenReturn(true);
        when(jwtService.extractUserId("token.for.inactive")).thenReturn(1L);
        when(jwtService.extractRole("token.for.inactive")).thenReturn("OPERATOR");
        when(jwtService.extractMobileNumber("token.for.inactive")).thenReturn("9876543210");
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(inactiveOperator));

        mockMvc.perform(get("/api/operators/me")
                        .header("Authorization", "Bearer token.for.inactive")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }
}
