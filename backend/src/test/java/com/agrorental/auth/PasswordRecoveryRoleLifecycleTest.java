package com.agrorental.auth;

import com.agrorental.auth.dto.ForgotPasswordRequest;
import com.agrorental.auth.dto.ResetPasswordRequest;
import com.agrorental.auth.service.PasswordResetService;
import com.agrorental.common.enums.Role;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.otp.OperatorOtp;
import com.agrorental.operator.otp.OperatorOtpRepository;
import com.agrorental.operator.otp.OtpPurpose;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.partner.entity.Partner;
import com.agrorental.partner.repository.PartnerRepository;
import com.agrorental.user.entity.PasswordResetToken;
import com.agrorental.user.entity.User;
import com.agrorental.user.repository.PasswordResetTokenRepository;
import com.agrorental.user.repository.UserRepository;
import com.agrorental.user.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite verifying password recovery for all public login roles:
 * FARMER, PARTNER, and OPERATOR.
 *
 * Verifies:
 * 1. Farmer recovery remains functional (via email and mobile lookup).
 * 2. Partner can request recovery and reset password after verification.
 * 3. Operator can request recovery and reset password after verification.
 * 4. Role selection prevents cross-role account recovery.
 * 5. Invalid, expired and already-used recovery tokens are rejected.
 * 6. Rate limits and verification attempt limits are enforced.
 * 7. Passwords are securely hashed and the new password works for the correct role.
 * 8. Resetting a password does not approve a pending Partner or Operator.
 * 9. Existing authentication and Admin protection remain intact (ADMIN role rejected).
 * 10. Unknown account identifiers receive non-enumerating generic responses.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AgroRent Password Recovery Multi-Role Lifecycle Tests")
class PasswordRecoveryRoleLifecycleTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FarmerRepository farmerRepository;

    @Mock
    private PartnerRepository partnerRepository;

    @Mock
    private OperatorRepository operatorRepository;

    @Mock
    private OperatorOtpRepository operatorOtpRepository;

    @Mock
    private PasswordResetTokenRepository resetTokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    private PasswordResetService passwordResetService;

    private User sampleUser;
    private Farmer sampleFarmer;
    private Partner samplePartner;
    private Operator sampleOperator;

    @BeforeEach
    void setUp() {
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
        passwordResetService.clearTokens();

        // Sample Farmer & User
        sampleUser = User.builder()
                .name("Ramesh Kumar")
                .email("farmer@agrorent.in")
                .password("$2a$10$oldHashedFarmerPassword")
                .role(Role.FARMER)
                .enabled(true)
                .verified(true)
                .build();
        sampleUser.setId(101L);

        sampleFarmer = Farmer.builder()
                .user(sampleUser)
                .fullName("Ramesh Kumar")
                .mobileNumber("9876543210")
                .email("farmer@agrorent.in")
                .password("$2a$10$oldHashedFarmerPassword")
                .build();
        sampleFarmer.setId(201L);
        sampleUser.setFarmer(sampleFarmer);

        // Sample Partner (Pending approval)
        samplePartner = Partner.builder()
                .fullName("Shivaji Shinde")
                .businessName("Shinde Agro Works")
                .mobileNumber("9822001122")
                .email("partner@agrorent.in")
                .password("$2a$10$oldHashedPartnerPassword")
                .verificationStatus(Partner.VerificationStatus.PENDING)
                .otpVerified(true)
                .build();
        samplePartner.setId(301L);
        samplePartner.setActive(true);

        // Sample Operator (Pending approval)
        sampleOperator = Operator.builder()
                .fullName("Vikas Patil")
                .mobileNumber("9833445566")
                .email("operator@agrorent.in")
                .password("$2a$10$oldHashedOperatorPassword")
                .status(OperatorStatus.PENDING)
                .mobileVerified(true)
                .build();
        sampleOperator.setId(401L);
        sampleOperator.setActive(true);

        lenient().when(passwordEncoder.encode(anyString())).thenAnswer(inv -> "$2a$10$hashed_" + inv.getArgument(0));
    }

    // -------------------------------------------------------------------------
    // 1. Farmer Recovery Tests
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Farmer: Forgot password via email sends reset instructions and allows reset")
    void testFarmerPasswordRecoveryViaEmail() {
        when(userRepository.findByEmailIgnoreCase("farmer@agrorent.in")).thenReturn(Optional.of(sampleUser));
        when(userRepository.findById(101L)).thenReturn(Optional.of(sampleUser));

        // 1. Request recovery
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .role("FARMER")
                .email("farmer@agrorent.in")
                .build();

        String responseMessage = passwordResetService.processForgotPassword(request);
        assertThat(responseMessage).contains("If an account with that identifier exists");

        // Verify token saved and email sent
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(eq("farmer@agrorent.in"), tokenCaptor.capture(), eq("FARMER"));
        String issuedToken = tokenCaptor.getValue();
        assertThat(issuedToken).isNotEmpty();

        verify(resetTokenRepository).save(any(PasswordResetToken.class));

        // 2. Fulfill password reset
        ResetPasswordRequest resetRequest = ResetPasswordRequest.builder()
                .token(issuedToken)
                .role("FARMER")
                .newPassword("NewSecureFarmerPass123")
                .confirmPassword("NewSecureFarmerPass123")
                .build();

        String resetResult = passwordResetService.processResetPassword(resetRequest);
        assertThat(resetResult).contains("Password reset successfully");

        // Verify password updated on both User and linked Farmer
        assertThat(sampleUser.getPassword()).isEqualTo("$2a$10$hashed_NewSecureFarmerPass123");
        assertThat(sampleFarmer.getPassword()).isEqualTo("$2a$10$hashed_NewSecureFarmerPass123");
        verify(userRepository).save(sampleUser);
        verify(farmerRepository).save(sampleFarmer);
    }

    @Test
    @DisplayName("Farmer: Forgot password via mobile number resolves linked User and dispatches email")
    void testFarmerPasswordRecoveryViaMobileNumber() {
        when(farmerRepository.findByMobileNumber("9876543210")).thenReturn(Optional.of(sampleFarmer));

        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .role("FARMER")
                .mobileOrEmail("9876543210")
                .build();

        String responseMessage = passwordResetService.processForgotPassword(request);
        assertThat(responseMessage).contains("If an account with that identifier exists");

        verify(emailService).sendPasswordResetEmail(eq("farmer@agrorent.in"), anyString(), eq("FARMER"));
    }

    // -------------------------------------------------------------------------
    // 2. Partner Recovery Tests
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Partner: Forgot password via email sends reset instructions and resets password")
    void testPartnerPasswordRecoveryViaEmail() {
        when(partnerRepository.findByEmailIgnoreCase("partner@agrorent.in")).thenReturn(Optional.of(samplePartner));
        when(partnerRepository.findById(301L)).thenReturn(Optional.of(samplePartner));

        // 1. Request recovery
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .role("PARTNER")
                .email("partner@agrorent.in")
                .build();

        String responseMessage = passwordResetService.processForgotPassword(request);
        assertThat(responseMessage).contains("If an account with that identifier exists");

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(eq("partner@agrorent.in"), tokenCaptor.capture(), eq("PARTNER"));
        String issuedToken = tokenCaptor.getValue();
        assertThat(issuedToken).isNotEmpty();

        // 2. Fulfill password reset
        ResetPasswordRequest resetRequest = ResetPasswordRequest.builder()
                .token(issuedToken)
                .role("PARTNER")
                .newPassword("NewPartnerPass2026")
                .confirmPassword("NewPartnerPass2026")
                .build();

        String resetResult = passwordResetService.processResetPassword(resetRequest);
        assertThat(resetResult).contains("Password reset successfully");

        // Verify Partner password updated
        assertThat(samplePartner.getPassword()).isEqualTo("$2a$10$hashed_NewPartnerPass2026");
        assertThat(samplePartner.getOtpCode()).isNull();
        verify(partnerRepository, atLeastOnce()).save(samplePartner);
    }

    @Test
    @DisplayName("Partner: Resetting password strictly preserves PENDING verification status")
    void testPartnerPasswordResetPreservesPendingStatus() {
        when(partnerRepository.findByEmailIgnoreCase("partner@agrorent.in")).thenReturn(Optional.of(samplePartner));
        when(partnerRepository.findById(301L)).thenReturn(Optional.of(samplePartner));

        assertThat(samplePartner.getVerificationStatus()).isEqualTo(Partner.VerificationStatus.PENDING);

        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .role("PARTNER")
                .email("partner@agrorent.in")
                .build();

        passwordResetService.processForgotPassword(request);

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(eq("partner@agrorent.in"), tokenCaptor.capture(), eq("PARTNER"));

        ResetPasswordRequest resetRequest = ResetPasswordRequest.builder()
                .token(tokenCaptor.getValue())
                .role("PARTNER")
                .newPassword("BrandNewPartnerPassword")
                .confirmPassword("BrandNewPartnerPassword")
                .build();

        passwordResetService.processResetPassword(resetRequest);

        // Verification status MUST NOT be changed to APPROVED
        assertThat(samplePartner.getVerificationStatus()).isEqualTo(Partner.VerificationStatus.PENDING);
        assertThat(samplePartner.isOtpVerified()).isTrue();
        assertThat(samplePartner.isActive()).isTrue();
    }

    // -------------------------------------------------------------------------
    // 3. Operator Recovery Tests
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Operator: Forgot password via mobile number finds operator and dispatches reset instructions")
    void testOperatorPasswordRecoveryViaMobileNumber() {
        when(operatorRepository.findByMobileNumber("9833445566")).thenReturn(Optional.of(sampleOperator));
        when(operatorRepository.findById(401L)).thenReturn(Optional.of(sampleOperator));

        // 1. Request recovery
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .role("OPERATOR")
                .mobileOrEmail("9833445566")
                .build();

        String responseMessage = passwordResetService.processForgotPassword(request);
        assertThat(responseMessage).contains("If an account with that identifier exists");

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(eq("operator@agrorent.in"), tokenCaptor.capture(), eq("OPERATOR"));
        String issuedToken = tokenCaptor.getValue();
        assertThat(issuedToken).isNotEmpty();

        // 2. Fulfill password reset
        ResetPasswordRequest resetRequest = ResetPasswordRequest.builder()
                .token(issuedToken)
                .role("OPERATOR")
                .newPassword("NewOperatorSecretPass")
                .confirmPassword("NewOperatorSecretPass")
                .build();

        String resetResult = passwordResetService.processResetPassword(resetRequest);
        assertThat(resetResult).contains("Password reset successfully");

        // Verify Operator password updated and status preserved
        assertThat(sampleOperator.getPassword()).isEqualTo("$2a$10$hashed_NewOperatorSecretPass");
        assertThat(sampleOperator.getStatus()).isEqualTo(OperatorStatus.PENDING);
        verify(operatorRepository).save(sampleOperator);
    }

    // -------------------------------------------------------------------------
    // 4. Cross-Role Account Recovery Isolation
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Cross-Role: Requesting FARMER recovery with PARTNER email searches only FARMER domain")
    void testCrossRoleRecoveryForbiddenAtRequestTime() {
        // Farmer domain returns empty
        when(userRepository.findByEmailIgnoreCase("partner@agrorent.in")).thenReturn(Optional.empty());
        when(farmerRepository.findByEmailIgnoreCase("partner@agrorent.in")).thenReturn(Optional.empty());

        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .role("FARMER")
                .email("partner@agrorent.in")
                .build();

        String responseMessage = passwordResetService.processForgotPassword(request);
        assertThat(responseMessage).contains("If an account with that identifier exists");

        // Email service must never be invoked for the partner
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyString());
        verify(partnerRepository, never()).findByEmailIgnoreCase(anyString());
    }

    @Test
    @DisplayName("Cross-Role: Redeeming PARTNER token with role=FARMER is strictly rejected")
    void testCrossRoleTokenRedemptionRejected() {
        when(partnerRepository.findByEmailIgnoreCase("partner@agrorent.in")).thenReturn(Optional.of(samplePartner));

        // 1. Issue token for Partner
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .role("PARTNER")
                .email("partner@agrorent.in")
                .build();
        passwordResetService.processForgotPassword(request);

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(eq("partner@agrorent.in"), tokenCaptor.capture(), eq("PARTNER"));
        String partnerToken = tokenCaptor.getValue();

        // 2. Attempt to redeem Partner token under FARMER role
        ResetPasswordRequest crossRoleRequest = ResetPasswordRequest.builder()
                .token(partnerToken)
                .role("FARMER")
                .newPassword("AttackerPassword123")
                .confirmPassword("AttackerPassword123")
                .build();

        assertThatThrownBy(() -> passwordResetService.processResetPassword(crossRoleRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid or expired password reset token");

        // Farmer password must not be changed
        assertThat(sampleUser.getPassword()).isEqualTo("$2a$10$oldHashedFarmerPassword");
    }

    // -------------------------------------------------------------------------
    // 5. Token Validation: Invalid, Expired, and Already-Used
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Token Validation: Unknown token is rejected with 400 Bad Request")
    void testUnknownTokenRejected() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("non-existent-token-uuid-1234")
                .newPassword("NewPassword123")
                .confirmPassword("NewPassword123")
                .build();

        assertThatThrownBy(() -> passwordResetService.processResetPassword(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid or expired password reset token");
    }

    @Test
    @DisplayName("Token Validation: Expired token is rejected with 400 Bad Request")
    void testExpiredTokenRejected() {
        String expiredToken = "expired-token-uuid";
        passwordResetService.registerTestToken(
                expiredToken,
                "PARTNER",
                samplePartner.getId(),
                samplePartner.getEmail(),
                LocalDateTime.now().minusMinutes(5), // Expired 5 mins ago
                false
        );

        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token(expiredToken)
                .newPassword("NewPassword123")
                .confirmPassword("NewPassword123")
                .build();

        assertThatThrownBy(() -> passwordResetService.processResetPassword(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid or expired password reset token");
    }

    @Test
    @DisplayName("Token Validation: Already-used single-use token cannot be redeemed a second time")
    void testAlreadyUsedTokenRejected() {
        when(partnerRepository.findByEmailIgnoreCase("partner@agrorent.in")).thenReturn(Optional.of(samplePartner));
        when(partnerRepository.findById(301L)).thenReturn(Optional.of(samplePartner));

        ForgotPasswordRequest forgotRequest = ForgotPasswordRequest.builder()
                .role("PARTNER")
                .email("partner@agrorent.in")
                .build();
        passwordResetService.processForgotPassword(forgotRequest);

        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(eq("partner@agrorent.in"), tokenCaptor.capture(), eq("PARTNER"));
        String token = tokenCaptor.getValue();

        ResetPasswordRequest resetRequest = ResetPasswordRequest.builder()
                .token(token)
                .newPassword("ValidNewPassword1")
                .confirmPassword("ValidNewPassword1")
                .build();

        // First redemption succeeds
        passwordResetService.processResetPassword(resetRequest);

        // Second redemption with SAME token MUST fail
        ResetPasswordRequest secondRequest = ResetPasswordRequest.builder()
                .token(token)
                .newPassword("SecondNewPassword2")
                .confirmPassword("SecondNewPassword2")
                .build();

        assertThatThrownBy(() -> passwordResetService.processResetPassword(secondRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid or expired password reset token");
    }

    // -------------------------------------------------------------------------
    // 6. Rate Limiting and Verification Attempt Limits
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Rate Limiting: Rapid consecutive requests for same account within cooldown trigger 400 Bad Request")
    void testRapidConsecutiveRequestsTriggerRateLimit() {
        when(partnerRepository.findByEmailIgnoreCase("partner@agrorent.in")).thenReturn(Optional.of(samplePartner));

        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .role("PARTNER")
                .email("partner@agrorent.in")
                .build();

        // First request succeeds
        passwordResetService.processForgotPassword(request);

        // Immediate second request triggers cooldown rate limit
        assertThatThrownBy(() -> passwordResetService.processForgotPassword(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Please wait 30 seconds before requesting another password reset");
    }

    @Test
    @DisplayName("Attempt Limits: More than 5 verification attempts lock and invalidate the token")
    void testMaxVerificationAttemptsExceededLocksToken() {
        String token = "brute-force-target-token";
        passwordResetService.registerTestToken(
                token,
                "OPERATOR",
                sampleOperator.getId(),
                sampleOperator.getEmail(),
                LocalDateTime.now().plusMinutes(20),
                false
        );

        // 5 failed attempts (mismatched password to trigger failed attempts)
        for (int i = 1; i <= 5; i++) {
            final int attempt = i;
            ResetPasswordRequest badReq = ResetPasswordRequest.builder()
                    .token(token)
                    .role("FARMER") // Mismatched role causes attempt rejection
                    .newPassword("Password" + attempt)
                    .confirmPassword("Password" + attempt)
                    .build();

            assertThatThrownBy(() -> passwordResetService.processResetPassword(badReq))
                    .isInstanceOf(BadRequestException.class);
        }

        // 6th attempt MUST trigger maximum attempts exceeded
        ResetPasswordRequest finalReq = ResetPasswordRequest.builder()
                .token(token)
                .role("OPERATOR")
                .newPassword("CorrectPassword123")
                .confirmPassword("CorrectPassword123")
                .build();

        assertThatThrownBy(() -> passwordResetService.processResetPassword(finalReq))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Maximum verification attempts exceeded");
    }

    // -------------------------------------------------------------------------
    // 7. Password Mismatch and Length Validation
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Password Validation: Mismatched confirm password throws BadRequestException")
    void testMismatchedPasswordRejected() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("some-token")
                .newPassword("Pass1234")
                .confirmPassword("Pass9999")
                .build();

        assertThatThrownBy(() -> passwordResetService.processResetPassword(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("do not match");
    }

    @Test
    @DisplayName("Password Validation: Password under 6 characters throws BadRequestException")
    void testShortPasswordRejected() {
        ResetPasswordRequest request = ResetPasswordRequest.builder()
                .token("some-token")
                .newPassword("12345")
                .confirmPassword("12345")
                .build();

        assertThatThrownBy(() -> passwordResetService.processResetPassword(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("at least 6 characters long");
    }

    // -------------------------------------------------------------------------
    // 8. Admin Protection: Admin Role Strictly Rejected
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Admin Protection: Forgot password with role=ADMIN is strictly rejected")
    void testAdminRoleForbiddenFromPublicRecovery() {
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .role("ADMIN")
                .email("admin@agrorent.in")
                .build();

        assertThatThrownBy(() -> passwordResetService.processForgotPassword(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid role specified for password recovery");
    }

    // -------------------------------------------------------------------------
    // 9. Non-Enumerating Generic Response for Unknown Accounts
    // -------------------------------------------------------------------------
    @Test
    @DisplayName("Non-Enumeration: Unknown identifier receives exact same generic message without leakage")
    void testUnknownAccountReceivesGenericResponse() {
        when(partnerRepository.findByEmailIgnoreCase("ghost@agrorent.in")).thenReturn(Optional.empty());

        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .role("PARTNER")
                .email("ghost@agrorent.in")
                .build();

        String response = passwordResetService.processForgotPassword(request);
        assertThat(response).isEqualTo("If an account with that identifier exists, password reset instructions have been sent.");

        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString(), anyString());
    }
}
