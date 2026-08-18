package com.agrorental.partner;

import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.partner.dto.PartnerChangePasswordRequest;
import com.agrorental.partner.dto.PartnerProfileResponse;
import com.agrorental.partner.dto.PartnerProfileUpdateRequest;
import com.agrorental.partner.dto.PartnerRegistrationRequest;
import com.agrorental.partner.entity.Partner;
import com.agrorental.partner.repository.PartnerRepository;
import com.agrorental.partner.service.PartnerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PartnerService Business Logic Unit Tests")
class PartnerServiceTest {

    @Mock
    private PartnerRepository partnerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PartnerService partnerService;

    private Partner testPartner;

    @BeforeEach
    void setUp() {
        testPartner = Partner.builder()
                .fullName("Rajesh Patel")
                .businessName("Patel Agro Fleet")
                .mobileNumber("9876543210")
                .email("rajesh.patel@example.com")
                .address("Plot 42, Agro Industrial Hub, Pune")
                .gstNumber("27AABCP1234F1Z5")
                .panNumber("ABCDE1234F")
                .aadhaarNumber("987654321012")
                .password("encoded_secret_pass")
                .profilePhoto("https://example.com/avatar.jpg")
                .otpVerified(true)
                .verificationStatus(Partner.VerificationStatus.APPROVED)
                .build();
        testPartner.setId(1L);
    }

    @Test
    @DisplayName("Should successfully register new partner")
    void shouldRegisterPartner() {
        PartnerRegistrationRequest request = new PartnerRegistrationRequest();
        request.setFullName("Rajesh Patel");
        request.setMobileNumber("9876543210");
        request.setEmail("rajesh.patel@example.com");
        request.setPassword("plain_secret_pass");

        when(partnerRepository.existsByMobileNumber("9876543210")).thenReturn(false);
        when(partnerRepository.existsByEmail("rajesh.patel@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plain_secret_pass")).thenReturn("encoded_secret_pass");
        when(partnerRepository.save(any(Partner.class))).thenReturn(testPartner);

        Partner registered = partnerService.registerPartner(request);

        assertThat(registered).isNotNull();
        assertThat(registered.getId()).isEqualTo(1L);
        assertThat(registered.getFullName()).isEqualTo("Rajesh Patel");
        verify(partnerRepository).save(any(Partner.class));
    }

    @Test
    @DisplayName("Should reject registration if mobile number is already registered")
    void shouldThrowExceptionWhenDuplicateMobile() {
        PartnerRegistrationRequest request = new PartnerRegistrationRequest();
        request.setFullName("Rajesh Patel");
        request.setMobileNumber("9876543210");
        request.setPassword("secret");

        when(partnerRepository.existsByMobileNumber("9876543210")).thenReturn(true);

        assertThatThrownBy(() -> partnerService.registerPartner(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Mobile number already registered");
    }

    @Test
    @DisplayName("Should successfully fetch partner profile")
    void shouldGetPartnerProfile() {
        when(partnerRepository.findById(1L)).thenReturn(Optional.of(testPartner));

        PartnerProfileResponse response = partnerService.getPartnerProfile(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getFullName()).isEqualTo("Rajesh Patel");
        assertThat(response.getBusinessName()).isEqualTo("Patel Agro Fleet");
        assertThat(response.getVerificationStatus()).isEqualTo(Partner.VerificationStatus.APPROVED);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when partner ID does not exist")
    void shouldThrowNotFoundWhenPartnerDoesNotExist() {
        when(partnerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> partnerService.getPartnerProfile(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Partner profile not found with ID: 99");
    }

    @Test
    @DisplayName("Should update partner profile details cleanly")
    void shouldUpdatePartnerProfile() {
        PartnerProfileUpdateRequest updateRequest = PartnerProfileUpdateRequest.builder()
                .fullName("Rajesh V. Patel")
                .businessName("Patel & Sons Agro Tech")
                .email("rajesh.new@example.com")
                .address("New Hub, Nashik")
                .gstNumber("27AABCP1234F1Z9")
                .panNumber("ABCDE1234G")
                .aadhaarNumber("987654321012")
                .build();

        when(partnerRepository.findById(1L)).thenReturn(Optional.of(testPartner));
        when(partnerRepository.findByEmail("rajesh.new@example.com")).thenReturn(Optional.empty());
        when(partnerRepository.save(any(Partner.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PartnerProfileResponse updated = partnerService.updatePartnerProfile(1L, updateRequest);

        assertThat(updated).isNotNull();
        assertThat(updated.getFullName()).isEqualTo("Rajesh V. Patel");
        assertThat(updated.getBusinessName()).isEqualTo("Patel & Sons Agro Tech");
        assertThat(updated.getEmail()).isEqualTo("rajesh.new@example.com");
    }

    @Test
    @DisplayName("Should change password successfully when current password matches")
    void shouldChangePassword() {
        PartnerChangePasswordRequest request = PartnerChangePasswordRequest.builder()
                .currentPassword("old_secret")
                .newPassword("new_strong_secret_123")
                .build();

        when(partnerRepository.findById(1L)).thenReturn(Optional.of(testPartner));
        when(passwordEncoder.matches("old_secret", "encoded_secret_pass")).thenReturn(true);
        when(passwordEncoder.matches("new_strong_secret_123", "encoded_secret_pass")).thenReturn(false);
        when(passwordEncoder.encode("new_strong_secret_123")).thenReturn("new_encoded_hash");

        partnerService.changePartnerPassword(1L, request);

        verify(partnerRepository).save(testPartner);
        assertThat(testPartner.getPassword()).isEqualTo("new_encoded_hash");
    }

    @Test
    @DisplayName("Should throw BadRequestException when current password is wrong")
    void shouldRejectPasswordChangeWhenCurrentPasswordWrong() {
        PartnerChangePasswordRequest request = PartnerChangePasswordRequest.builder()
                .currentPassword("wrong_password")
                .newPassword("new_strong_secret_123")
                .build();

        when(partnerRepository.findById(1L)).thenReturn(Optional.of(testPartner));
        when(passwordEncoder.matches("wrong_password", "encoded_secret_pass")).thenReturn(false);

        assertThatThrownBy(() -> partnerService.changePartnerPassword(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Current password provided is incorrect");
    }
}
