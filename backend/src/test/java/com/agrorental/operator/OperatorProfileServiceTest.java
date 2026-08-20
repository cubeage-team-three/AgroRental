package com.agrorental.operator;

import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.ForbiddenException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.operator.dto.OperatorChangePasswordRequest;
import com.agrorental.operator.dto.OperatorProfileResponse;
import com.agrorental.operator.dto.OperatorProfileUpdateRequest;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.mapper.OperatorDocumentMapper;
import com.agrorental.operator.mapper.OperatorMapper;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.operator.service.OperatorProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OperatorProfileService Business Logic Unit Tests")
class OperatorProfileServiceTest {

    @Mock
    private OperatorRepository operatorRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private OperatorMapper operatorMapper;
    private OperatorProfileService operatorProfileService;

    private Operator testOperator;

    @BeforeEach
    void setUp() {
        operatorMapper = new OperatorMapper(new OperatorDocumentMapper());
        operatorProfileService = new OperatorProfileService(
                operatorRepository,
                operatorMapper,
                passwordEncoder
        );

        testOperator = Operator.builder()
                .fullName("Sunil Jadhav")
                .mobileNumber("9876543210")
                .email("sunil@agrorental.com")
                .address("Nashik, Maharashtra")
                .experience(7)
                .skills("Tractor Ploughing, Seeder")
                .aadhaarNumber("123456789012")
                .drivingLicenseNumber("MH1520210098765")
                .password("$2a$10$encodedOldPasswordHash")
                .status(OperatorStatus.APPROVED)
                .mobileVerified(true)
                .build();
        testOperator.setId(1L);
        testOperator.setActive(true);
    }

    @Test
    @DisplayName("Should successfully retrieve active approved operator profile")
    void testGetCurrentProfileSuccess() {
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));

        OperatorProfileResponse response = operatorProfileService.getCurrentProfile(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getFullName()).isEqualTo("Sunil Jadhav");
        assertThat(response.getMobileNumber()).isEqualTo("9876543210");
        assertThat(response.getEmail()).isEqualTo("sunil@agrorental.com");
        assertThat(response.getAddress()).isEqualTo("Nashik, Maharashtra");
        assertThat(response.getExperience()).isEqualTo(7);
        assertThat(response.getSkills()).isEqualTo("Tractor Ploughing, Seeder");
        assertThat(response.getMaskedAadhaarNumber()).isEqualTo("XXXX-XXXX-9012");
        assertThat(response.getMaskedDrivingLicenseNumber()).isEqualTo("DL-XXXX-8765");
        assertThat(response.getStatus()).isEqualTo(OperatorStatus.APPROVED);
        assertThat(response.isMobileVerified()).isTrue();
        assertThat(response.isActive()).isTrue();
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when operator does not exist")
    void testGetCurrentProfileNotFound() {
        when(operatorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> operatorProfileService.getCurrentProfile(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Operator not found with ID: 99");
    }

    @Test
    @DisplayName("Should throw ForbiddenException when operator is inactive")
    void testGetCurrentProfileInactive() {
        testOperator.setActive(false);
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));

        assertThatThrownBy(() -> operatorProfileService.getCurrentProfile(1L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Operator account is inactive");
    }

    @Test
    @DisplayName("Should throw ForbiddenException when operator status is PENDING")
    void testGetCurrentProfilePending() {
        testOperator.setStatus(OperatorStatus.PENDING);
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));

        assertThatThrownBy(() -> operatorProfileService.getCurrentProfile(1L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Operator account is not approved");
    }

    @Test
    @DisplayName("Should successfully update permitted editable profile fields and preserve immutables")
    void testUpdateCurrentProfileSuccess() {
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));
        when(operatorRepository.existsByEmailAndIdNot("sunil.new@agrorental.com", 1L)).thenReturn(false);
        when(operatorRepository.save(any(Operator.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OperatorProfileUpdateRequest request = OperatorProfileUpdateRequest.builder()
                .fullName("Sunil R. Jadhav")
                .email("sunil.new@agrorental.com")
                .address("Aurangabad, Maharashtra")
                .experience(9)
                .skills("Tractor, Combine Harvester, Drone Spraying")
                .profilePhoto("https://cdn.agrorental.com/photo.jpg")
                .build();

        OperatorProfileResponse response = operatorProfileService.updateCurrentProfile(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.getFullName()).isEqualTo("Sunil R. Jadhav");
        assertThat(response.getEmail()).isEqualTo("sunil.new@agrorental.com");
        assertThat(response.getAddress()).isEqualTo("Aurangabad, Maharashtra");
        assertThat(response.getExperience()).isEqualTo(9);
        assertThat(response.getSkills()).isEqualTo("Tractor, Combine Harvester, Drone Spraying");
        assertThat(response.getProfilePhoto()).isEqualTo("https://cdn.agrorental.com/photo.jpg");

        // Assert immutable fields remain completely untouched
        assertThat(testOperator.getMobileNumber()).isEqualTo("9876543210");
        assertThat(testOperator.getAadhaarNumber()).isEqualTo("123456789012");
        assertThat(testOperator.getDrivingLicenseNumber()).isEqualTo("MH1520210098765");
        assertThat(testOperator.getStatus()).isEqualTo(OperatorStatus.APPROVED);
        assertThat(testOperator.isActive()).isTrue();
        assertThat(testOperator.isMobileVerified()).isTrue();
        assertThat(testOperator.getPassword()).isEqualTo("$2a$10$encodedOldPasswordHash");

        verify(operatorRepository).save(testOperator);
    }

    @Test
    @DisplayName("Should reject email update when email already exists on another operator account")
    void testUpdateProfileDuplicateEmailRejected() {
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));
        when(operatorRepository.existsByEmailAndIdNot("taken@agrorental.com", 1L)).thenReturn(true);

        OperatorProfileUpdateRequest request = OperatorProfileUpdateRequest.builder()
                .fullName("Sunil Jadhav")
                .email("taken@agrorental.com")
                .address("Nashik, Maharashtra")
                .experience(7)
                .skills("Tractor")
                .build();

        assertThatThrownBy(() -> operatorProfileService.updateCurrentProfile(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email is already registered by another account");

        verify(operatorRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should successfully change password when current password matches and new password meets criteria")
    void testChangePasswordSuccess() {
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));
        when(passwordEncoder.matches("OldPassword@123", "$2a$10$encodedOldPasswordHash")).thenReturn(true);
        when(passwordEncoder.matches("NewSecurePass@456", "$2a$10$encodedOldPasswordHash")).thenReturn(false);
        when(passwordEncoder.encode("NewSecurePass@456")).thenReturn("$2a$10$encodedNewPasswordHash");

        OperatorChangePasswordRequest request = OperatorChangePasswordRequest.builder()
                .currentPassword("OldPassword@123")
                .newPassword("NewSecurePass@456")
                .confirmPassword("NewSecurePass@456")
                .build();

        operatorProfileService.changePassword(1L, request);

        assertThat(testOperator.getPassword()).isEqualTo("$2a$10$encodedNewPasswordHash");
        verify(operatorRepository).save(testOperator);
    }

    @Test
    @DisplayName("Should reject password change when newPassword and confirmPassword do not match")
    void testChangePasswordMismatch() {
        OperatorChangePasswordRequest request = OperatorChangePasswordRequest.builder()
                .currentPassword("OldPassword@123")
                .newPassword("NewSecurePass@456")
                .confirmPassword("MismatchedPass@789")
                .build();

        assertThatThrownBy(() -> operatorProfileService.changePassword(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("New passwords do not match");

        verify(operatorRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject password change when newPassword is less than 8 characters")
    void testChangePasswordTooShort() {
        OperatorChangePasswordRequest request = OperatorChangePasswordRequest.builder()
                .currentPassword("OldPassword@123")
                .newPassword("short")
                .confirmPassword("short")
                .build();

        assertThatThrownBy(() -> operatorProfileService.changePassword(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("New password must be at least 8 characters long");

        verify(operatorRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject password change when currentPassword is wrong")
    void testChangePasswordWrongCurrentPassword() {
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));
        when(passwordEncoder.matches("WrongPassword@123", "$2a$10$encodedOldPasswordHash")).thenReturn(false);

        OperatorChangePasswordRequest request = OperatorChangePasswordRequest.builder()
                .currentPassword("WrongPassword@123")
                .newPassword("NewSecurePass@456")
                .confirmPassword("NewSecurePass@456")
                .build();

        assertThatThrownBy(() -> operatorProfileService.changePassword(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Current password is incorrect");

        verify(operatorRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject password change when newPassword is same as current password")
    void testChangePasswordSameAsCurrent() {
        when(operatorRepository.findById(1L)).thenReturn(Optional.of(testOperator));
        when(passwordEncoder.matches("SamePassword@123", "$2a$10$encodedOldPasswordHash")).thenReturn(true);

        OperatorChangePasswordRequest request = OperatorChangePasswordRequest.builder()
                .currentPassword("SamePassword@123")
                .newPassword("SamePassword@123")
                .confirmPassword("SamePassword@123")
                .build();

        assertThatThrownBy(() -> operatorProfileService.changePassword(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("New password must be different from current password");

        verify(operatorRepository, never()).save(any());
    }
}
