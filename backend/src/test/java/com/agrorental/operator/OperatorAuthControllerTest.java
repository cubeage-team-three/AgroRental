package com.agrorental.operator;

import com.agrorental.common.exception.GlobalExceptionHandler;
import com.agrorental.operator.controller.OperatorAuthController;
import com.agrorental.operator.dto.AuthenticatedOperatorResponse;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.service.OperatorAuthService;
import com.agrorental.security.principal.OperatorPrincipal;
import org.junit.jupiter.api.AfterEach;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("OperatorAuthController MockMvc Standalone Unit Tests")
class OperatorAuthControllerTest {

    /** Standalone MockMvc tests set principals on this thread; never leak them into later test classes. */
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private MockMvc mockMvc;

    @Mock
    private OperatorAuthService operatorAuthService;

    @InjectMocks
    private OperatorAuthController operatorAuthController;

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
    @DisplayName("POST /api/operators/login - Retired: 410 Gone even with credentials, no token, service never called")
    void testRetiredLoginWithCredentials() throws Exception {
        mockMvc.perform(post("/api/operators/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mobileNumber\":\"9876543210\",\"password\":\"Password@123\"}"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is(OperatorAuthController.LOGIN_RETIRED_MESSAGE)))
                .andExpect(jsonPath("$.data").doesNotExist());

        verifyNoInteractions(operatorAuthService);
    }

    @Test
    @DisplayName("POST /api/operators/login - Retired: the answer does not depend on the request (no validation, no lookup)")
    void testRetiredLoginIsRequestIndependent() throws Exception {
        for (String body : new String[]{"{}", "{\"mobileNumber\":\"123\",\"password\":\"\"}",
                "{\"mobileNumber\":\"9000000000\",\"password\":\"WrongPassword1\"}"}) {
            mockMvc.perform(post("/api/operators/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isGone())
                    .andExpect(jsonPath("$.message", is(OperatorAuthController.LOGIN_RETIRED_MESSAGE)));
        }
        verifyNoInteractions(operatorAuthService);
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
