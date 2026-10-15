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
        request.setPassword("plain_secret_pass1");

        when(partnerRepository.existsByMobileNumber("9876543210"))
                .thenReturn(false);

        when(partnerRepository.existsByEmail("rajesh.patel@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("plain_secret_pass1"))
                .thenReturn("encoded_secret_pass");

        when(partnerRepository.save(any(Partner.class)))
                .thenReturn(testPartner);

        Partner registered = partnerService.registerPartner(request);

        assertThat(registered).isNotNull();
        assertThat(registered.getId()).isEqualTo(1L);
        assertThat(registered.getFullName()).isEqualTo("Rajesh Patel");

        org.mockito.ArgumentCaptor<Partner> partnerCaptor = org.mockito.ArgumentCaptor.forClass(Partner.class);
        verify(partnerRepository).save(partnerCaptor.capture());
        Partner capturedPartner = partnerCaptor.getValue();
        assertThat(capturedPartner.getVerificationStatus()).isEqualTo(Partner.VerificationStatus.PENDING);
        assertThat(capturedPartner.isOtpVerified()).isFalse();
        assertThat(capturedPartner.getFullName()).isEqualTo("Rajesh Patel");
        assertThat(capturedPartner.getMobileNumber()).isEqualTo("9876543210");
        assertThat(capturedPartner.getEmail()).isEqualTo("rajesh.patel@example.com");
        assertThat(capturedPartner.getPassword()).isEqualTo("encoded_secret_pass");
    }

    @Test
    @DisplayName("Should reject registration if mobile number is already registered")
    void shouldThrowExceptionWhenDuplicateMobile() {

        PartnerRegistrationRequest request = new PartnerRegistrationRequest();

        request.setFullName("Rajesh Patel");
        request.setMobileNumber("9876543210");
        request.setPassword("secret123");

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
    // OTP (RETIRED) AND PASSWORD POLICY
    // =========================================================

    @Test
    @DisplayName("Partner OTP is retired: PartnerService no longer generates, stores or verifies OTP codes")
    void partnerOtpIsRetired() {
        assertThat(java.util.Arrays.stream(PartnerService.class.getDeclaredMethods())
                .map(java.lang.reflect.Method::getName)
                .filter(name -> name.toLowerCase().contains("otp")))
                .isEmpty();
        assertThat(java.util.Arrays.stream(Partner.class.getDeclaredFields())
                .map(java.lang.reflect.Field::getName))
                .doesNotContain("otpCode", "otpExpiry")
                .contains("otpVerified");
    }

    @Test
    @DisplayName("Registration enforces the shared password policy before touching the database")
    void registrationRejectsWeakPassword() {
        for (String weak : new String[]{"short1", "onlyletters", "1234567890", "        ", "a1".repeat(33)}) {
            PartnerRegistrationRequest request = new PartnerRegistrationRequest();
            request.setFullName("Weak Password");
            request.setMobileNumber("9876500000");
            request.setPassword(weak);

            assertThatThrownBy(() -> partnerService.registerPartner(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage(com.agrorental.auth.service.PasswordPolicy.REQUIREMENTS_MESSAGE);
        }
        org.mockito.Mockito.verifyNoInteractions(partnerRepository, passwordEncoder);
    }

    @Test
    @DisplayName("Password change enforces the shared password policy")
    void passwordChangeRejectsWeakPassword() {
        when(partnerRepository.findById(1L)).thenReturn(Optional.of(testPartner));
        when(passwordEncoder.matches("current_pass_1", "encoded_secret_pass")).thenReturn(true);
        when(passwordEncoder.matches("allletters", "encoded_secret_pass")).thenReturn(false);

        assertThatThrownBy(() -> partnerService.changePartnerPassword(1L,
                PartnerChangePasswordRequest.builder().currentPassword("current_pass_1").newPassword("allletters").build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(com.agrorental.auth.service.PasswordPolicy.REQUIREMENTS_MESSAGE);
        verify(partnerRepository, org.mockito.Mockito.never()).save(any(Partner.class));
    }

    // =========================================================
    // DASHBOARD
    // =========================================================

    @Test
    @DisplayName("Dashboard counts AVAILABLE and BOOKED (not disabled) machines as active, and only this partner's approved active operators")
    void dashboardCountsMachinesInServiceAndOwnOperators() {
        com.agrorental.equipment.repository.EquipmentRepository equipmentRepository =
                org.mockito.Mockito.mock(com.agrorental.equipment.repository.EquipmentRepository.class);
        com.agrorental.operator.repository.OperatorRepository operatorRepository =
                org.mockito.Mockito.mock(com.agrorental.operator.repository.OperatorRepository.class);
        PartnerService service = new PartnerService(partnerRepository, passwordEncoder, equipmentRepository,
                null, null, null, operatorRepository);

        java.util.List<com.agrorental.equipment.entity.Equipment> fleet = java.util.List.of(
                machine(com.agrorental.equipment.enums.AvailabilityStatus.AVAILABLE, false),
                machine(com.agrorental.equipment.enums.AvailabilityStatus.BOOKED, false),
                machine(com.agrorental.equipment.enums.AvailabilityStatus.BOOKED, false),
                machine(com.agrorental.equipment.enums.AvailabilityStatus.AVAILABLE, true),
                machine(com.agrorental.equipment.enums.AvailabilityStatus.UNDER_MAINTENANCE, false),
                machine(com.agrorental.equipment.enums.AvailabilityStatus.INACTIVE, false));
        when(partnerRepository.findById(1L)).thenReturn(Optional.of(testPartner));
        when(equipmentRepository.findByPartnerId(1L)).thenReturn(fleet);
        when(operatorRepository.findByPartnerId(1L)).thenReturn(java.util.List.of(
                operator(com.agrorental.operator.entity.OperatorStatus.APPROVED, true),
                operator(com.agrorental.operator.entity.OperatorStatus.APPROVED, true),
                operator(com.agrorental.operator.entity.OperatorStatus.APPROVED, false),
                operator(com.agrorental.operator.entity.OperatorStatus.PENDING, true),
                operator(com.agrorental.operator.entity.OperatorStatus.REJECTED, false)));

        PartnerDashboardResponse dashboard = service.getPartnerDashboard(1L).orElseThrow();

        assertThat(dashboard.getTotalMachines()).isEqualTo(6);
        assertThat(dashboard.getActiveMachines()).isEqualTo(3);
        assertThat(dashboard.getActiveOperators()).isEqualTo(2);
        org.mockito.Mockito.verify(operatorRepository, org.mockito.Mockito.never())
                .findByStatus(com.agrorental.operator.entity.OperatorStatus.APPROVED);
    }

    private static com.agrorental.equipment.entity.Equipment machine(
            com.agrorental.equipment.enums.AvailabilityStatus status, boolean disabled) {
        return com.agrorental.equipment.entity.Equipment.builder().name("Machine " + status)
                .availabilityStatus(status).isDisabled(disabled).build();
    }

    private static com.agrorental.operator.entity.Operator operator(
            com.agrorental.operator.entity.OperatorStatus status, boolean active) {
        com.agrorental.operator.entity.Operator operator = com.agrorental.operator.entity.Operator.builder()
                .fullName("Fleet Operator").status(status).build();
        operator.setActive(active);
        return operator;
    }

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

    // =========================================================
    // GET ALL PARTNERS
    // =========================================================

    @Test
    @DisplayName("Should fetch all registered partners")
    void shouldGetAllPartners() {
        when(partnerRepository.findAll())
                .thenReturn(java.util.List.of(testPartner));

        java.util.List<PartnerProfileResponse> list = partnerService.getAllPartners();

        assertThat(list).isNotEmpty();
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getId()).isEqualTo(1L);
        assertThat(list.get(0).getFullName()).isEqualTo("Rajesh Patel");

        verify(partnerRepository).findAll();
    }
}

