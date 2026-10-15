package com.agrorental.auth;

import com.agrorental.auth.dto.LoginRequest;
import com.agrorental.auth.dto.LoginResponse;
import com.agrorental.auth.service.AuthService;
import com.agrorental.common.enums.Role;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.operator.dto.OperatorRegistrationRequest;
import com.agrorental.operator.dto.OperatorResponse;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.mapper.OperatorDocumentMapper;
import com.agrorental.operator.mapper.OperatorMapper;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.operator.service.OperatorService;
import com.agrorental.partner.repository.PartnerRepository;
import com.agrorental.admin.repository.AdminRepository;
import com.agrorental.security.jwt.JwtProperties;
import com.agrorental.security.jwt.JwtService;
import com.agrorental.user.entity.User;
import com.agrorental.user.repository.UserRepository;
import com.agrorental.booking.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

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
 * 8. Approval/inactive messages are only returned after the password was verified (no status leak).
 * 9. Pending and inactive accounts remain blocked; mobile (OTP) verification is no longer required.
 * 10. Role isolation: Farmer and Partner cannot authenticate as Operator.
 * 11. The registration onboarding token is never an operator session token.
 * 12. JWT claims accurately reflect the OPERATOR role.
 *
 * <p>Password reset followed by login is covered end to end (real JPA) by PasswordResetFlowIntegrationTest,
 * and the retired duplicate /api/operators/login by OperatorAuthControllerTest.
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
    private BookingRepository bookingRepository;

    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private OperatorMapper operatorMapper;
    private AuthService authService;
    private OperatorService operatorService;

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
                passwordEncoder,
                jwtService
        );

        operatorService = new OperatorService(
                operatorRepository,
                passwordEncoder,
                operatorMapper,
                bookingRepository,
                jwtService
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
                .hasMessage(AuthService.INVALID_CREDENTIALS_MESSAGE);
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
        assertThat(regResponse.getStatus()).isEqualTo(OperatorStatus.PENDING);
        assertThat(regResponse.isMobileVerified()).isFalse();
        assertThat(regResponse.getOnboardingToken()).isNotBlank();

        // Capture saved entity and simulate admin approval (no OTP / mobile verification step any more)
        Operator registeredEntity = operatorMapper.toEntity(regRequest, passwordEncoder.encode(registeredPassword));
        registeredEntity.setId(88L);
        registeredEntity.setStatus(OperatorStatus.APPROVED);
        registeredEntity.setActive(true);
        assertThat(registeredEntity.isMobileVerified()).isFalse();

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
    @DisplayName("8. Approval state is only revealed after the password was verified")
    void testStatusIsNotRevealedBeforePasswordVerification() {
        eligibleOperator.setStatus(OperatorStatus.PENDING);
        when(operatorRepository.findByMobileNumberOrEmailIgnoreCase("9876543220", "9876543220"))
                .thenReturn(Optional.of(eligibleOperator));

        LoginRequest wrongPassword = LoginRequest.builder()
                .mobileOrEmail("9876543220")
                .password("NotTheRightPassword9")
                .role("OPERATOR")
                .loginType("PASSWORD")
                .build();

        assertThatThrownBy(() -> authService.login(wrongPassword))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(AuthService.INVALID_CREDENTIALS_MESSAGE);

        eligibleOperator.setStatus(OperatorStatus.REJECTED);
        eligibleOperator.setActive(false);
        eligibleOperator.setRejectionReason("Expired licence");
        assertThatThrownBy(() -> authService.login(wrongPassword))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(AuthService.INVALID_CREDENTIALS_MESSAGE);
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
    @DisplayName("11. Approved operator whose mobile was never OTP-verified can log in (mobileVerified is informational)")
    void testUnverifiedMobileOperatorCanLogIn() {
        eligibleOperator.setMobileVerified(false);
        when(operatorRepository.findByMobileNumberOrEmailIgnoreCase("9876543220", "9876543220"))
                .thenReturn(Optional.of(eligibleOperator));

        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("9876543220")
                .password("Operator@2026")
                .role("OPERATOR")
                .loginType("PASSWORD")
                .build();

        LoginResponse response = authService.login(request);
        assertThat(response.getOperatorId()).isEqualTo(77L);
        assertThat(jwtService.extractRole(response.getToken())).isEqualTo("OPERATOR");
        assertThat(jwtService.extractAccountType(response.getToken())).isEqualTo("OPERATOR");
    }

    @Test
    @DisplayName("12. Role isolation: Farmer account cannot authenticate as Operator")
    void testFarmerCannotAuthenticateAsOperator() {
        when(operatorRepository.findByMobileNumberOrEmailIgnoreCase("farmer@agrorent.in", "farmer@agrorent.in"))
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
                .hasMessage(AuthService.INVALID_CREDENTIALS_MESSAGE);
    }

    @Test
    @DisplayName("13. Role isolation: Partner account cannot authenticate as Operator")
    void testPartnerCannotAuthenticateAsOperator() {
        when(operatorRepository.findByMobileNumberOrEmailIgnoreCase("partner@agrorent.in", "partner@agrorent.in"))
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
                .hasMessage(AuthService.INVALID_CREDENTIALS_MESSAGE);
    }

    @Test
    @DisplayName("14. The registration onboarding token is bound to operators.id but is never an OPERATOR session token")
    void testOnboardingTokenIsNotASessionToken() {
        String onboardingToken = jwtService.generateOperatorOnboardingToken(77L);

        assertThat(jwtService.validateToken(onboardingToken)).isTrue();
        assertThat(jwtService.extractUserId(onboardingToken)).isEqualTo(77L);
        assertThat(jwtService.extractAccountType(onboardingToken)).isEqualTo("OPERATOR");
        assertThat(jwtService.extractRole(onboardingToken)).isEqualTo(JwtService.OPERATOR_ONBOARDING_ROLE).isNotEqualTo("OPERATOR");
        assertThat(jwtService.extractScope(onboardingToken)).isEqualTo(JwtService.OPERATOR_ONBOARDING_SCOPE);
        long lifetime = jwtService.extractClaims(onboardingToken).get("exp").asLong()
                - jwtService.extractClaims(onboardingToken).get("iat").asLong();
        assertThat(lifetime).isPositive().isLessThanOrEqualTo(2 * 60 * 60);

        // Session tokens can never be scoped, so a session token cannot be turned into an onboarding token
        assertThatThrownBy(() -> jwtService.generateToken(77L, "OPERATOR", "9876543220",
                new java.util.HashMap<>(java.util.Map.of(JwtService.ACCOUNT_TYPE_CLAIM, "OPERATOR",
                        JwtService.SCOPE_CLAIM, JwtService.OPERATOR_ONBOARDING_SCOPE))))
                .isInstanceOf(IllegalArgumentException.class);
        // Session tokens carry no scope
        assertThat(jwtService.extractScope(jwtService.generateOperatorToken(77L, "9876543220"))).isNull();
    }

    @Test
    @DisplayName("15. P0-3: A users-table OPERATOR account cannot authenticate (operators table only, no fallback)")
    void testCoreUserRegisteredOperatorCannotAuthenticate() {
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
        when(userRepository.findByEmailIgnoreCaseAndRole("core.operator@agrorent.in", Role.OPERATOR))
                .thenReturn(Optional.of(coreUserOperator));

        LoginRequest request = LoginRequest.builder()
                .mobileOrEmail("core.operator@agrorent.in")
                .password("CorePass@123")
                .role("OPERATOR")
                .loginType("PASSWORD")
                .build();

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(AuthService.INVALID_CREDENTIALS_MESSAGE);
    }
}
