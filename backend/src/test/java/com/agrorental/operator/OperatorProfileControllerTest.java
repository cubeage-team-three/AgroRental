package com.agrorental.operator;

import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.GlobalExceptionHandler;
import com.agrorental.operator.controller.OperatorProfileController;
import com.agrorental.operator.dto.OperatorChangePasswordRequest;
import com.agrorental.operator.dto.OperatorProfileResponse;
import com.agrorental.operator.dto.OperatorProfileUpdateRequest;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.service.OperatorProfileService;
import com.agrorental.security.principal.OperatorPrincipal;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("OperatorProfileController MockMvc Unit Tests")
class OperatorProfileControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private OperatorProfileService operatorProfileService;

    @InjectMocks
    private OperatorProfileController operatorProfileController;

    private OperatorPrincipal testPrincipal;
    private OperatorProfileResponse testProfileResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        testPrincipal = OperatorPrincipal.builder()
                .id(1L)
                .mobileNumber("9876543210")
                .fullName("Sunil Jadhav")
                .role("OPERATOR")
                .build();

        testProfileResponse = OperatorProfileResponse.builder()
                .id(1L)
                .fullName("Sunil Jadhav")
                .mobileNumber("9876543210")
                .email("sunil@agrorental.com")
                .address("Nashik, Maharashtra")
                .experience(7)
                .skills("Tractor, Seeder")
                .maskedAadhaarNumber("XXXX-XXXX-9012")
                .maskedDrivingLicenseNumber("MH15XXXXXXXX765")
                .status(OperatorStatus.APPROVED)
                .mobileVerified(true)
                .active(true)
                .build();

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
                return testPrincipal;
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(operatorProfileController)
                .setCustomArgumentResolvers(principalResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/operators/profile should return 200 OK with safe profile details")
    void testGetProfileSuccess() throws Exception {
        when(operatorProfileService.getCurrentProfile(1L)).thenReturn(testProfileResponse);

        mockMvc.perform(get("/api/operators/profile")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(1)))
                .andExpect(jsonPath("$.data.fullName", is("Sunil Jadhav")))
                .andExpect(jsonPath("$.data.maskedAadhaarNumber", is("XXXX-XXXX-9012")))
                .andExpect(jsonPath("$.data.status", is("APPROVED")));
    }

    @Test
    @DisplayName("GET /api/operators/profile without authenticated principal should return 401 Unauthorized")
    void testGetProfileUnauthorizedWithoutPrincipal() throws Exception {
        HandlerMethodArgumentResolver nullResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().equals(OperatorPrincipal.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest,
                                          WebDataBinderFactory binderFactory) {
                return null;
            }
        };

        MockMvc nullMvc = MockMvcBuilders.standaloneSetup(operatorProfileController)
                .setCustomArgumentResolvers(nullResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        nullMvc.perform(get("/api/operators/profile")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("PUT /api/operators/profile should update profile and return 200 OK")
    void testUpdateProfileSuccess() throws Exception {
        OperatorProfileUpdateRequest request = OperatorProfileUpdateRequest.builder()
                .fullName("Sunil R. Jadhav")
                .email("sunil.new@agrorental.com")
                .address("Aurangabad, Maharashtra")
                .experience(9)
                .skills("Tractor, Drone Spraying")
                .build();

        when(operatorProfileService.updateCurrentProfile(eq(1L), any(OperatorProfileUpdateRequest.class)))
                .thenReturn(testProfileResponse);

        mockMvc.perform(put("/api/operators/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Operator profile updated successfully")));
    }

    @Test
    @DisplayName("PUT /api/operators/profile with invalid inputs should return 400 Bad Request")
    void testUpdateProfileValidationFailure() throws Exception {
        OperatorProfileUpdateRequest invalidRequest = OperatorProfileUpdateRequest.builder()
                .fullName("") // Blank name
                .email("invalid-email-format") // Invalid email
                .address("")
                .experience(-5) // Negative experience
                .skills("")
                .build();

        mockMvc.perform(put("/api/operators/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("PATCH /api/operators/profile/password should update password and return 200 OK")
    void testChangePasswordSuccess() throws Exception {
        OperatorChangePasswordRequest request = OperatorChangePasswordRequest.builder()
                .currentPassword("OldPassword@123")
                .newPassword("NewSecurePass@456")
                .confirmPassword("NewSecurePass@456")
                .build();

        doNothing().when(operatorProfileService).changePassword(eq(1L), any(OperatorChangePasswordRequest.class));

        mockMvc.perform(patch("/api/operators/profile/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", is("Password changed successfully")));
    }

    @Test
    @DisplayName("PATCH /api/operators/profile/password with wrong current password should return 400 Bad Request")
    void testChangePasswordWrongCurrentPassword() throws Exception {
        OperatorChangePasswordRequest request = OperatorChangePasswordRequest.builder()
                .currentPassword("WrongPassword@123")
                .newPassword("NewSecurePass@456")
                .confirmPassword("NewSecurePass@456")
                .build();

        doThrow(new BadRequestException("Current password is incorrect"))
                .when(operatorProfileService).changePassword(eq(1L), any(OperatorChangePasswordRequest.class));

        mockMvc.perform(patch("/api/operators/profile/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Current password is incorrect")));
    }
}
