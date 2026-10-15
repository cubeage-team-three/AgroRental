package com.agrorental.partner;

import com.agrorental.admin.entity.Admin;
import com.agrorental.admin.repository.AdminRepository;
import com.agrorental.auth.dto.LoginRequest;
import com.agrorental.auth.dto.LoginResponse;
import com.agrorental.auth.service.AuthService;
import com.agrorental.common.enums.Role;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.partner.dto.PartnerProfileResponse;
import com.agrorental.partner.dto.PartnerRegistrationRequest;
import com.agrorental.partner.entity.Partner;
import com.agrorental.partner.repository.PartnerRepository;
import com.agrorental.partner.service.PartnerService;
import com.agrorental.security.jwt.JwtService;
import com.agrorental.user.entity.User;
import com.agrorental.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * End-to-end unit and lifecycle test verifying the complete Partner
 * registration, admin approval, and authentication lifecycle as specified
 * in AgroRent P0 requirements:
 * 1. Partner registration persists the correct initial status (PENDING).
 * 2. A pending Partner is rejected with the intended error (only after the password was verified).
 * 3. An authorized admin can approve a Partner through the existing workflow.
 * 4. An approved Partner can log in and receives the correct role and token claims.
 * 5. An approved Partner is not incorrectly rejected as pending.
 * 6. Invalid credentials remain rejected.
 * 7. Farmer login and existing Operator authentication still work.
 * 8. A Partner cannot authenticate as Farmer or Admin by changing the requested role.
 * 9. Existing Admin approval and authorization controls remain effective.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Partner Registration, Approval & Authentication Full Lifecycle Tests")
class PartnerRegistrationApprovalLifecycleIntegrationTest {

    @Mock
    private PartnerRepository partnerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FarmerRepository farmerRepository;

    @Mock
    private OperatorRepository operatorRepository;

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private PartnerService partnerService;
    private AuthService authService;

    private Partner registeredPendingPartner;
    private Partner approvedPartner;
    private Farmer activeFarmer;
    private Operator approvedOperator;
    private Admin platformAdmin;

    @BeforeEach
    void setUp() {
        partnerService = new PartnerService(partnerRepository, passwordEncoder, null, null, null, null, null);

        authService = new AuthService(
                userRepository,
                farmerRepository,
                partnerRepository,
                adminRepository,
                operatorRepository,
                passwordEncoder,
                jwtService
        );

        registeredPendingPartner = Partner.builder()
                .fullName("Vikram Shinde")
                .businessName("Shinde Agro Fleets")
                .mobileNumber("9823456789")
                .email("vikram@agrorent.in")
                .password("encoded_pass_123")
                .otpVerified(false)
                .verificationStatus(Partner.VerificationStatus.PENDING)
                .build();
        registeredPendingPartner.setId(101L);
        registeredPendingPartner.setActive(true);

        approvedPartner = Partner.builder()
                .fullName("Vikram Shinde")
                .businessName("Shinde Agro Fleets")
                .mobileNumber("9823456789")
                .email("vikram@agrorent.in")
                .password("encoded_pass_123")
                .otpVerified(true)
                .verificationStatus(Partner.VerificationStatus.APPROVED)
                .build();
        approvedPartner.setId(101L);
        approvedPartner.setActive(true);

        activeFarmer = Farmer.builder()
                .fullName("Ramesh Yadav")
                .mobileNumber("9876543211")
                .email("ramesh@agrorent.in")
                .password("encoded_farmer_pass")
                .accountStatus("ACTIVE")
                .build();
        activeFarmer.setId(201L);

        approvedOperator = Operator.builder()
                .fullName("Rohan Operator")
                .mobileNumber("9876543299")
                .email("rohan@agrorent.in")
                .password("encoded_operator_pass")
                .mobileVerified(true)
                .status(OperatorStatus.APPROVED)
                .build();
        approvedOperator.setId(301L);
        approvedOperator.setActive(true);

        platformAdmin = Admin.builder()
                .fullName("AgroRent Administrator")
                .email("agrorent@admin.in")
                .password("encoded_admin_pass")
                .build();
        platformAdmin.setId(1L);
        platformAdmin.setActive(true);
    }

    @Test
    @DisplayName("Lifecycle 1: Partner registration persists initial status as PENDING (not APPROVED or auto-approved)")
    void testPartnerRegistrationPersistsPendingStatus() {
        PartnerRegistrationRequest request = new PartnerRegistrationRequest();
        request.setFullName("Vikram Shinde");
        request.setBusinessName("Shinde Agro Fleets");
        request.setMobileNumber("9823456789");
        request.setEmail("vikram@agrorent.in");
        request.setPassword("SecretPass@123");

        when(partnerRepository.existsByMobileNumber("9823456789")).thenReturn(false);
        when(partnerRepository.existsByEmail("vikram@agrorent.in")).thenReturn(false);
        when(passwordEncoder.encode("SecretPass@123")).thenReturn("encoded_pass_123");
        when(partnerRepository.save(any(Partner.class))).thenReturn(registeredPendingPartner);

        Partner result = partnerService.registerPartner(request);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(101L);

        ArgumentCaptor<Partner> captor = ArgumentCaptor.forClass(Partner.class);
        verify(partnerRepository).save(captor.capture());
        Partner persisted = captor.getValue();

        // Must strictly be PENDING verification status by default
        assertThat(persisted.getVerificationStatus()).isEqualTo(Partner.VerificationStatus.PENDING);
        assertThat(persisted.isOtpVerified()).isFalse();
        assertThat(persisted.getFullName()).isEqualTo("Vikram Shinde");
        assertThat(persisted.getMobileNumber()).isEqualTo("9823456789");
    }

    @Test
    @DisplayName("Lifecycle 2: Pending Partner with the correct password is blocked with the intended approval message")
    void testPendingPartnerLoginRejectedWithPendingApprovalMessage() {
        LoginRequest loginRequest = LoginRequest.builder()
                .mobileOrEmail("9823456789")
                .password("SecretPass@123")
                .role("PARTNER")
                .loginType("PASSWORD")
                .build();

        when(partnerRepository.findByMobileNumberOrEmail("9823456789", "9823456789"))
                .thenReturn(Optional.of(registeredPendingPartner));
        when(passwordEncoder.matches("SecretPass@123", "encoded_pass_123")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Your account verification is currently pending admin approval. Please check back later.");
        org.mockito.Mockito.verify(jwtService, org.mockito.Mockito.never()).generateToken(any(), any(), any(), anyMap());
    }

    @Test
    @DisplayName("Lifecycle 2b: Pending Partner with a wrong password gets only the generic credential error (no status leak)")
    void testPendingPartnerWithWrongPasswordGetsGenericError() {
        LoginRequest loginRequest = LoginRequest.builder()
                .mobileOrEmail("9823456789")
                .password("WrongPassword@999")
                .role("PARTNER")
                .loginType("PASSWORD")
                .build();

        when(partnerRepository.findByMobileNumberOrEmail("9823456789", "9823456789"))
                .thenReturn(Optional.of(registeredPendingPartner));
        when(passwordEncoder.matches("WrongPassword@999", "encoded_pass_123")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(AuthService.INVALID_CREDENTIALS_MESSAGE);
    }

    @Test
    @DisplayName("Lifecycle 3: Authorized admin approves Partner KYC through existing workflow")
    void testAdminCanApprovePartnerKyc() {
        when(partnerRepository.findById(101L)).thenReturn(Optional.of(registeredPendingPartner));
        when(partnerRepository.save(any(Partner.class))).thenAnswer(inv -> inv.getArgument(0));

        PartnerProfileResponse response = partnerService.approvePartnerKyc(101L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(101L);
        assertThat(response.getVerificationStatus()).isEqualTo(Partner.VerificationStatus.APPROVED);
        assertThat(response.isActive()).isTrue();

        ArgumentCaptor<Partner> captor = ArgumentCaptor.forClass(Partner.class);
        verify(partnerRepository).save(captor.capture());
        Partner approved = captor.getValue();
        assertThat(approved.getVerificationStatus()).isEqualTo(Partner.VerificationStatus.APPROVED);
        assertThat(approved.isActive()).isTrue();
    }

    @Test
    @DisplayName("Lifecycle 4: Approved Partner logs in successfully and receives correct PARTNER role and token claims")
    void testApprovedPartnerLoginSucceeds() {
        LoginRequest loginRequest = LoginRequest.builder()
                .mobileOrEmail("9823456789")
                .password("SecretPass@123")
                .role("PARTNER")
                .loginType("PASSWORD")
                .build();

        when(partnerRepository.findByMobileNumberOrEmail("9823456789", "9823456789"))
                .thenReturn(Optional.of(approvedPartner));
        when(passwordEncoder.matches("SecretPass@123", "encoded_pass_123")).thenReturn(true);
        when(jwtService.generateToken(eq(101L), eq("PARTNER"), eq("9823456789"), anyMap()))
                .thenReturn("valid.jwt.token.partner");

        LoginResponse response = authService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("valid.jwt.token.partner");
        assertThat(response.getRole()).isEqualTo("PARTNER");
        assertThat(response.getPartnerId()).isEqualTo(101L);
        assertThat(response.getAccountStatus()).isEqualTo("APPROVED");
        assertThat(response.getMessage()).contains("Partner login successful");
    }

    @Test
    @DisplayName("Lifecycle 5: Approved Partner is not incorrectly rejected as pending when logging in by email")
    void testApprovedPartnerNotRejectedAsPendingWhenUsingEmail() {
        LoginRequest loginRequest = LoginRequest.builder()
                .mobileOrEmail("vikram@agrorent.in")
                .password("SecretPass@123")
                .role("PARTNER")
                .loginType("PASSWORD")
                .build();

        when(partnerRepository.findByMobileNumberOrEmail("vikram@agrorent.in", "vikram@agrorent.in"))
                .thenReturn(Optional.empty());
        when(partnerRepository.findByMobileNumberOrEmailIgnoreCase("vikram@agrorent.in", "vikram@agrorent.in"))
                .thenReturn(Optional.of(approvedPartner));
        when(passwordEncoder.matches("SecretPass@123", "encoded_pass_123")).thenReturn(true);
        when(jwtService.generateToken(eq(101L), eq("PARTNER"), eq("9823456789"), anyMap()))
                .thenReturn("valid.jwt.token.partner");

        LoginResponse response = authService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getRole()).isEqualTo("PARTNER");
        assertThat(response.getPartnerId()).isEqualTo(101L);
    }

    @Test
    @DisplayName("Lifecycle 6: Invalid credentials for approved Partner remain rejected")
    void testApprovedPartnerWithInvalidPasswordRejected() {
        LoginRequest loginRequest = LoginRequest.builder()
                .mobileOrEmail("9823456789")
                .password("WrongPassword@999")
                .role("PARTNER")
                .loginType("PASSWORD")
                .build();

        when(partnerRepository.findByMobileNumberOrEmail("9823456789", "9823456789"))
                .thenReturn(Optional.of(approvedPartner));
        when(passwordEncoder.matches("WrongPassword@999", "encoded_pass_123")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(AuthService.INVALID_CREDENTIALS_MESSAGE);
    }

    @Test
    @DisplayName("Lifecycle 7: Farmer login and Operator authentication still work as expected")
    void testFarmerAndOperatorLoginStillWork() {
        // Farmer login
        LoginRequest farmerLogin = LoginRequest.builder()
                .mobileOrEmail("9876543211")
                .password("FarmerSecret")
                .role("FARMER")
                .loginType("PASSWORD")
                .build();

        when(farmerRepository.findByMobileNumberOrEmail("9876543211", "9876543211"))
                .thenReturn(Optional.of(activeFarmer));
        when(passwordEncoder.matches("FarmerSecret", "encoded_farmer_pass")).thenReturn(true);
        when(jwtService.generateToken(eq(201L), eq("FARMER"), eq("9876543211"), anyMap()))
                .thenReturn("farmer.jwt.token");

        LoginResponse farmerRes = authService.login(farmerLogin);
        assertThat(farmerRes.getRole()).isEqualTo("FARMER");
        assertThat(farmerRes.getFarmerId()).isEqualTo(201L);

        // Operator login
        LoginRequest operatorLogin = LoginRequest.builder()
                .mobileOrEmail("9876543299")
                .password("OperatorSecret")
                .role("OPERATOR")
                .loginType("PASSWORD")
                .build();

        when(operatorRepository.findByMobileNumberOrEmailIgnoreCase("9876543299", "9876543299"))
                .thenReturn(Optional.of(approvedOperator));
        when(passwordEncoder.matches("OperatorSecret", "encoded_operator_pass")).thenReturn(true);
        when(jwtService.generateOperatorToken(301L, "9876543299"))
                .thenReturn("operator.jwt.token");

        LoginResponse operatorRes = authService.login(operatorLogin);
        assertThat(operatorRes.getRole()).isEqualTo("OPERATOR");
        assertThat(operatorRes.getOperatorId()).isEqualTo(301L);
    }

    @Test
    @DisplayName("Lifecycle 8: Partner cannot authenticate as Farmer or Admin by tampering with the requested role")
    void testPartnerCannotAuthenticateAsFarmerOrAdmin() {
        // 1. Partner credentials submitted with FARMER role
        LoginRequest partnerAsFarmer = LoginRequest.builder()
                .mobileOrEmail("vikram@agrorent.in")
                .password("SecretPass@123")
                .role("FARMER")
                .loginType("PASSWORD")
                .build();

        when(userRepository.findByEmailAndRole("vikram@agrorent.in", Role.FARMER)).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCaseAndRole("vikram@agrorent.in", Role.FARMER)).thenReturn(Optional.empty());
        when(farmerRepository.findByMobileNumberOrEmail("vikram@agrorent.in", "vikram@agrorent.in")).thenReturn(Optional.empty());
        when(farmerRepository.findByMobileNumberOrEmailIgnoreCase("vikram@agrorent.in", "vikram@agrorent.in")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(partnerAsFarmer))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(AuthService.INVALID_CREDENTIALS_MESSAGE);

        // 2. Partner credentials submitted with ADMIN role
        LoginRequest partnerAsAdmin = LoginRequest.builder()
                .mobileOrEmail("vikram@agrorent.in")
                .password("SecretPass@123")
                .role("ADMIN")
                .loginType("PASSWORD")
                .build();

        when(adminRepository.findByEmail("vikram@agrorent.in")).thenReturn(Optional.empty());
        when(adminRepository.findByEmailIgnoreCase("vikram@agrorent.in")).thenReturn(Optional.empty());
        when(userRepository.findByEmailAndRole("vikram@agrorent.in", Role.ADMIN)).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCaseAndRole("vikram@agrorent.in", Role.ADMIN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(partnerAsAdmin))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(AuthService.INVALID_CREDENTIALS_MESSAGE);
    }

    @Test
    @DisplayName("Lifecycle 9: Admin login and approval controls remain effective")
    void testAdminLoginRemainsEffective() {
        LoginRequest adminLogin = LoginRequest.builder()
                .mobileOrEmail("agrorent@admin.in")
                .password("AdminSecret")
                .role("ADMIN")
                .loginType("PASSWORD")
                .build();

        when(adminRepository.findByEmail("agrorent@admin.in")).thenReturn(Optional.of(platformAdmin));
        when(passwordEncoder.matches("AdminSecret", "encoded_admin_pass")).thenReturn(true);
        when(jwtService.generateToken(eq(1L), eq("ADMIN"), eq("agrorent@admin.in"), anyMap()))
                .thenReturn("admin.jwt.token");

        LoginResponse adminRes = authService.login(adminLogin);
        assertThat(adminRes.getRole()).isEqualTo("ADMIN");
        assertThat(adminRes.getEmail()).isEqualTo("agrorent@admin.in");
        assertThat(adminRes.getToken()).isEqualTo("admin.jwt.token");
    }
}
