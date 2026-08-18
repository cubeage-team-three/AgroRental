package com.agrorental.operator;

import com.agrorental.common.security.JwtTokenProvider;
import com.agrorental.operator.controller.OperatorController;
import com.agrorental.operator.dto.OperatorProfileResponse;
import com.agrorental.operator.dto.OperatorProfileUpdateRequest;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.repository.OperatorDocumentRepository;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.operator.service.OperatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OperatorProfileTest {

    @Mock
    private OperatorRepository operatorRepository;

    @Mock
    private OperatorDocumentRepository operatorDocumentRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    private OperatorService operatorService;
    private OperatorController operatorController;

    private Operator testOperator;
    private String validToken;

    @BeforeEach
    void setUp() {
        operatorService = new OperatorService(
                operatorRepository,
                operatorDocumentRepository,
                passwordEncoder,
                "target/test_uploads/operator_photos"
        );
        operatorService.init();
        operatorController = new OperatorController(operatorService, jwtTokenProvider);

        testOperator = Operator.builder()
                .fullName("Vikram Singh")
                .mobileNumber("9876543210")
                .email("vikram.singh@agro.com")
                .address("Village Raipur, District Karnal, Haryana")
                .aadhaarNumber("123456789012")
                .drivingLicenseNumber("DL-HR05-2021-998877")
                .experience(5)
                .skills("Tractor 4WD, Combine Harvester, Rotavator")
                .password("$2a$10$hashedpassword")
                .status(OperatorStatus.APPROVED)
                .mobileVerified(true)
                .documentsSubmitted(true)
                .build();
        testOperator.setId(101L);

        validToken = "valid.jwt.token";
    }

    @Test
    @DisplayName("Should retrieve operator profile successfully with valid Bearer token")
    void testGetOperatorProfile_Success() {
        when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getOperatorIdFromToken(validToken)).thenReturn(101L);
        when(operatorRepository.findById(101L)).thenReturn(Optional.of(testOperator));
        when(operatorDocumentRepository.findAllByOperatorId(101L)).thenReturn(Collections.emptyList());

        ResponseEntity<?> responseEntity = operatorController.getProfile("Bearer " + validToken);

        assertNotNull(responseEntity);
        assertEquals(200, responseEntity.getStatusCode().value());
        verify(operatorRepository, times(1)).findById(101L);
    }

    @Test
    @DisplayName("Should reject profile retrieval without valid Bearer token")
    void testGetOperatorProfile_Unauthorized() {
        assertThrows(RuntimeException.class, () -> {
            operatorController.getProfile(null);
        });

        assertThrows(RuntimeException.class, () -> {
            operatorController.getProfile("InvalidHeader");
        });
    }

    @Test
    @DisplayName("Should update operator profile fields successfully")
    void testUpdateOperatorProfile_Success() {
        when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getOperatorIdFromToken(validToken)).thenReturn(101L);
        when(operatorRepository.findById(101L)).thenReturn(Optional.of(testOperator));
        when(operatorRepository.save(any(Operator.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(operatorDocumentRepository.findAllByOperatorId(101L)).thenReturn(Collections.emptyList());

        OperatorProfileUpdateRequest updateRequest = OperatorProfileUpdateRequest.builder()
                .fullName("Vikram S. Chauhan")
                .email("vikram.chauhan@agro.com")
                .address("New Farm Colony, Karnal")
                .experience(7)
                .skills("Tractor, Harvester, Drone Sprayer")
                .build();

        ResponseEntity<?> response = operatorController.updateProfile("Bearer " + validToken, updateRequest);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals("Vikram S. Chauhan", testOperator.getFullName());
        assertEquals("vikram.chauhan@agro.com", testOperator.getEmail());
        assertEquals(7, testOperator.getExperience());
        assertEquals("9876543210", testOperator.getMobileNumber()); // Mobile number remains unchanged
        verify(operatorRepository, times(1)).save(testOperator);
    }

    @Test
    @DisplayName("Should upload profile photo successfully")
    void testUploadProfilePhoto_Success() {
        when(jwtTokenProvider.validateToken(validToken)).thenReturn(true);
        when(jwtTokenProvider.getOperatorIdFromToken(validToken)).thenReturn(101L);
        when(operatorRepository.findById(101L)).thenReturn(Optional.of(testOperator));
        when(operatorRepository.save(any(Operator.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(operatorDocumentRepository.findAllByOperatorId(101L)).thenReturn(Collections.emptyList());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.jpg",
                "image/jpeg",
                "dummy image content".getBytes()
        );

        ResponseEntity<?> response = operatorController.uploadProfilePhoto("Bearer " + validToken, file);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(testOperator.getProfilePhoto());
        assertTrue(testOperator.getProfilePhoto().startsWith("photo_101_"));
        verify(operatorRepository, times(1)).save(testOperator);
    }
}
