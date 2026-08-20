package com.agrorental.operator;

import com.agrorental.common.exception.BadRequestException;
import com.agrorental.operator.dto.OperatorRegistrationRequest;
import com.agrorental.operator.dto.OperatorResponse;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.mapper.OperatorMapper;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.operator.service.OperatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OperatorService Business Logic Unit Tests")
class OperatorServiceTest {

    @Mock
    private OperatorRepository operatorRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Spy
    private OperatorMapper operatorMapper = new OperatorMapper();

    @InjectMocks
    private OperatorService operatorService;

    private OperatorRegistrationRequest testRequest;

    @BeforeEach
    void setUp() {
        testRequest = new OperatorRegistrationRequest();
        testRequest.setFullName("Suresh Shinde");
        testRequest.setMobileNumber("9876543210");
        testRequest.setEmail("suresh@example.com");
        testRequest.setAddress("Village Khed, Pune");
        testRequest.setAadhaarNumber("123456789012");
        testRequest.setDrivingLicenseNumber("MH1220200012345");
        testRequest.setExperience(5);
        testRequest.setSkills("Tractor & Rotavator");
        testRequest.setPassword("SecurePass@123");
        testRequest.setProfilePhoto("photo.jpg");
    }

    @Test
    @DisplayName("registerOperator() - Should successfully register new operator and return safe OperatorResponse")
    void shouldRegisterOperatorSuccessfully() {
        when(operatorRepository.existsByMobileNumber("9876543210")).thenReturn(false);
        when(operatorRepository.existsByEmail("suresh@example.com")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass@123")).thenReturn("bcrypt_hashed_pass");

        when(operatorRepository.save(any(Operator.class))).thenAnswer(invocation -> {
            Operator saved = invocation.getArgument(0);
            saved.setId(1L);
            saved.setCreatedAt(LocalDateTime.now());
            saved.setUpdatedAt(LocalDateTime.now());
            saved.setActive(true);
            return saved;
        });

        OperatorResponse response = operatorService.registerOperator(testRequest);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Suresh Shinde", response.getFullName());
        assertEquals("9876543210", response.getMobileNumber());
        assertEquals("suresh@example.com", response.getEmail());
        assertEquals(OperatorStatus.PENDING, response.getStatus());
        assertTrue(response.isActive());

        verify(operatorRepository).existsByMobileNumber("9876543210");
        verify(operatorRepository).existsByEmail("suresh@example.com");
        verify(passwordEncoder).encode("SecurePass@123");
        verify(operatorRepository).save(any(Operator.class));
    }

    @Test
    @DisplayName("registerOperator() - Should throw BadRequestException when mobile number is already registered")
    void shouldThrowBadRequestWhenMobileAlreadyExists() {
        when(operatorRepository.existsByMobileNumber("9876543210")).thenReturn(true);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> operatorService.registerOperator(testRequest)
        );

        assertEquals("Mobile number is already registered", ex.getMessage());
        verify(operatorRepository).existsByMobileNumber("9876543210");
        verify(operatorRepository, never()).save(any(Operator.class));
    }

    @Test
    @DisplayName("registerOperator() - Should throw BadRequestException when email is already registered")
    void shouldThrowBadRequestWhenEmailAlreadyExists() {
        when(operatorRepository.existsByMobileNumber("9876543210")).thenReturn(false);
        when(operatorRepository.existsByEmail("suresh@example.com")).thenReturn(true);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> operatorService.registerOperator(testRequest)
        );

        assertEquals("Email is already registered", ex.getMessage());
        verify(operatorRepository).existsByMobileNumber("9876543210");
        verify(operatorRepository).existsByEmail("suresh@example.com");
        verify(operatorRepository, never()).save(any(Operator.class));
    }
}
