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
import com.agrorental.farmer.service.FarmerOtpService;
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
    private FarmerOtpService farmerOtpService;

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

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Your account verification is currently pending admin approval");
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

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Partner account is currently deactivated");
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
                .hasMessageContaining("Invalid mobile/email or password.");
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
                .hasMessageContaining("Invalid email or password.");
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
                .hasMessageContaining("Invalid email or password.");
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
                .hasMessageContaining("Invalid email or password.");
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
        when(userRepository.findByEmailAndRole("admin@agro.com", Role.PARTNER)).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCaseAndRole("admin@agro.com", Role.PARTNER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid email or password.");
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
                .hasMessageContaining("Invalid email or password.");
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
                .hasMessageContaining("Invalid email or password.");
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
        when(userRepository.findByEmailAndRole("farmer@agro.com", Role.PARTNER)).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCaseAndRole("farmer@agro.com", Role.PARTNER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid email or password.");
    }

    @Test
    @DisplayName("Security: Non-Farmer roles must reject OTP login mode")
    void shouldRejectOtpLoginForNonFarmerRoles() {
        LoginRequest partnerOtp = LoginRequest.builder()
                .mobileOrEmail("partner@agro.com")
                .role("PARTNER")
                .loginType("OTP")
                .build();

        assertThatThrownBy(() -> authService.login(partnerOtp))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("OTP login is only supported for Farmer accounts");

        LoginRequest operatorOtp = LoginRequest.builder()
                .mobileOrEmail("operator@agro.com")
                .role("OPERATOR")
                .loginType("OTP")
                .build();

        assertThatThrownBy(() -> authService.login(operatorOtp))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("OTP login is only supported for Farmer accounts");

        LoginRequest adminOtp = LoginRequest.builder()
                .mobileOrEmail("admin@agro.com")
                .role("ADMIN")
                .loginType("OTP")
                .build();

        assertThatThrownBy(() -> authService.login(adminOtp))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("OTP login is only supported for Farmer accounts");
    }

    @Test
    @DisplayName("Operator Lifecycle: Pending, inactive, or unverified operator accounts must be blocked")
    void shouldEnforceOperatorLifecycleConstraints() {
        Operator pendingOp = Operator.builder()
                .fullName("Pending Op")
                .mobileNumber("9876543201")
                .email("pending@agro.com")
                .password("pwd")
                .status(OperatorStatus.PENDING)
                .mobileVerified(true)
                .build();
        pendingOp.setId(51L);
        pendingOp.setActive(true);

        LoginRequest req1 = LoginRequest.builder()
                .mobileOrEmail("pending@agro.com")
                .password("pwd")
                .role("OPERATOR")
                .build();

        when(operatorRepository.findByMobileNumberOrEmailIgnoreCase("pending@agro.com", "pending@agro.com"))
                .thenReturn(Optional.of(pendingOp));

        assertThatThrownBy(() -> authService.login(req1))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Operator account is pending admin approval.");

        Operator unverifiedOp = Operator.builder()
                .fullName("Unverified Op")
                .mobileNumber("9876543202")
                .email("unverified@agro.com")
                .password("pwd")
                .status(OperatorStatus.APPROVED)
                .mobileVerified(false)
                .build();
        unverifiedOp.setId(52L);
        unverifiedOp.setActive(true);

        LoginRequest req2 = LoginRequest.builder()
                .mobileOrEmail("unverified@agro.com")
                .password("pwd")
                .role("OPERATOR")
                .build();

        when(operatorRepository.findByMobileNumberOrEmailIgnoreCase("unverified@agro.com", "unverified@agro.com"))
                .thenReturn(Optional.of(unverifiedOp));

        assertThatThrownBy(() -> authService.login(req2))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Mobile number is not verified");
    }

    @Test
    @DisplayName("Farmer Lifecycle: Pending OTP farmer account must be blocked from password login")
    void shouldBlockLoginForFarmerPendingOtp() {
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

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("PENDING_OTP: Account is not activated yet");
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

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Account has been deactivated");
    }

    @Test
    @DisplayName("Farmer OTP: Should successfully log in farmer using OTP")
    void shouldLoginFarmerViaOtp() {
        Farmer farmer = Farmer.builder()
                .fullName("OTP Farmer")
                .mobileNumber("9876543297")
                .email("otpfarmer@agro.com")
                .accountStatus("ACTIVE")
                .build();
        farmer.setId(703L);
        farmer.setActive(true);

        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("9876543297")
                .otp("123456")
                .role("FARMER")
                .loginType("OTP")
                .build();

        when(farmerRepository.findByMobileNumber("9876543297")).thenReturn(Optional.of(farmer));
        when(jwtService.generateToken(anyLong(), anyString(), anyString(), any())).thenReturn("mock.farmer.otp.jwt");

        LoginResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getRole()).isEqualTo("FARMER");
        assertThat(response.getFarmerId()).isEqualTo(703L);
        assertThat(response.getToken()).isEqualTo("mock.farmer.otp.jwt");
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

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Admin account is currently deactivated");
    }
}
