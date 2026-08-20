package com.agrorental.operator;

import com.agrorental.common.exception.ForbiddenException;
import com.agrorental.common.exception.UnauthorizedException;
import com.agrorental.operator.dto.AuthenticatedOperatorResponse;
import com.agrorental.operator.dto.OperatorLoginRequest;
import com.agrorental.operator.dto.OperatorLoginResponse;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.mapper.OperatorDocumentMapper;
import com.agrorental.operator.mapper.OperatorMapper;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.operator.service.OperatorAuthService;
import com.agrorental.security.jwt.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OperatorAuthService Business Logic Unit Tests")
class OperatorAuthServiceTest {

    @Mock
    private OperatorRepository operatorRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private OperatorMapper operatorMapper;
    private OperatorAuthService operatorAuthService;

    private Operator approvedOperator;

    @BeforeEach
    void setUp() {
        operatorMapper = new OperatorMapper(new OperatorDocumentMapper());
        operatorAuthService = new OperatorAuthService(
                operatorRepository,
                passwordEncoder,
                jwtService,
                operatorMapper
        );

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
    @DisplayName("Should successfully authenticate approved operator with valid credentials")
    void testSuccessfulLogin() {
        OperatorLoginRequest request = OperatorLoginRequest.builder()
                .mobileNumber("9876543210")
                .password("Secret@123")
                .build();

        when(operatorRepository.findByMobileNumber("9876543210")).thenReturn(Optional.of(approvedOperator));
        when(passwordEncoder.matches("Secret@123", "$2a$10$encodedPasswordHash")).thenReturn(true);
        when(jwtService.generateOperatorToken(1L, "9876543210")).thenReturn("mocked.jwt.token");
        when(jwtService.getExpiresInSeconds()).thenReturn(3600L);

        OperatorLoginResponse response = operatorAuthService.login(request);

        assertNotNull(response);
        assertEquals("mocked.jwt.token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(3600L, response.getExpiresIn());

        AuthenticatedOperatorResponse operator = response.getOperator();
        assertNotNull(operator);
        assertEquals(1L, operator.getId());
        assertEquals("Rajesh Shinde", operator.getFullName());
        assertEquals("9876543210", operator.getMobileNumber());
        assertEquals("OPERATOR", operator.getRole());
        assertEquals(OperatorStatus.APPROVED, operator.getStatus());
        assertTrue(operator.isMobileVerified());
        assertTrue(operator.isActive());
    }

    @Test
    @DisplayName("Should reject login when mobile number is not registered")
    void testLoginUnknownMobile() {
        OperatorLoginRequest request = OperatorLoginRequest.builder()
                .mobileNumber("9000000000")
                .password("Secret@123")
                .build();

        when(operatorRepository.findByMobileNumber("9000000000")).thenReturn(Optional.empty());

        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () ->
                operatorAuthService.login(request)
        );

        assertEquals("Invalid mobile number or password", exception.getMessage());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("Should reject login when password does not match")
    void testLoginWrongPassword() {
        OperatorLoginRequest request = OperatorLoginRequest.builder()
                .mobileNumber("9876543210")
                .password("WrongPassword")
                .build();

        when(operatorRepository.findByMobileNumber("9876543210")).thenReturn(Optional.of(approvedOperator));
        when(passwordEncoder.matches("WrongPassword", "$2a$10$encodedPasswordHash")).thenReturn(false);

        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () ->
                operatorAuthService.login(request)
        );

        assertEquals("Invalid mobile number or password", exception.getMessage());
        verify(jwtService, never()).generateOperatorToken(anyLong(), anyString());
    }

    @Test
    @DisplayName("Should reject login when mobile is not verified")
    void testLoginUnverifiedMobile() {
        approvedOperator.setMobileVerified(false);

        OperatorLoginRequest request = OperatorLoginRequest.builder()
                .mobileNumber("9876543210")
                .password("Secret@123")
                .build();

        when(operatorRepository.findByMobileNumber("9876543210")).thenReturn(Optional.of(approvedOperator));
        when(passwordEncoder.matches("Secret@123", "$2a$10$encodedPasswordHash")).thenReturn(true);

        ForbiddenException exception = assertThrows(ForbiddenException.class, () ->
                operatorAuthService.login(request)
        );

        assertEquals("Mobile number is not verified", exception.getMessage());
    }

    @Test
    @DisplayName("Should reject login when operator status is PENDING")
    void testLoginPendingOperator() {
        approvedOperator.setStatus(OperatorStatus.PENDING);

        OperatorLoginRequest request = OperatorLoginRequest.builder()
                .mobileNumber("9876543210")
                .password("Secret@123")
                .build();

        when(operatorRepository.findByMobileNumber("9876543210")).thenReturn(Optional.of(approvedOperator));
        when(passwordEncoder.matches("Secret@123", "$2a$10$encodedPasswordHash")).thenReturn(true);

        ForbiddenException exception = assertThrows(ForbiddenException.class, () ->
                operatorAuthService.login(request)
        );

        assertEquals("Operator account is pending admin approval", exception.getMessage());
    }

    @Test
    @DisplayName("Should reject login when operator status is REJECTED")
    void testLoginRejectedOperator() {
        approvedOperator.setStatus(OperatorStatus.REJECTED);
        approvedOperator.setRejectionReason("Invalid driving license");

        OperatorLoginRequest request = OperatorLoginRequest.builder()
                .mobileNumber("9876543210")
                .password("Secret@123")
                .build();

        when(operatorRepository.findByMobileNumber("9876543210")).thenReturn(Optional.of(approvedOperator));
        when(passwordEncoder.matches("Secret@123", "$2a$10$encodedPasswordHash")).thenReturn(true);

        ForbiddenException exception = assertThrows(ForbiddenException.class, () ->
                operatorAuthService.login(request)
        );

        assertTrue(exception.getMessage().contains("Operator account has been rejected"));
        assertTrue(exception.getMessage().contains("Invalid driving license"));
    }

    @Test
    @DisplayName("Should reject login when operator account is inactive")
    void testLoginInactiveOperator() {
        approvedOperator.setActive(false);

        OperatorLoginRequest request = OperatorLoginRequest.builder()
                .mobileNumber("9876543210")
                .password("Secret@123")
                .build();

        when(operatorRepository.findByMobileNumber("9876543210")).thenReturn(Optional.of(approvedOperator));
        when(passwordEncoder.matches("Secret@123", "$2a$10$encodedPasswordHash")).thenReturn(true);

        ForbiddenException exception = assertThrows(ForbiddenException.class, () ->
                operatorAuthService.login(request)
        );

        assertEquals("Operator account is inactive", exception.getMessage());
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
    }

    @Test
    @DisplayName("Should reject getCurrentOperator when operator is missing or inactive")
    void testGetCurrentOperatorInactive() {
        approvedOperator.setActive(false);
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(approvedOperator));

        assertThrows(ForbiddenException.class, () -> operatorAuthService.getCurrentOperator(1L));
    }
}
