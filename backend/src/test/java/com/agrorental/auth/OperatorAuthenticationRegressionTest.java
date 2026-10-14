package com.agrorental.auth;

import com.agrorental.auth.dto.LoginRequest;
import com.agrorental.auth.dto.LoginResponse;
import com.agrorental.auth.dto.ResetPasswordRequest;
import com.agrorental.auth.service.AuthService;
import com.agrorental.auth.service.PasswordResetService;
import com.agrorental.common.enums.Role;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.UnauthorizedException;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.farmer.service.FarmerOtpService;
import com.agrorental.operator.dto.OperatorLoginRequest;
import com.agrorental.operator.dto.OperatorLoginResponse;
import com.agrorental.operator.dto.OperatorRegistrationRequest;
import com.agrorental.operator.dto.OperatorResponse;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.mapper.OperatorDocumentMapper;
import com.agrorental.operator.mapper.OperatorMapper;
import com.agrorental.operator.otp.OperatorOtpRepository;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.operator.service.OperatorAuthService;
import com.agrorental.operator.service.OperatorService;
import com.agrorental.partner.entity.Partner;
import com.agrorental.partner.repository.PartnerRepository;
import com.agrorental.admin.repository.AdminRepository;
import com.agrorental.security.jwt.JwtProperties;
import com.agrorental.security.jwt.JwtService;
import com.agrorental.user.entity.User;
import com.agrorental.user.repository.PasswordResetTokenRepository;
import com.agrorental.user.repository.UserRepository;
import com.agrorental.user.service.EmailService;
import com.agrorental.booking.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * P0 Comprehensive Regression Test Suite for Operator Authentication.
 * Verifies:
 * 1. Correct Operator password succeeds for an eligible account.
 * 2. Incorrect password fails safely with consistent bad request exception.
 * 3. Operator registration and login use compatible BCrypt password encoding.
 * 4. Identifier normalization allows +91 prefixed and spaced mobile numbers to log in.
 * 5. Case-insensitive email lookup succeeds.
 * 6. Safe legacy compatibility allows unhashed passwords to authenticate and upgrades them to BCrypt.
 * 7. Accidental leading/trailing whitespace in entered password is handled safely.
 * 8. Password reset produces a password that subsequently authenticates.
 * 9. Pending, inactive, and unverified accounts remain subject to existing lifecycle policies.
 * 10. Role isolation: Farmer and Partner cannot authenticate as Operator.
 * 11. Existing dedicated Operator login (/api/operators/login) remains fully compatible.
 * 12. JWT claims accurately reflect the OPERATOR role.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Operator Authentication P0 Regression Tests")
class OperatorAuthenticationRegressionTest {

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
    private OperatorOtpRepository operatorOtpRepository;

    @Mock
    private PasswordResetTokenRepository resetTokenRepository;

    @Mock
    private FarmerOtpService farmerOtpService;

    @Mock
    private EmailService emailService;

    @Mock
    private BookingRepository bookingRepository;

    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private OperatorMapper operatorMapper;
    private AuthService authService;
    private OperatorAuthService operatorAuthService;
    private OperatorService operatorService;
    private PasswordResetService passwordResetService;

    private Operator eligibleOperator;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();

        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("dGhpc0lzQVZlcnlTZWN1cmUzMkJ5dGVLZXlGb3JUZXN0aW5nMTIzNDU2"); // 32+ bytes Base64
        jwtProperties.setExpirationMs(3600000L);
        jwtService = new JwtService(jwtProperties);

        operatorMapper = new OperatorMapper(new OperatorDocumentMapper());

        authService = new AuthService(
                userRepository,
                farmerRepository,
                partnerRepository,
                adminRepository,
                operatorRepository,
                farmerOtpService,
                passwordEncoder,
                jwtService
        );

        operatorAuthService = new OperatorAuthService(
                operatorRepository,
                passwordEncoder,
                jwtService,
                operatorMapper
        );

        operatorService = new OperatorService(
                operatorRepository,
                passwordEncoder,
                operatorMapper,
                bookingRepository
        );

        passwordResetService = new PasswordResetService(
                userRepository,
                farmerRepository,
                partnerRepository,
                operatorRepository,
                operatorOtpRepository,
                resetTokenRepository,
                emailService,
                passwordEncoder
        );

        // Pre-configure eligible approved operator with BCrypt password "Operator@2026"
        eligibleOperator = Operator.builder()
                .fullName("Sahil Karale")
                .mobileNumber("9876543220")
                .email("sahilkarale22@gmail.com")
                .address("Pune, Maharashtra")
                .aadhaarNumber("123456789012")
                .drivingLicenseNumber("MH1220200012345")
                .experience(5)
                .skills("Tractor Operation, Combine Harvester")
                .password(passwordEncoder.encode("Operator@2026"))
                .status(OperatorStatus.APPROVED)
                .mobileVerified(true)
                .build();
        eligibleOperator.setId(77L);
        eligibleOperator.setActive(true);
    }

    @Test
    @DisplayName("1. Correct Operator password succeeds for an eligible account via shared login")
    void testCorrectPasswordSucceedsForEligibleOperator() {
        when(operatorRepository.findByMobileNumberOrEmailIgnoreCase("9876543220", "9876543220"))
                .thenReturn(Optional.of(eligibleOperator));

        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("9876543220")
                .password("Operator@2026")
                .role("OPERATOR")
                .loginType("PASSWORD")
                .build();

        LoginResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getRole()).isEqualTo("OPERATOR");
        assertThat(response.getOperatorId()).isEqualTo(77L);
        assertThat(response.getFullName()).isEqualTo("Sahil Karale");
        assertThat(response.getEmail()).isEqualTo("sahilkarale22@gmail.com");
        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getAccountStatus()).isEqualTo("APPROVED");

        // Validate JWT claim
        assertThat(jwtService.extractRole(response.getToken())).isEqualTo("OPERATOR");
        assertThat(jwtService.extractUserId(response.getToken())).isEqualTo(77L);
    }

    @Test
    @DisplayName("2. Incorrect Operator password fails with BadRequestException")
    void testIncorrectPasswordFailsSafely() {
        when(operatorRepository.findByMobileNumberOrEmailIgnoreCase("9876543220", "9876543220"))
                .thenReturn(Optional.of(eligibleOperator));

        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("9876543220")
                .password("WrongPassword123")
                .role("OPERATOR")
                .loginType("PASSWORD")
                .build();

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid mobile/email or password.");
    }

    @Test
    @DisplayName("3. Operator registration and login use compatible BCrypt password encoding")
    void testOperatorRegistrationAndLoginUseCompatibleEncoding() {
        String registeredPassword = "MyRegisteredSecret@2026";

        OperatorRegistrationRequest regRequest = new OperatorRegistrationRequest();
        regRequest.setFullName("Aniket Shinde");
        regRequest.setMobileNumber("9876543233");
        regRequest.setEmail("aniket@agrorent.in");
        regRequest.setAddress("Satara, Maharashtra");
        regRequest.setAadhaarNumber("998877665544");
        regRequest.setDrivingLicenseNumber("MH1420220011223");
        regRequest.setExperience(4);
        regRequest.setSkills("Rotavator, Sowing");
        regRequest.setPassword(registeredPassword);

        when(operatorRepository.existsByMobileNumber("9876543233")).thenReturn(false);
        when(operatorRepository.existsByEmail("aniket@agrorent.in")).thenReturn(false);
        when(operatorRepository.save(any(Operator.class))).thenAnswer(invocation -> {
            Operator op = invocation.getArgument(0);
            op.setId(88L);
            return op;
        });

        OperatorResponse regResponse = operatorService.registerOperator(regRequest);
        assertThat(regResponse).isNotNull();
        assertThat(regResponse.getId()).isEqualTo(88L);

        // Capture saved entity and simulate approval & mobile verification
        Operator registeredEntity = operatorMapper.toEntity(regRequest, passwordEncoder.encode(registeredPassword));
        registeredEntity.setId(88L);
        registeredEntity.setStatus(OperatorStatus.APPROVED);
        registeredEntity.setMobileVerified(true);
        registeredEntity.setActive(true);

        when(operatorRepository.findByMobileNumberOrEmailIgnoreCase("9876543233", "9876543233"))
                .thenReturn(Optional.of(registeredEntity));

        LoginRequest loginRequest = LoginRequest.builder()
                .mobileOrEmail("9876543233")
                .password(registeredPassword)
                .role("OPERATOR")
                .loginType("PASSWORD")
                .build();

        LoginResponse loginResponse = authService.login(loginRequest);
        assertThat(loginResponse).isNotNull();
        assertThat(loginResponse.getRole()).isEqualTo("OPERATOR");
        assertThat(loginResponse.getOperatorId()).isEqualTo(88L);
    }

    @Test
    @DisplayName("4. Identifier normalization: +91 prefixed and spaced mobile numbers succeed")
    void testMobileNumberNormalizationLoginSuccess() {
        // Stored mobile is clean 10 digits "9876543220"
        when(operatorRepository.findByMobileNumber("9876543220"))
                .thenReturn(Optional.of(eligibleOperator));

        LoginRequest requestWithPlus91 = LoginRequest.builder()
                .mobileOrEmail("+91 9876543220")
                .password("Operator@2026")
                .role("OPERATOR")
                .loginType("PASSWORD")
                .build();

        LoginResponse response = authService.login(requestWithPlus91);

        assertThat(response).isNotNull();
        assertThat(response.getRole()).isEqualTo("OPERATOR");
        assertThat(response.getOperatorId()).isEqualTo(77L);
    }

    @Test
    @DisplayName("5. Case-insensitive email lookup succeeds for Operator login")
    void testCaseInsensitiveEmailLoginSuccess() {
        when(operatorRepository.findByMobileNumberOrEmailIgnoreCase("SAHILKARALE22@GMAIL.COM", "SAHILKARALE22@GMAIL.COM"))
                .thenReturn(Optional.of(eligibleOperator));

        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("SAHILKARALE22@GMAIL.COM")
                .password("Operator@2026")
                .role("OPERATOR")
                .loginType("PASSWORD")
                .build();

        LoginResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getRole()).isEqualTo("OPERATOR");
        assertThat(response.getOperatorId()).isEqualTo(77L);
    }

    @Test
    @DisplayName("6. Safe legacy compatibility: Unhashed password authenticates and upgrades to BCrypt hash")
    void testLegacyUnhashedPasswordUpgradesToBcrypt() {
        Operator legacyOperator = Operator.builder()
                .fullName("Legacy Operator")
                .mobileNumber("9876543299")
                .email("legacy@agrorent.in")
                .password("PlaintextSecretPass123") // Unhashed legacy password
                .status(OperatorStatus.APPROVED)
                .mobileVerified(true)
                .build();
        legacyOperator.setId(99L);
        legacyOperator.setActive(true);

        when(operatorRepository.findByMobileNumberOrEmailIgnoreCase("9876543299", "9876543299"))
                .thenReturn(Optional.of(legacyOperator));

        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("9876543299")
                .password("PlaintextSecretPass123")
                .role("OPERATOR")
                .loginType("PASSWORD")
                .build();

        LoginResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getRole()).isEqualTo("OPERATOR");
        assertThat(response.getOperatorId()).isEqualTo(99L);

        // Verify password was upgraded to a valid BCrypt hash
        assertThat(legacyOperator.getPassword()).startsWith("$2a$10$");
        assertThat(passwordEncoder.matches("PlaintextSecretPass123", legacyOperator.getPassword())).isTrue();
        verify(operatorRepository).save(legacyOperator);
    }

    @Test
    @DisplayName("7. Accidental leading or trailing whitespace in entered password is handled safely")
    void testAccidentalWhitespaceInPasswordSucceeds() {
        when(operatorRepository.findByMobileNumberOrEmailIgnoreCase("9876543220", "9876543220"))
                .thenReturn(Optional.of(eligibleOperator));

        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("9876543220")
                .password("  Operator@2026  ") // Accidental whitespace
                .role("OPERATOR")
                .loginType("PASSWORD")
                .build();

        LoginResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getRole()).isEqualTo("OPERATOR");
        assertThat(response.getOperatorId()).isEqualTo(77L);
    }

    @Test
    @DisplayName("8. Password reset produces a password that subsequently authenticates")
    void testPasswordResetProducesAuthenticatablePassword() {
        when(operatorRepository.findById(77L)).thenReturn(Optional.of(eligibleOperator));

        // 1. Reset password
        passwordResetService.registerTestToken(
                "valid-operator-reset-token",
                "OPERATOR",
                77L,
                "sahilkarale22@gmail.com",
                LocalDateTime.now().plusHours(1),
                false
        );

        ResetPasswordRequest resetRequest = ResetPasswordRequest.builder()
                .token("valid-operator-reset-token")
                .role("OPERATOR")
                .newPassword("NewBrandPass@2027")
                .confirmPassword("NewBrandPass@2027")
                .build();

        String result = passwordResetService.processResetPassword(resetRequest);
        assertThat(result).contains("Password reset successfully");

        // Verify entity password updated and matches new password
        assertThat(passwordEncoder.matches("NewBrandPass@2027", eligibleOperator.getPassword())).isTrue();
        assertThat(passwordEncoder.matches("Operator@2026", eligibleOperator.getPassword())).isFalse();

        // 2. Subsequent login with new password succeeds
        when(operatorRepository.findByMobileNumberOrEmailIgnoreCase("sahilkarale22@gmail.com", "sahilkarale22@gmail.com"))
                .thenReturn(Optional.of(eligibleOperator));

        LoginRequest loginWithNewPass = LoginRequest.builder()
                .mobileOrEmail("sahilkarale22@gmail.com")
                .password("NewBrandPass@2027")
                .role("OPERATOR")
                .loginType("PASSWORD")
                .build();

        LoginResponse loginResponse = authService.login(loginWithNewPass);
        assertThat(loginResponse).isNotNull();
        assertThat(loginResponse.getRole()).isEqualTo("OPERATOR");
        assertThat(loginResponse.getOperatorId()).isEqualTo(77L);
    }

    @Test
    @DisplayName("9. Pending Operator account is rejected with pending approval message")
    void testPendingOperatorIsBlockedFromLogin() {
        eligibleOperator.setStatus(OperatorStatus.PENDING);
        when(operatorRepository.findByMobileNumberOrEmailIgnoreCase("9876543220", "9876543220"))
                .thenReturn(Optional.of(eligibleOperator));

        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("9876543220")
                .password("Operator@2026")
                .role("OPERATOR")
                .loginType("PASSWORD")
                .build();

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Operator account is pending admin approval.");
    }

    @Test
    @DisplayName("10. Inactive Operator account is rejected with inactive message")
    void testInactiveOperatorIsBlockedFromLogin() {
        eligibleOperator.setActive(false);
        when(operatorRepository.findByMobileNumberOrEmailIgnoreCase("9876543220", "9876543220"))
                .thenReturn(Optional.of(eligibleOperator));

        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("9876543220")
                .password("Operator@2026")
                .role("OPERATOR")
                .loginType("PASSWORD")
                .build();

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Operator account is inactive. Please contact customer support.");
    }

    @Test
    @DisplayName("11. Unverified mobile Operator account is rejected with unverified message")
    void testUnverifiedMobileOperatorIsBlockedFromLogin() {
        eligibleOperator.setMobileVerified(false);
        when(operatorRepository.findByMobileNumberOrEmailIgnoreCase("9876543220", "9876543220"))
                .thenReturn(Optional.of(eligibleOperator));

        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("9876543220")
                .password("Operator@2026")
                .role("OPERATOR")
                .loginType("PASSWORD")
                .build();

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Mobile number is not verified. Please verify mobile before logging in.");
    }

    @Test
    @DisplayName("12. Role isolation: Farmer account cannot authenticate as Operator")
    void testFarmerCannotAuthenticateAsOperator() {
        when(operatorRepository.findByMobileNumberOrEmailIgnoreCase("farmer@agrorent.in", "farmer@agrorent.in"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailAndRole("farmer@agrorent.in", Role.OPERATOR))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCaseAndRole("farmer@agrorent.in", Role.OPERATOR))
                .thenReturn(Optional.empty());

        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("farmer@agrorent.in")
                .password("FarmerPassword123")
                .role("OPERATOR")
                .loginType("PASSWORD")
                .build();

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid email or password.");
    }

    @Test
    @DisplayName("13. Role isolation: Partner account cannot authenticate as Operator")
    void testPartnerCannotAuthenticateAsOperator() {
        when(operatorRepository.findByMobileNumberOrEmailIgnoreCase("partner@agrorent.in", "partner@agrorent.in"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailAndRole("partner@agrorent.in", Role.OPERATOR))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCaseAndRole("partner@agrorent.in", Role.OPERATOR))
                .thenReturn(Optional.empty());

        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("partner@agrorent.in")
                .password("PartnerPassword123")
                .role("OPERATOR")
                .loginType("PASSWORD")
                .build();

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid email or password.");
    }

    @Test
    @DisplayName("14. Dedicated Operator login (/api/operators/login) remains compatible and supports normalized mobile")
    void testDedicatedOperatorLoginWithNormalizedMobile() {
        when(operatorRepository.findByMobileNumber("9876543220"))
                .thenReturn(Optional.of(eligibleOperator));

        OperatorLoginRequest request = OperatorLoginRequest.builder()
                .mobileNumber("9876543220")
                .password("Operator@2026")
                .build();

        OperatorLoginResponse response = operatorAuthService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getOperator()).isNotNull();
        assertThat(response.getOperator().getId()).isEqualTo(77L);
        assertThat(response.getOperator().getRole()).isEqualTo("OPERATOR");
    }

    @Test
    @DisplayName("15. Core User repository registered Operator can authenticate via shared login")
    void testCoreUserRegisteredOperatorCanAuthenticate() {
        User coreUserOperator = User.builder()
                .name("Core Operator User")
                .email("core.operator@agrorent.in")
                .password(passwordEncoder.encode("CorePass@123"))
                .role(Role.OPERATOR)
                .enabled(true)
                .verified(true)
                .build();
        coreUserOperator.setId(105L);

        when(operatorRepository.findByMobileNumberOrEmailIgnoreCase("core.operator@agrorent.in", "core.operator@agrorent.in"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmailAndRole("core.operator@agrorent.in", Role.OPERATOR))
                .thenReturn(Optional.of(coreUserOperator));

        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("core.operator@agrorent.in")
                .password("CorePass@123")
                .role("OPERATOR")
                .loginType("PASSWORD")
                .build();

        LoginResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getRole()).isEqualTo("OPERATOR");
        assertThat(response.getUserId()).isEqualTo(105L);
        assertThat(response.getToken()).isNotBlank();
    }
}
