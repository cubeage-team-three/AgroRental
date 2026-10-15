package com.agrorental.auth;

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
import com.agrorental.partner.entity.Partner;
import com.agrorental.partner.repository.PartnerRepository;
import com.agrorental.user.entity.User;
import com.agrorental.user.repository.UserRepository;
import com.agrorental.security.jwt.JwtService;
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

import static org.mockito.ArgumentMatchers.anyLong;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Partner Login & Verification Tests")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FarmerRepository farmerRepository;

    @Mock
    private PartnerRepository partnerRepository;

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private OperatorRepository operatorRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private Partner approvedPartner;
    private Partner pendingPartner;

    @BeforeEach
    void setUp() {
        approvedPartner = Partner.builder()
                .fullName("Ramesh Patel")
                .businessName("Ramesh Agro Rentals")
                .mobileNumber("9876543210")
                .email("ramesh@agro.com")
                .password("encoded_password")
                .verificationStatus(Partner.VerificationStatus.APPROVED)
                .build();
        approvedPartner.setId(10L);
        approvedPartner.setActive(true);

        pendingPartner = Partner.builder()
                .fullName("Test Partner")
                .mobileNumber("9876543299")
                .email("testpartner@gmail.com")
                .password("encoded_password")
                .verificationStatus(Partner.VerificationStatus.PENDING)
                .build();
        pendingPartner.setId(11L);
        pendingPartner.setActive(true);
    }

    @Test
    @DisplayName("Should successfully log in approved partner by email")
    void shouldLoginApprovedPartnerByEmail() {
        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("ramesh@agro.com")
                .password("plain_password")
                .build();

        when(adminRepository.findByEmail("ramesh@agro.com")).thenReturn(Optional.empty());
        when(farmerRepository.findByMobileNumberOrEmail("ramesh@agro.com", "ramesh@agro.com")).thenReturn(Optional.empty());
        when(partnerRepository.findByMobileNumberOrEmailIgnoreCase("ramesh@agro.com", "ramesh@agro.com"))
                .thenReturn(Optional.of(approvedPartner));
        when(passwordEncoder.matches("plain_password", "encoded_password")).thenReturn(true);

        when(jwtService.generateToken(anyLong(), anyString(), anyString(), any()))
                .thenReturn("mock.jwt.token");


        LoginResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getPartnerId()).isEqualTo(10L);
        assertThat(response.getRole()).isEqualTo("PARTNER");
        assertThat(response.getEmail()).isEqualTo("ramesh@agro.com");
        assertThat(response.getToken()).isNotBlank();
    }

    @Test
    @DisplayName("Should successfully log in approved partner with uppercase email")
    void shouldLoginApprovedPartnerByUppercaseEmail() {
        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("RAMESH@AGRO.COM")
                .password("plain_password")
                .build();

        when(adminRepository.findByEmail("RAMESH@AGRO.COM")).thenReturn(Optional.empty());
        when(farmerRepository.findByMobileNumberOrEmail("RAMESH@AGRO.COM", "RAMESH@AGRO.COM")).thenReturn(Optional.empty());
        when(partnerRepository.findByMobileNumberOrEmailIgnoreCase("RAMESH@AGRO.COM", "RAMESH@AGRO.COM"))
                .thenReturn(Optional.of(approvedPartner));
        when(passwordEncoder.matches("plain_password", "encoded_password")).thenReturn(true);

        LoginResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getPartnerId()).isEqualTo(10L);
        assertThat(response.getRole()).isEqualTo("PARTNER");
    }

    @Test
    @DisplayName("Should successfully log in approved partner by mobile number")
    void shouldLoginApprovedPartnerByMobileNumber() {
        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("9876543210")
                .password("plain_password")
                .build();

        when(adminRepository.findByEmail("9876543210")).thenReturn(Optional.empty());
        when(farmerRepository.findByMobileNumberOrEmail("9876543210", "9876543210")).thenReturn(Optional.empty());
        when(partnerRepository.findByMobileNumberOrEmailIgnoreCase("9876543210", "9876543210"))
                .thenReturn(Optional.of(approvedPartner));
        when(passwordEncoder.matches("plain_password", "encoded_password")).thenReturn(true);

        LoginResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getPartnerId()).isEqualTo(10L);
        assertThat(response.getRole()).isEqualTo("PARTNER");
    }

    @Test
    @DisplayName("Should block login and return verification pending message for pending partner")
    void shouldBlockLoginForPendingPartner() {
        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("testpartner@gmail.com")
                .password("plain_password")
                .build();

        when(adminRepository.findByEmail("testpartner@gmail.com")).thenReturn(Optional.empty());
        when(farmerRepository.findByMobileNumberOrEmail("testpartner@gmail.com", "testpartner@gmail.com")).thenReturn(Optional.empty());
        when(partnerRepository.findByMobileNumberOrEmailIgnoreCase("testpartner@gmail.com", "testpartner@gmail.com"))
                .thenReturn(Optional.of(pendingPartner));
        when(passwordEncoder.matches("plain_password", "encoded_password")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Your account verification is currently pending admin approval");
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Gate order: a pending partner with a WRONG password only gets the generic credential error")
    void shouldNotRevealPendingStatusBeforePasswordCheck() {
        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("testpartner@gmail.com")
                .password("wrong_password")
                .build();

        when(adminRepository.findByEmail("testpartner@gmail.com")).thenReturn(Optional.empty());
        when(farmerRepository.findByMobileNumberOrEmail("testpartner@gmail.com", "testpartner@gmail.com")).thenReturn(Optional.empty());
        when(partnerRepository.findByMobileNumberOrEmailIgnoreCase("testpartner@gmail.com", "testpartner@gmail.com"))
                .thenReturn(Optional.of(pendingPartner));
        when(passwordEncoder.matches("wrong_password", "encoded_password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(AuthService.INVALID_CREDENTIALS_MESSAGE);
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Should block login for deactivated partner")
    void shouldBlockLoginForDeactivatedPartner() {
        approvedPartner.setActive(false);

        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("ramesh@agro.com")
                .password("plain_password")
                .build();

        when(adminRepository.findByEmail("ramesh@agro.com")).thenReturn(Optional.empty());
        when(farmerRepository.findByMobileNumberOrEmail("ramesh@agro.com", "ramesh@agro.com")).thenReturn(Optional.empty());
        when(partnerRepository.findByMobileNumberOrEmailIgnoreCase("ramesh@agro.com", "ramesh@agro.com"))
                .thenReturn(Optional.of(approvedPartner));
        when(passwordEncoder.matches("plain_password", "encoded_password")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Partner account is currently deactivated");
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Should reject login with invalid password")
    void shouldRejectInvalidPassword() {
        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("ramesh@agro.com")
                .password("wrong_password")
                .build();

        when(adminRepository.findByEmail("ramesh@agro.com")).thenReturn(Optional.empty());
        when(farmerRepository.findByMobileNumberOrEmail("ramesh@agro.com", "ramesh@agro.com")).thenReturn(Optional.empty());
        when(partnerRepository.findByMobileNumberOrEmailIgnoreCase("ramesh@agro.com", "ramesh@agro.com"))
                .thenReturn(Optional.of(approvedPartner));
        when(passwordEncoder.matches("wrong_password", "encoded_password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(AuthService.INVALID_CREDENTIALS_MESSAGE);
    }

    @Test
    @DisplayName("Should reject login when account does not exist")
    void shouldRejectNonExistentAccount() {
        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("unknown@agro.com")
                .password("any_password")
                .build();

        when(adminRepository.findByEmail("unknown@agro.com")).thenReturn(Optional.empty());
        when(farmerRepository.findByMobileNumberOrEmail("unknown@agro.com", "unknown@agro.com")).thenReturn(Optional.empty());
        when(partnerRepository.findByMobileNumberOrEmailIgnoreCase("unknown@agro.com", "unknown@agro.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(AuthService.INVALID_CREDENTIALS_MESSAGE);
    }

    @Test
    @DisplayName("Explicit Role: Should successfully log in farmer when role FARMER is selected")
    void shouldLoginFarmerWithExplicitFarmerRole() {
        Farmer farmer = Farmer.builder()
                .fullName("Suresh Kisan")
                .mobileNumber("9876543222")
                .email("suresh@agro.com")
                .password("encoded_pwd")
                .accountStatus("ACTIVE")
                .build();
        farmer.setId(100L);
        farmer.setActive(true);

        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("suresh@agro.com")
                .password("plain_pwd")
                .role("FARMER")
                .loginType("PASSWORD")
                .build();

        when(userRepository.findByEmailAndRole("suresh@agro.com", Role.FARMER)).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCaseAndRole("suresh@agro.com", Role.FARMER)).thenReturn(Optional.empty());
        when(farmerRepository.findByMobileNumberOrEmail("suresh@agro.com", "suresh@agro.com")).thenReturn(Optional.of(farmer));
        when(passwordEncoder.matches("plain_pwd", "encoded_pwd")).thenReturn(true);
        when(jwtService.generateToken(anyLong(), anyString(), anyString(), any())).thenReturn("mock.jwt.token");

        LoginResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getRole()).isEqualTo("FARMER");
        assertThat(response.getFarmerId()).isEqualTo(100L);
        assertThat(response.getFullName()).isEqualTo("Suresh Kisan");
    }

    @Test
    @DisplayName("Explicit Role: Should successfully log in partner when role PARTNER is selected")
    void shouldLoginPartnerWithExplicitPartnerRole() {
        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("ramesh@agro.com")
                .password("plain_password")
                .role("PARTNER")
                .loginType("PASSWORD")
                .build();

        when(partnerRepository.findByMobileNumberOrEmailIgnoreCase("ramesh@agro.com", "ramesh@agro.com"))
                .thenReturn(Optional.of(approvedPartner));
        when(passwordEncoder.matches("plain_password", "encoded_password")).thenReturn(true);
        when(jwtService.generateToken(anyLong(), anyString(), anyString(), any())).thenReturn("mock.jwt.token");

        LoginResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getRole()).isEqualTo("PARTNER");
        assertThat(response.getPartnerId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Disambiguation: Same email in both Farmer and Partner accounts resolves correctly based on explicit role")
    void shouldDisambiguateWhenSameEmailExistsForFarmerAndPartner() {
        String sharedEmail = "dual.account@agro.com";

        Farmer farmer = Farmer.builder()
                .fullName("Dual Identity Farmer")
                .mobileNumber("9876543255")
                .email(sharedEmail)
                .password("shared_pwd")
                .accountStatus("ACTIVE")
                .build();
        farmer.setId(101L);
        farmer.setActive(true);

        Partner partner = Partner.builder()
                .fullName("Dual Identity Partner")
                .businessName("Dual Machinery Rentals")
                .mobileNumber("9876543255")
                .email(sharedEmail)
                .password("shared_pwd")
                .verificationStatus(Partner.VerificationStatus.APPROVED)
                .build();
        partner.setId(202L);
        partner.setActive(true);

        // 1. Log in explicitly choosing FARMER
        LoginRequest farmerRequest = LoginRequest.builder()
                .mobileOrEmail(sharedEmail)
                .password("plain_shared_pwd")
                .role("FARMER")
                .loginType("PASSWORD")
                .build();

        when(userRepository.findByEmailAndRole(sharedEmail, Role.FARMER)).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCaseAndRole(sharedEmail, Role.FARMER)).thenReturn(Optional.empty());
        when(farmerRepository.findByMobileNumberOrEmail(sharedEmail, sharedEmail)).thenReturn(Optional.of(farmer));
        when(passwordEncoder.matches("plain_shared_pwd", "shared_pwd")).thenReturn(true);
        when(jwtService.generateToken(anyLong(), anyString(), anyString(), any())).thenReturn("mock.farmer.jwt");

        LoginResponse farmerResponse = authService.login(farmerRequest);
        assertThat(farmerResponse.getRole()).isEqualTo("FARMER");
        assertThat(farmerResponse.getFarmerId()).isEqualTo(101L);

        // 2. Log in explicitly choosing PARTNER
        LoginRequest partnerRequest = LoginRequest.builder()
                .mobileOrEmail(sharedEmail)
                .password("plain_shared_pwd")
                .role("PARTNER")
                .loginType("PASSWORD")
                .build();

        when(partnerRepository.findByMobileNumberOrEmailIgnoreCase(sharedEmail, sharedEmail)).thenReturn(Optional.of(partner));
        when(jwtService.generateToken(anyLong(), anyString(), anyString(), any())).thenReturn("mock.partner.jwt");

        LoginResponse partnerResponse = authService.login(partnerRequest);
        assertThat(partnerResponse.getRole()).isEqualTo("PARTNER");
        assertThat(partnerResponse.getPartnerId()).isEqualTo(202L);
    }

    @Test
    @DisplayName("Explicit Role: Should successfully log in operator when role OPERATOR is selected")
    void shouldLoginOperatorWithExplicitOperatorRole() {
        Operator operator = Operator.builder()
                .fullName("Rohan Operator")
                .mobileNumber("9876543277")
                .email("rohan@agro.com")
                .password("encoded_pwd")
                .status(OperatorStatus.APPROVED)
                .mobileVerified(true)
                .build();
        operator.setId(50L);
        operator.setActive(true);

        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("rohan@agro.com")
                .password("plain_pwd")
                .role("OPERATOR")
                .loginType("PASSWORD")
                .build();

        when(operatorRepository.findByMobileNumberOrEmailIgnoreCase("rohan@agro.com", "rohan@agro.com"))
                .thenReturn(Optional.of(operator));
        when(passwordEncoder.matches("plain_pwd", "encoded_pwd")).thenReturn(true);
        when(jwtService.generateOperatorToken(50L, "9876543277")).thenReturn("mock.operator.jwt");

        LoginResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getRole()).isEqualTo("OPERATOR");
        assertThat(response.getOperatorId()).isEqualTo(50L);
        assertThat(response.getToken()).isEqualTo("mock.operator.jwt");
    }

    @Test
    @DisplayName("Explicit Role: Should successfully log in admin when role ADMIN is selected")
    void shouldLoginAdminWithExplicitAdminRole() {
        Admin admin = Admin.builder()
                .fullName("Platform Administrator")
                .email("admin@agro.com")
                .password("encoded_pwd")
                .build();
        admin.setId(1L);
        admin.setActive(true);

        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("admin@agro.com")
                .password("plain_pwd")
                .role("ADMIN")
                .loginType("PASSWORD")
                .build();

        when(adminRepository.findByEmail("admin@agro.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("plain_pwd", "encoded_pwd")).thenReturn(true);
        when(jwtService.generateToken(anyLong(), anyString(), anyString(), any())).thenReturn("mock.admin.jwt");

        LoginResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getRole()).isEqualTo("ADMIN");
        assertThat(response.getEmail()).isEqualTo("admin@agro.com");
    }

    @Test
    @DisplayName("Security: Invalid role string must be rejected with BadRequestException")
    void shouldRejectInvalidRoleValue() {
        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("any@agro.com")
                .password("any_password")
                .role("SUPERUSER")
                .build();

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid role specified. Allowed roles are: FARMER, PARTNER, OPERATOR, ADMIN.");
    }

    @Test
    @DisplayName("Security: Role tampering attempt to authenticate as ADMIN with farmer credentials fails safely")
    void shouldPreventPrivilegeEscalationRoleTampering() {
        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("farmer@agro.com")
                .password("farmer_pwd")
                .role("ADMIN")
                .loginType("PASSWORD")
                .build();

        when(adminRepository.findByEmail("farmer@agro.com")).thenReturn(Optional.empty());
        when(adminRepository.findByEmailIgnoreCase("farmer@agro.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailAndRole("farmer@agro.com", Role.ADMIN)).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCaseAndRole("farmer@agro.com", Role.ADMIN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(AuthService.INVALID_CREDENTIALS_MESSAGE);
    }

    @Test
    @DisplayName("Security: Submitting FARMER role cannot authenticate an ADMIN account")
    void shouldNotAuthenticateAdminWhenFarmerRoleSelected() {
        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("admin@agro.com")
                .password("admin_pwd")
                .role("FARMER")
                .loginType("PASSWORD")
                .build();

        when(userRepository.findByEmailAndRole("admin@agro.com", Role.FARMER)).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCaseAndRole("admin@agro.com", Role.FARMER)).thenReturn(Optional.empty());
        when(farmerRepository.findByMobileNumberOrEmail("admin@agro.com", "admin@agro.com")).thenReturn(Optional.empty());
        when(farmerRepository.findByMobileNumberOrEmailIgnoreCase("admin@agro.com", "admin@agro.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(AuthService.INVALID_CREDENTIALS_MESSAGE);
    }

    @Test
    @DisplayName("Security: Submitting PARTNER role cannot authenticate an ADMIN account")
    void shouldNotAuthenticateAdminWhenPartnerRoleSelected() {
        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("admin@agro.com")
                .password("admin_pwd")
                .role("PARTNER")
                .loginType("PASSWORD")
                .build();

        when(partnerRepository.findByMobileNumberOrEmailIgnoreCase("admin@agro.com", "admin@agro.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCaseAndRole("admin@agro.com", Role.PARTNER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(AuthService.INVALID_CREDENTIALS_MESSAGE);
    }

    @Test
    @DisplayName("Security: Submitting OPERATOR role cannot authenticate an ADMIN account")
    void shouldNotAuthenticateAdminWhenOperatorRoleSelected() {
        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("admin@agro.com")
                .password("admin_pwd")
                .role("OPERATOR")
                .loginType("PASSWORD")
                .build();

        when(operatorRepository.findByMobileNumberOrEmailIgnoreCase("admin@agro.com", "admin@agro.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(AuthService.INVALID_CREDENTIALS_MESSAGE);
    }

    @Test
    @DisplayName("Role Isolation: Submitting FARMER role cannot authenticate a PARTNER account")
    void shouldNotAuthenticatePartnerWhenFarmerRoleSelected() {
        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("partner@agro.com")
                .password("partner_pwd")
                .role("FARMER")
                .loginType("PASSWORD")
                .build();

        when(userRepository.findByEmailAndRole("partner@agro.com", Role.FARMER)).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCaseAndRole("partner@agro.com", Role.FARMER)).thenReturn(Optional.empty());
        when(farmerRepository.findByMobileNumberOrEmail("partner@agro.com", "partner@agro.com")).thenReturn(Optional.empty());
        when(farmerRepository.findByMobileNumberOrEmailIgnoreCase("partner@agro.com", "partner@agro.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(AuthService.INVALID_CREDENTIALS_MESSAGE);
    }

    @Test
    @DisplayName("Role Isolation: Submitting PARTNER role cannot authenticate a FARMER account")
    void shouldNotAuthenticateFarmerWhenPartnerRoleSelected() {
        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("farmer@agro.com")
                .password("farmer_pwd")
                .role("PARTNER")
                .loginType("PASSWORD")
                .build();

        when(partnerRepository.findByMobileNumberOrEmailIgnoreCase("farmer@agro.com", "farmer@agro.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCaseAndRole("farmer@agro.com", Role.PARTNER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(AuthService.INVALID_CREDENTIALS_MESSAGE);
    }

    @Test
    @DisplayName("Security: OTP login mode is rejected for every role (FARMER included) before any account lookup")
    void shouldRejectOtpLoginForEveryRole() {
        for (String role : new String[]{"FARMER", "PARTNER", "OPERATOR", "ADMIN", null}) {
            LoginRequest otpLogin = LoginRequest.builder()
                    .mobileOrEmail("9876543297")
                    .otp("123456")
                    .role(role)
                    .loginType("OTP")
                    .build();

            assertThatThrownBy(() -> authService.login(otpLogin))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage(AuthService.OTP_LOGIN_DISABLED_MESSAGE);
        }
        verifyNoInteractions(userRepository, farmerRepository, partnerRepository, adminRepository, operatorRepository,
                passwordEncoder, jwtService);
    }

    @Test
    @DisplayName("Operator Lifecycle: pending, rejected and inactive operators are blocked after the password check; unverified mobile is allowed")
    void shouldEnforceOperatorLifecycleConstraints() {
        when(passwordEncoder.matches("pwd", "enc_pwd")).thenReturn(true);

        Operator pendingOp = operator(51L, "pending@agro.com", OperatorStatus.PENDING, true, true);
        assertThatThrownBy(() -> authService.login(operatorLogin("pending@agro.com", "pwd")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Operator account is pending admin approval.");

        Operator rejectedOp = operator(53L, "rejected@agro.com", OperatorStatus.REJECTED, true, true);
        rejectedOp.setRejectionReason("Licence expired");
        assertThatThrownBy(() -> authService.login(operatorLogin("rejected@agro.com", "pwd")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Operator account has been rejected: Licence expired");

        Operator inactiveOp = operator(54L, "inactive@agro.com", OperatorStatus.APPROVED, false, true);
        assertThatThrownBy(() -> authService.login(operatorLogin("inactive@agro.com", "pwd")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Operator account is inactive");

        // Contract v1.1: mobile (OTP) verification is no longer a login requirement
        Operator unverifiedOp = operator(52L, "unverified@agro.com", OperatorStatus.APPROVED, true, false);
        when(jwtService.generateOperatorToken(52L, unverifiedOp.getMobileNumber())).thenReturn("op.jwt");
        LoginResponse response = authService.login(operatorLogin("unverified@agro.com", "pwd"));
        assertThat(response.getOperatorId()).isEqualTo(52L);
        assertThat(response.getToken()).isEqualTo("op.jwt");

        // Wrong password on a pending operator reveals nothing about its status
        when(passwordEncoder.matches("bad", "enc_pwd")).thenReturn(false);
        assertThatThrownBy(() -> authService.login(operatorLogin("pending@agro.com", "bad")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(AuthService.INVALID_CREDENTIALS_MESSAGE);
        verify(jwtService, never()).generateOperatorToken(51L, pendingOp.getMobileNumber());
        verify(jwtService, never()).generateOperatorToken(53L, rejectedOp.getMobileNumber());
        verify(jwtService, never()).generateOperatorToken(54L, inactiveOp.getMobileNumber());
    }

    private Operator operator(Long id, String email, OperatorStatus status, boolean active, boolean mobileVerified) {
        Operator operator = Operator.builder()
                .fullName("Op " + id)
                .mobileNumber("98765432" + (id % 100))
                .email(email)
                .password("enc_pwd")
                .status(status)
                .mobileVerified(mobileVerified)
                .build();
        operator.setId(id);
        operator.setActive(active);
        when(operatorRepository.findByMobileNumberOrEmailIgnoreCase(email, email)).thenReturn(Optional.of(operator));
        return operator;
    }

    private static LoginRequest operatorLogin(String identifier, String password) {
        return LoginRequest.builder().mobileOrEmail(identifier).password(password).role("OPERATOR").build();
    }

    @Test
    @DisplayName("Farmer Lifecycle: a legacy PENDING_OTP farmer can log in with its password (PENDING_OTP no longer blocks)")
    void shouldAllowPasswordLoginForFarmerPendingOtp() {
        Farmer pendingFarmer = Farmer.builder()
                .fullName("Pending Farmer")
                .mobileNumber("9876543299")
                .email("pendingfarmer@agro.com")
                .password("encoded_pwd")
                .accountStatus("PENDING_OTP")
                .build();
        pendingFarmer.setId(701L);
        pendingFarmer.setActive(true);

        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("9876543299")
                .password("plain_pwd")
                .role("FARMER")
                .loginType("PASSWORD")
                .build();

        when(farmerRepository.findByMobileNumber("9876543299")).thenReturn(Optional.of(pendingFarmer));
        when(passwordEncoder.matches("plain_pwd", "encoded_pwd")).thenReturn(true);
        when(jwtService.generateToken(anyLong(), anyString(), anyString(), any())).thenReturn("pending.otp.farmer.jwt");

        LoginResponse response = authService.login(request);
        assertThat(response.getFarmerId()).isEqualTo(701L);
        assertThat(response.getToken()).isEqualTo("pending.otp.farmer.jwt");
    }

    @Test
    @DisplayName("Farmer Lifecycle: a farmer without a password (former OTP-only account) fails exactly like a wrong password")
    void shouldRejectFarmerWithoutPasswordGenerically() {
        Farmer otpOnlyFarmer = Farmer.builder()
                .fullName("OTP Only Farmer")
                .mobileNumber("9876543288")
                .accountStatus("ACTIVE")
                .build();
        otpOnlyFarmer.setId(706L);
        otpOnlyFarmer.setActive(true);

        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("9876543288")
                .password("anything123")
                .role("FARMER")
                .loginType("PASSWORD")
                .build();
        when(farmerRepository.findByMobileNumber("9876543288")).thenReturn(Optional.of(otpOnlyFarmer));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(AuthService.INVALID_CREDENTIALS_MESSAGE);
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Farmer Lifecycle: Inactive farmer account must be blocked")
    void shouldBlockLoginForFarmerInactive() {
        Farmer inactiveFarmer = Farmer.builder()
                .fullName("Inactive Farmer")
                .mobileNumber("9876543298")
                .email("inactivefarmer@agro.com")
                .password("encoded_pwd")
                .accountStatus("INACTIVE")
                .build();
        inactiveFarmer.setId(702L);
        inactiveFarmer.setActive(true);

        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("9876543298")
                .password("plain_pwd")
                .role("FARMER")
                .loginType("PASSWORD")
                .build();

        when(farmerRepository.findByMobileNumber("9876543298")).thenReturn(Optional.of(inactiveFarmer));
        when(passwordEncoder.matches("plain_pwd", "encoded_pwd")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Account has been deactivated");
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Farmer OTP: OTP login no longer issues a session, even for an active farmer")
    void shouldRejectFarmerOtpLogin() {
        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("9876543297")
                .otp("123456")
                .role("FARMER")
                .loginType("OTP")
                .build();

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(AuthService.OTP_LOGIN_DISABLED_MESSAGE);
        verifyNoInteractions(farmerRepository, jwtService);
    }

    @Test
    @DisplayName("Farmer Login: Should normalize +91 country code in mobile number")
    void shouldNormalizeFarmerMobileWithCountryCode() {
        Farmer farmer = Farmer.builder()
                .fullName("Prefix Farmer")
                .mobileNumber("9876543296")
                .email("prefixfarmer@agro.com")
                .password("encoded_pwd")
                .accountStatus("ACTIVE")
                .build();
        farmer.setId(704L);
        farmer.setActive(true);

        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("+91 9876543296")
                .password("plain_pwd")
                .role("FARMER")
                .loginType("PASSWORD")
                .build();

        when(farmerRepository.findByMobileNumber("9876543296")).thenReturn(Optional.of(farmer));
        when(passwordEncoder.matches("plain_pwd", "encoded_pwd")).thenReturn(true);
        when(jwtService.generateToken(anyLong(), anyString(), anyString(), any())).thenReturn("mock.farmer.jwt");

        LoginResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getRole()).isEqualTo("FARMER");
        assertThat(response.getFarmerId()).isEqualTo(704L);
    }

    @Test
    @DisplayName("Partner Login: Should normalize +91 country code in mobile number")
    void shouldNormalizePartnerMobileWithCountryCode() {
        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("+91 9876543210")
                .password("plain_password")
                .role("PARTNER")
                .loginType("PASSWORD")
                .build();

        when(partnerRepository.findByMobileNumber("9876543210")).thenReturn(Optional.of(approvedPartner));
        when(passwordEncoder.matches("plain_password", "encoded_password")).thenReturn(true);
        when(jwtService.generateToken(anyLong(), anyString(), anyString(), any())).thenReturn("mock.partner.jwt");

        LoginResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getRole()).isEqualTo("PARTNER");
        assertThat(response.getPartnerId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Core User: Should authenticate Farmer user by email with linked profile data")
    void shouldAuthenticateFarmerUserByEmailWithLinkedProfile() {
        Farmer linkedFarmer = Farmer.builder()
                .fullName("Core Farmer")
                .mobileNumber("9876543295")
                .password("encoded_pwd")
                .preferredLanguage("Marathi")
                .accountStatus("ACTIVE")
                .build();
        linkedFarmer.setId(705L);

        User user = User.builder()
                .name("Core Farmer")
                .email("corefarmer@agro.com")
                .password("encoded_pwd")
                .role(Role.FARMER)
                .enabled(true)
                .verified(true)
                .build();
        user.setId(501L);
        user.setFarmer(linkedFarmer);

        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("corefarmer@agro.com")
                .password("plain_pwd")
                .role("FARMER")
                .loginType("PASSWORD")
                .build();

        when(userRepository.findByEmailAndRole("corefarmer@agro.com", Role.FARMER)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("plain_pwd", "encoded_pwd")).thenReturn(true);
        when(jwtService.generateToken(anyLong(), anyString(), anyString(), any())).thenReturn("mock.core.farmer.jwt");

        LoginResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getRole()).isEqualTo("FARMER");
        assertThat(response.getUserId()).isEqualTo(501L);
        assertThat(response.getFarmerId()).isEqualTo(705L);
        assertThat(response.getMobileNumber()).isEqualTo("9876543295");
        assertThat(response.getPreferredLanguage()).isEqualTo("Marathi");
    }

    @Test
    @DisplayName("Core User: the farmer profile's password is authoritative; a users-table password alone never authenticates a farmer")
    void shouldNotAuthenticateFarmerWithCoreUserPasswordOnly() {
        Farmer linkedFarmer = Farmer.builder()
                .fullName("Core Farmer")
                .mobileNumber("9876543294")
                .accountStatus("ACTIVE")
                .build();
        linkedFarmer.setId(707L);
        User user = User.builder()
                .name("Core Farmer")
                .email("corefarmer2@agro.com")
                .password("encoded_user_only_pwd")
                .role(Role.FARMER)
                .enabled(true)
                .build();
        user.setId(502L);
        user.setFarmer(linkedFarmer);

        when(userRepository.findByEmailAndRole("corefarmer2@agro.com", Role.FARMER)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(LoginRequest.builder()
                .mobileOrEmail("corefarmer2@agro.com").password("plain_pwd").role("FARMER").build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(AuthService.INVALID_CREDENTIALS_MESSAGE);
        verify(passwordEncoder, never()).matches("plain_pwd", "encoded_user_only_pwd");
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Admin Lifecycle: Deactivated admin account must be blocked")
    void shouldBlockDeactivatedAdmin() {
        Admin inactiveAdmin = Admin.builder()
                .fullName("Inactive Admin")
                .email("inactiveadmin@agro.com")
                .password("pwd")
                .build();
        inactiveAdmin.setId(9L);
        inactiveAdmin.setActive(false);

        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("inactiveadmin@agro.com")
                .password("pwd")
                .role("ADMIN")
                .loginType("PASSWORD")
                .build();

        when(adminRepository.findByEmail("inactiveadmin@agro.com")).thenReturn(Optional.of(inactiveAdmin));
        when(passwordEncoder.matches("pwd", "pwd")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Admin account is currently deactivated");
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("Admin Lifecycle: a deactivated admin with a wrong password gets only the generic error")
    void shouldNotRevealDeactivatedAdminBeforePasswordCheck() {
        Admin inactiveAdmin = Admin.builder()
                .fullName("Inactive Admin")
                .email("inactiveadmin@agro.com")
                .password("pwd")
                .build();
        inactiveAdmin.setId(9L);
        inactiveAdmin.setActive(false);
        when(adminRepository.findByEmail("inactiveadmin@agro.com")).thenReturn(Optional.of(inactiveAdmin));
        when(passwordEncoder.matches("nope", "pwd")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(LoginRequest.builder()
                .mobileOrEmail("inactiveadmin@agro.com").password("nope").role("ADMIN").loginType("PASSWORD").build()))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(AuthService.INVALID_CREDENTIALS_MESSAGE);
    }

    @Test
    @DisplayName("Non-enumeration: unknown account, wrong password and missing password produce the identical message")
    void unknownAndWrongPasswordAreIndistinguishable() {
        // lenient: the same lookups/checks are intentionally invoked with other arguments for the unknown account
        // (including a dummy BCrypt check that equalizes timing)
        org.mockito.Mockito.lenient().when(partnerRepository.findByMobileNumberOrEmailIgnoreCase("ramesh@agro.com", "ramesh@agro.com"))
                .thenReturn(Optional.of(approvedPartner));
        org.mockito.Mockito.lenient().when(passwordEncoder.matches("wrong_password", "encoded_password")).thenReturn(false);

        String unknown = catchMessage(LoginRequest.builder().mobileOrEmail("nobody@agro.com").password("x1234567").role("PARTNER").build());
        String wrong = catchMessage(LoginRequest.builder().mobileOrEmail("ramesh@agro.com").password("wrong_password").role("PARTNER").build());
        String missing = catchMessage(LoginRequest.builder().mobileOrEmail("ramesh@agro.com").role("PARTNER").build());

        assertThat(unknown).isEqualTo(wrong).isEqualTo(missing).isEqualTo(AuthService.INVALID_CREDENTIALS_MESSAGE);
    }

    private String catchMessage(LoginRequest request) {
        try {
            authService.login(request);
        } catch (BadRequestException e) {
            return e.getMessage();
        }
        throw new AssertionError("login unexpectedly succeeded");
    }
}
