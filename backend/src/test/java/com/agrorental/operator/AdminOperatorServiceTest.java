package com.agrorental.operator;

import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.operator.dto.OperatorDetailResponse;
import com.agrorental.operator.dto.OperatorSummaryResponse;
import com.agrorental.operator.dto.OperatorVerificationRequest;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.mapper.OperatorDocumentMapper;
import com.agrorental.operator.mapper.OperatorMapper;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.operator.service.AdminOperatorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminOperatorService Unit Tests")
class AdminOperatorServiceTest {

    @Mock
    private OperatorRepository operatorRepository;

    @Spy
    private OperatorMapper operatorMapper = new OperatorMapper(new OperatorDocumentMapper());

    @InjectMocks
    private AdminOperatorService adminOperatorService;

    private Operator pendingVerifiedOperator;
    private Operator pendingUnverifiedOperator;
    private Operator approvedOperator;

    @BeforeEach
    void setUp() {
        pendingVerifiedOperator = Operator.builder()
                .fullName("Suresh Shinde")
                .mobileNumber("9876543210")
                .email("suresh@example.com")
                .address("Village Khed")
                .aadhaarNumber("123456789012")
                .drivingLicenseNumber("MH1220200012345")
                .experience(5)
                .skills("Tractor")
                .status(OperatorStatus.PENDING)
                .mobileVerified(true)
                .documents(new ArrayList<>())
                .build();
        pendingVerifiedOperator.setId(1L);
        pendingVerifiedOperator.setCreatedAt(LocalDateTime.now());

        pendingUnverifiedOperator = Operator.builder()
                .fullName("Ramesh Gaikwad")
                .mobileNumber("9876543211")
                .email("ramesh@example.com")
                .address("Village Baramati")
                .aadhaarNumber("123456789013")
                .drivingLicenseNumber("MH1220200012346")
                .experience(3)
                .skills("Harvester")
                .status(OperatorStatus.PENDING)
                .mobileVerified(false)
                .documents(new ArrayList<>())
                .build();
        pendingUnverifiedOperator.setId(2L);
        pendingUnverifiedOperator.setCreatedAt(LocalDateTime.now());

        approvedOperator = Operator.builder()
                .fullName("Anil Pawar")
                .mobileNumber("9876543212")
                .email("anil@example.com")
                .status(OperatorStatus.APPROVED)
                .mobileVerified(true)
                .documents(new ArrayList<>())
                .build();
        approvedOperator.setId(3L);
    }

    @Test
    @DisplayName("getOperators() - Should return paginated OperatorSummaryResponse list")
    void shouldReturnPaginatedOperators() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Operator> page = new PageImpl<>(Collections.singletonList(pendingVerifiedOperator), pageable, 1);

        when(operatorRepository.findByStatus(OperatorStatus.PENDING, pageable)).thenReturn(page);

        Page<OperatorSummaryResponse> result = adminOperatorService.getOperators(OperatorStatus.PENDING, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Suresh Shinde", result.getContent().get(0).getFullName());
        assertTrue(result.getContent().get(0).isMobileVerified());
    }

    @Test
    @DisplayName("getOperatorDetail() - Should return full detail response with masked IDs")
    void shouldReturnOperatorDetail() {
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(pendingVerifiedOperator));

        OperatorDetailResponse detail = adminOperatorService.getOperatorDetail(1L);

        assertNotNull(detail);
        assertEquals(1L, detail.getId());
        assertEquals("XXXX-XXXX-9012", detail.getMaskedAadhaarNumber());
        assertEquals("DL-XXXX-2345", detail.getMaskedDrivingLicenseNumber());
    }

    @Test
    @DisplayName("verifyOperator() - Should successfully approve PENDING and mobile-verified operator")
    void shouldApprovePendingVerifiedOperator() {
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(pendingVerifiedOperator));
        when(operatorRepository.save(any(Operator.class))).thenAnswer(i -> i.getArgument(0));

        OperatorVerificationRequest request = OperatorVerificationRequest.builder()
                .status(OperatorStatus.APPROVED)
                .build();

        OperatorDetailResponse response = adminOperatorService.verifyOperator(1L, request);

        assertNotNull(response);
        assertEquals(OperatorStatus.APPROVED, response.getStatus());
        assertTrue(response.isActive());
        assertNull(response.getRejectionReason());
        verify(operatorRepository).save(pendingVerifiedOperator);
    }

    @Test
    @DisplayName("verifyOperator() - Should reject approval if operator mobile is NOT verified")
    void shouldRejectApprovalIfMobileNotVerified() {
        when(operatorRepository.findById(2L)).thenReturn(Optional.of(pendingUnverifiedOperator));

        OperatorVerificationRequest request = OperatorVerificationRequest.builder()
                .status(OperatorStatus.APPROVED)
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> adminOperatorService.verifyOperator(2L, request));

        assertTrue(ex.getMessage().contains("mobile number must be verified"));
        verify(operatorRepository, never()).save(any(Operator.class));
    }

    @Test
    @DisplayName("verifyOperator() - Should successfully reject PENDING operator and record rejection reason")
    void shouldRejectPendingOperatorWithReason() {
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(pendingVerifiedOperator));
        when(operatorRepository.save(any(Operator.class))).thenAnswer(i -> i.getArgument(0));

        OperatorVerificationRequest request = OperatorVerificationRequest.builder()
                .status(OperatorStatus.REJECTED)
                .rejectionReason("Driving license image blurry and illegible")
                .build();

        OperatorDetailResponse response = adminOperatorService.verifyOperator(1L, request);

        assertNotNull(response);
        assertEquals(OperatorStatus.REJECTED, response.getStatus());
        assertFalse(response.isActive());
        assertEquals("Driving license image blurry and illegible", response.getRejectionReason());
        verify(operatorRepository).save(pendingVerifiedOperator);
    }

    @Test
    @DisplayName("verifyOperator() - Should fail if operator is already APPROVED")
    void shouldDisallowReverificationOfApprovedOperator() {
        when(operatorRepository.findById(3L)).thenReturn(Optional.of(approvedOperator));

        OperatorVerificationRequest request = OperatorVerificationRequest.builder()
                .status(OperatorStatus.APPROVED)
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> adminOperatorService.verifyOperator(3L, request));

        assertTrue(ex.getMessage().contains("current status is: APPROVED"));
        verify(operatorRepository, never()).save(any(Operator.class));
    }

    @Test
    @DisplayName("verifyOperator() - Should throw ResourceNotFoundException if operator not found")
    void shouldThrowNotFoundForNonExistentOperator() {
        when(operatorRepository.findById(999L)).thenReturn(Optional.empty());

        OperatorVerificationRequest request = OperatorVerificationRequest.builder()
                .status(OperatorStatus.APPROVED)
                .build();

        assertThrows(ResourceNotFoundException.class,
                () -> adminOperatorService.verifyOperator(999L, request));
    }
}
