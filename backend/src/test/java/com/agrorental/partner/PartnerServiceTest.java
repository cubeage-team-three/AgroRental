package com.agrorental.partner;

import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.partner.dto.PartnerChangePasswordRequest;
import com.agrorental.partner.dto.PartnerDashboardResponse;
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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

    // =========================================================
    // REGISTRATION
    // =========================================================

    @Test
    @DisplayName("Should successfully register new partner")
    void shouldRegisterPartner() {

        PartnerRegistrationRequest request = new PartnerRegistrationRequest();

        request.setFullName("Rajesh Patel");
        request.setMobileNumber("9876543210");
        request.setEmail("rajesh.patel@example.com");
        request.setPassword("plain_secret_pass");

        when(partnerRepository.existsByMobileNumber("9876543210"))
                .thenReturn(false);

        when(partnerRepository.existsByEmail("rajesh.patel@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("plain_secret_pass"))
                .thenReturn("encoded_secret_pass");

        when(partnerRepository.save(any(Partner.class)))
                .thenReturn(testPartner);

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

        when(partnerRepository.existsByMobileNumber("9876543210"))
                .thenReturn(true);

        assertThatThrownBy(() ->
                partnerService.registerPartner(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Mobile number already registered");
    }

    // =========================================================
    // PROFILE
    // =========================================================

    @Test
    @DisplayName("Should successfully fetch partner profile")
    void shouldGetPartnerProfile() {

        when(partnerRepository.findById(1L))
                .thenReturn(Optional.of(testPartner));

        PartnerProfileResponse response =
                partnerService.getPartnerProfile(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getFullName()).isEqualTo("Rajesh Patel");
        assertThat(response.getBusinessName())
                .isEqualTo("Patel Agro Fleet");
        assertThat(response.getVerificationStatus())
                .isEqualTo(Partner.VerificationStatus.APPROVED);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when partner ID does not exist")
    void shouldThrowNotFoundWhenPartnerDoesNotExist() {

        when(partnerRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                partnerService.getPartnerProfile(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(
                        "Partner profile not found with ID: 99"
                );
    }

    @Test
    @DisplayName("Should update partner profile details cleanly")
    void shouldUpdatePartnerProfile() {

        PartnerProfileUpdateRequest updateRequest =
                PartnerProfileUpdateRequest.builder()
                        .fullName("Rajesh V. Patel")
                        .businessName("Patel & Sons Agro Tech")
                        .email("rajesh.new@example.com")
                        .address("New Hub, Nashik")
                        .gstNumber("27AABCP1234F1Z9")
                        .panNumber("ABCDE1234G")
                        .aadhaarNumber("987654321012")
                        .build();

        when(partnerRepository.findById(1L))
                .thenReturn(Optional.of(testPartner));

        when(partnerRepository.findByEmail("rajesh.new@example.com"))
                .thenReturn(Optional.empty());

        when(partnerRepository.save(any(Partner.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PartnerProfileResponse updated =
                partnerService.updatePartnerProfile(1L, updateRequest);

        assertThat(updated).isNotNull();
        assertThat(updated.getFullName())
                .isEqualTo("Rajesh V. Patel");
        assertThat(updated.getBusinessName())
                .isEqualTo("Patel & Sons Agro Tech");
        assertThat(updated.getEmail())
                .isEqualTo("rajesh.new@example.com");
    }

    // =========================================================
    // PASSWORD
    // =========================================================

    @Test
    @DisplayName("Should change password successfully when current password matches")
    void shouldChangePassword() {

        PartnerChangePasswordRequest request =
                PartnerChangePasswordRequest.builder()
                        .currentPassword("old_secret")
                        .newPassword("new_strong_secret_123")
                        .build();

        when(partnerRepository.findById(1L))
                .thenReturn(Optional.of(testPartner));

        when(passwordEncoder.matches(
                "old_secret",
                "encoded_secret_pass"))
                .thenReturn(true);

        when(passwordEncoder.matches(
                "new_strong_secret_123",
                "encoded_secret_pass"))
                .thenReturn(false);

        when(passwordEncoder.encode("new_strong_secret_123"))
                .thenReturn("new_encoded_hash");

        partnerService.changePartnerPassword(1L, request);

        verify(partnerRepository).save(testPartner);

        assertThat(testPartner.getPassword())
                .isEqualTo("new_encoded_hash");
    }

    @Test
    @DisplayName("Should throw BadRequestException when current password is wrong")
    void shouldRejectPasswordChangeWhenCurrentPasswordWrong() {

        PartnerChangePasswordRequest request =
                PartnerChangePasswordRequest.builder()
                        .currentPassword("wrong_password")
                        .newPassword("new_strong_secret_123")
                        .build();

        when(partnerRepository.findById(1L))
                .thenReturn(Optional.of(testPartner));

        when(passwordEncoder.matches(
                "wrong_password",
                "encoded_secret_pass"))
                .thenReturn(false);

        assertThatThrownBy(() ->
                partnerService.changePartnerPassword(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining(
                        "Current password provided is incorrect"
                );
    }

    // =========================================================
    // OTP - SEND
    // =========================================================

    @Test
    @DisplayName("Should send OTP successfully")
    void shouldSendOtp() {

        testPartner.setOtpVerified(false);

        when(partnerRepository.findById(1L))
                .thenReturn(Optional.of(testPartner));

        when(partnerRepository.save(any(Partner.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String otp = partnerService.sendOtp(1L);

        assertThat(otp).isNotNull();
        assertThat(otp).matches("\\d{6}");

        assertThat(testPartner.getOtpCode())
                .isEqualTo(otp);

        assertThat(testPartner.getOtpExpiry())
                .isNotNull();

        verify(partnerRepository).save(testPartner);
    }

    // =========================================================
    // OTP - VERIFY
    // =========================================================

    @Test
    @DisplayName("Should verify OTP successfully")
    void shouldVerifyOtp() {

        testPartner.setOtpVerified(false);
        testPartner.setOtpCode("123456");
        testPartner.setOtpExpiry(
                LocalDateTime.now().plusMinutes(5)
        );

        when(partnerRepository.findById(1L))
                .thenReturn(Optional.of(testPartner));

        when(partnerRepository.save(any(Partner.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PartnerProfileResponse response =
                partnerService.verifyOtp(1L, "123456");

        assertThat(response).isNotNull();

        assertThat(response.isOtpVerified())
                .isTrue();

        assertThat(testPartner.isOtpVerified())
                .isTrue();

        assertThat(testPartner.getOtpCode())
                .isNull();

        assertThat(testPartner.getOtpExpiry())
                .isNull();

        verify(partnerRepository).save(testPartner);
    }

    // =========================================================
    // OTP - INVALID
    // =========================================================

    @Test
    @DisplayName("Should reject invalid OTP")
    void shouldRejectInvalidOtp() {

        testPartner.setOtpVerified(false);
        testPartner.setOtpCode("123456");
        testPartner.setOtpExpiry(
                LocalDateTime.now().plusMinutes(5)
        );

        when(partnerRepository.findById(1L))
                .thenReturn(Optional.of(testPartner));

        assertThatThrownBy(() ->
                partnerService.verifyOtp(1L, "999999"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid OTP");
    }

    // =========================================================
    // OTP - EXPIRED
    // =========================================================

    @Test
    @DisplayName("Should reject expired OTP")
    void shouldRejectExpiredOtp() {

        testPartner.setOtpVerified(false);
        testPartner.setOtpCode("123456");
        testPartner.setOtpExpiry(
                LocalDateTime.now().minusMinutes(1)
        );

        when(partnerRepository.findById(1L))
                .thenReturn(Optional.of(testPartner));

        assertThatThrownBy(() ->
                partnerService.verifyOtp(1L, "123456"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("OTP has expired");
    }

    // =========================================================
    // OTP - RESEND
    // =========================================================

    @Test
    @DisplayName("Should resend OTP successfully")
    void shouldResendOtp() {

        testPartner.setOtpVerified(false);

        when(partnerRepository.findById(1L))
                .thenReturn(Optional.of(testPartner));

        when(partnerRepository.save(any(Partner.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String otp = partnerService.resendOtp(1L);

        assertThat(otp).isNotNull();
        assertThat(otp).matches("\\d{6}");

        assertThat(testPartner.getOtpCode())
                .isEqualTo(otp);

        assertThat(testPartner.getOtpExpiry())
                .isNotNull();

        verify(partnerRepository).save(testPartner);
    }

    // =========================================================
    // DASHBOARD
    // =========================================================

    @Test
    @DisplayName("Should return partner dashboard")
    void shouldGetPartnerDashboard() {

        when(partnerRepository.findById(1L))
                .thenReturn(Optional.of(testPartner));

        Optional<PartnerDashboardResponse> result =
                partnerService.getPartnerDashboard(1L);

        assertThat(result).isPresent();

        PartnerDashboardResponse dashboard =
                result.get();

        assertThat(dashboard.id())
                .isEqualTo(1L);

        assertThat(dashboard.fullName())
                .isEqualTo("Rajesh Patel");

        assertThat(dashboard.businessName())
                .isEqualTo("Patel Agro Fleet");

        assertThat(dashboard.mobileNumber())
                .isEqualTo("9876543210");

        assertThat(dashboard.email())
                .isEqualTo("rajesh.patel@example.com");

        assertThat(dashboard.otpVerified())
                .isTrue();

        assertThat(dashboard.verificationStatus())
                .isEqualTo(Partner.VerificationStatus.APPROVED);
    }

    // =========================================================
    // KYC APPROVE
    // =========================================================

    @Test
    @DisplayName("Should approve partner KYC")
    void shouldApprovePartnerKyc() {

        testPartner.setVerificationStatus(
                Partner.VerificationStatus.PENDING
        );

        testPartner.setActive(false);

        when(partnerRepository.findById(1L))
                .thenReturn(Optional.of(testPartner));

        when(partnerRepository.save(any(Partner.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PartnerProfileResponse response =
                partnerService.approvePartnerKyc(1L);

        assertThat(response).isNotNull();

        assertThat(response.getVerificationStatus())
                .isEqualTo(Partner.VerificationStatus.APPROVED);

        assertThat(response.isActive())
                .isTrue();

        verify(partnerRepository).save(testPartner);
    }

    // =========================================================
    // KYC REJECT
    // =========================================================

    @Test
    @DisplayName("Should reject partner KYC")
    void shouldRejectPartnerKyc() {

        testPartner.setVerificationStatus(
                Partner.VerificationStatus.PENDING
        );

        testPartner.setActive(true);

        when(partnerRepository.findById(1L))
                .thenReturn(Optional.of(testPartner));

        when(partnerRepository.save(any(Partner.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PartnerProfileResponse response =
                partnerService.rejectPartnerKyc(1L);

        assertThat(response).isNotNull();

        assertThat(response.getVerificationStatus())
                .isEqualTo(Partner.VerificationStatus.REJECTED);

        assertThat(response.isActive())
                .isFalse();

        verify(partnerRepository).save(testPartner);
    }
    
}

