package com.agrorental.operator;

import com.agrorental.common.exception.ForbiddenException;
import com.agrorental.common.exception.UnauthorizedException;
import com.agrorental.operator.dto.AuthenticatedOperatorResponse;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.mapper.OperatorDocumentMapper;
import com.agrorental.operator.mapper.OperatorMapper;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.operator.service.OperatorAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Operator session identity. Operator credential checks moved to AuthService (POST /api/auth/login, role
 * OPERATOR); their scenarios (success, unknown mobile, wrong password, pending, rejected, inactive and the
 * no-longer-required mobile verification) are covered by
 * {@code com.agrorental.auth.OperatorAuthenticationRegressionTest} and
 * {@code com.agrorental.auth.PasswordLoginIntegrationTest}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OperatorAuthService Business Logic Unit Tests")
class OperatorAuthServiceTest {

    @Mock
    private OperatorRepository operatorRepository;

    private OperatorAuthService operatorAuthService;

    private Operator approvedOperator;

    @BeforeEach
    void setUp() {
        operatorAuthService = new OperatorAuthService(operatorRepository, new OperatorMapper(new OperatorDocumentMapper()));

        approvedOperator = Operator.builder()
                .fullName("Rajesh Shinde")
                .mobileNumber("9876543210")
                .email("rajesh@agrorental.com")
                .password("$2a$10$encodedPasswordHash")
                .aadhaarNumber("123456789012")
                .drivingLicenseNumber("MH1220200012345")
                .experience(5)
                .skills("Tractor Operation, Combine Harvester")
                .status(OperatorStatus.APPROVED)
                .mobileVerified(true)
                .build();
        approvedOperator.setId(1L);
        approvedOperator.setActive(true);
    }

    @Test
    @DisplayName("OperatorAuthService no longer exposes a login method (single login: POST /api/auth/login)")
    void noDuplicateLoginImplementation() {
        assertTrue(Arrays.stream(OperatorAuthService.class.getDeclaredMethods())
                        .noneMatch(method -> method.getName().toLowerCase().contains("login")),
                "Operator credentials must only be checked by AuthService");
    }

    @Test
    @DisplayName("Should retrieve current authenticated operator identity successfully")
    void testGetCurrentOperator() {
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(approvedOperator));

        AuthenticatedOperatorResponse response = operatorAuthService.getCurrentOperator(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Rajesh Shinde", response.getFullName());
        assertEquals("OPERATOR", response.getRole());
        assertEquals(OperatorStatus.APPROVED, response.getStatus());
        assertTrue(response.isActive());
    }

    @Test
    @DisplayName("An approved operator whose mobile was never OTP-verified still resolves (mobileVerified is informational)")
    void testGetCurrentOperatorWithoutMobileVerification() {
        approvedOperator.setMobileVerified(false);
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(approvedOperator));

        AuthenticatedOperatorResponse response = operatorAuthService.getCurrentOperator(1L);

        assertEquals(1L, response.getId());
        assertFalse(response.isMobileVerified());
    }

    @Test
    @DisplayName("Should reject getCurrentOperator when operator is missing or inactive")
    void testGetCurrentOperatorInactive() {
        approvedOperator.setActive(false);
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(approvedOperator));

        assertThrows(ForbiddenException.class, () -> operatorAuthService.getCurrentOperator(1L));
    }

    @Test
    @DisplayName("Should reject getCurrentOperator for a PENDING or REJECTED operator")
    void testGetCurrentOperatorNotApproved() {
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(approvedOperator));

        approvedOperator.setStatus(OperatorStatus.PENDING);
        assertThrows(ForbiddenException.class, () -> operatorAuthService.getCurrentOperator(1L));

        approvedOperator.setStatus(OperatorStatus.REJECTED);
        assertThrows(ForbiddenException.class, () -> operatorAuthService.getCurrentOperator(1L));
    }

    @Test
    @DisplayName("Should reject getCurrentOperator without an operator ID or for an unknown operator")
    void testGetCurrentOperatorUnknown() {
        assertThrows(UnauthorizedException.class, () -> operatorAuthService.getCurrentOperator(null));

        when(operatorRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(UnauthorizedException.class, () -> operatorAuthService.getCurrentOperator(99L));
    }
}
