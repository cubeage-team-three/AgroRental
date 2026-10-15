package com.agrorental.operator.otp;

import com.agrorental.common.config.DeploymentProfiles;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.operator.dto.OperatorOtpResponse;
import com.agrorental.operator.dto.OperatorOtpSendRequest;
import com.agrorental.operator.dto.OperatorOtpVerifyRequest;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.repository.OperatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Service managing the Operator OTP lifecycle: generation, delivery, secure hashing,
 * attempt counting, expiration enforcement, and mobile verification updates.
 *
 * <p>OTP is no longer part of registration, login or approval: a verified mobile number is informational
 * only (contract v1.1). OTPs never yield a session or a password reset token; the OTP-based password reset
 * purpose is refused (operators use POST /api/auth/forgot-password).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperatorOtpService {

    private final OperatorOtpRepository operatorOtpRepository;
    private final OperatorRepository operatorRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpDeliveryService otpDeliveryService;

    static final String OTP_PASSWORD_RESET_RETIRED_MESSAGE =
            "OTP-based password reset is no longer supported. Please use 'Forgot password?' on the login page.";

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.otp.expiry-minutes:5}")
    private int otpExpiryMinutes = 5;

    @Value("${app.otp.max-attempts:3}")
    private int maxAttempts = 3;

    @Value("${app.otp.resend-cooldown-seconds:30}")
    private int resendCooldownSeconds = 30;

    @Value("${app.otp.include-dev-mock:false}")
    private boolean includeDevMock = false;

    @Autowired(required = false)
    private Environment environment;

    /**
     * Generates and dispatches a secure 6-digit OTP for the given mobile number.
     * Invalidates any prior active OTPs for the mobile and purpose.
     */
    @Transactional
    public OperatorOtpResponse sendOtp(OperatorOtpSendRequest request) {
        String mobile = request.getMobileNumber().trim();
        OtpPurpose purpose = request.getPurpose() != null ? request.getPurpose() : OtpPurpose.MOBILE_VERIFICATION;
        rejectRetiredPurpose(purpose);

        // Check for resend cooldown on latest OTP
        Optional<OperatorOtp> latestOtpOpt = operatorOtpRepository
                .findTopByMobileNumberAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(mobile, purpose);

        if (latestOtpOpt.isPresent()) {
            OperatorOtp latest = latestOtpOpt.get();
            LocalDateTime cooldownEnd = latest.getCreatedAt().plusSeconds(resendCooldownSeconds);
            if (LocalDateTime.now().isBefore(cooldownEnd)) {
                throw new BadRequestException("Please wait " + resendCooldownSeconds + " seconds before requesting a new OTP.");
            }
        }

        // Invalidate all prior unverified OTPs
        List<OperatorOtp> activeOtps = operatorOtpRepository
                .findByMobileNumberAndPurposeAndVerifiedFalse(mobile, purpose);
        for (OperatorOtp oldOtp : activeOtps) {
            oldOtp.setVerified(false);
            oldOtp.setExpiresAt(LocalDateTime.now().minusSeconds(1));
            operatorOtpRepository.save(oldOtp);
        }

        // Generate 6-digit numeric OTP
        int rawOtpNumber = 100000 + secureRandom.nextInt(900000);
        String rawOtp = String.valueOf(rawOtpNumber);

        // Store secure hash
        String hashedOtp = passwordEncoder.encode(rawOtp);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(otpExpiryMinutes);

        // Find associated operator ID if registered
        Optional<Operator> operatorOpt = operatorRepository.findByMobileNumber(mobile);
        Long operatorId = operatorOpt.map(Operator::getId).orElse(null);

        OperatorOtp otpRecord = OperatorOtp.builder()
                .mobileNumber(mobile)
                .operatorId(operatorId)
                .otpHash(hashedOtp)
                .purpose(purpose)
                .expiresAt(expiresAt)
                .attemptCount(0)
                .maxAttempts(maxAttempts)
                .verified(false)
                .build();

        operatorOtpRepository.save(otpRecord);

        // Deliver OTP through delivery service
        otpDeliveryService.deliverOtp(mobile, rawOtp, purpose);

        return OperatorOtpResponse.builder()
                .mobileNumber(mobile)
                .verified(false)
                .message("OTP sent successfully to " + maskMobile(mobile))
                .expiresAt(expiresAt)
                .attemptsRemaining(maxAttempts)
                .devMockOtp(isDevMockOtpAllowed() ? rawOtp : null)
                .build();
    }

    /**
     * Verifies the provided OTP against the active record for the mobile number.
     * Enforces expiry and max-attempt threshold.
     */
    @Transactional
    public OperatorOtpResponse verifyOtp(OperatorOtpVerifyRequest request) {
        String mobile = request.getMobileNumber().trim();
        String userOtp = request.getOtp().trim();
        OtpPurpose purpose = request.getPurpose() != null ? request.getPurpose() : OtpPurpose.MOBILE_VERIFICATION;
        rejectRetiredPurpose(purpose);

        OperatorOtp otpRecord = operatorOtpRepository
                .findTopByMobileNumberAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(mobile, purpose)
                .orElseThrow(() -> new BadRequestException("No active OTP found. Please request a new OTP."));

        // Check if expired
        if (LocalDateTime.now().isAfter(otpRecord.getExpiresAt())) {
            throw new BadRequestException("OTP has expired. Please request a new OTP.");
        }

        // Check attempt threshold
        if (otpRecord.getAttemptCount() >= otpRecord.getMaxAttempts()) {
            throw new BadRequestException("Maximum verification attempts exceeded. Please request a new OTP.");
        }

        // Increment attempt count
        otpRecord.setAttemptCount(otpRecord.getAttemptCount() + 1);

        // Validate OTP match
        boolean matches = passwordEncoder.matches(userOtp, otpRecord.getOtpHash());
        if (!matches) {
            operatorOtpRepository.save(otpRecord);
            int remaining = otpRecord.getMaxAttempts() - otpRecord.getAttemptCount();
            if (remaining > 0) {
                throw new BadRequestException("Invalid OTP. " + remaining + " attempts remaining.");
            } else {
                throw new BadRequestException("Invalid OTP. Maximum attempts exceeded. Please request a new OTP.");
            }
        }

        // Successful verification
        otpRecord.setVerified(true);
        operatorOtpRepository.save(otpRecord);

        // Record the (informational) verified mobile number if an operator account exists
        Optional<Operator> operatorOpt = operatorRepository.findByMobileNumber(mobile);
        if (operatorOpt.isPresent()) {
            Operator operator = operatorOpt.get();
            operator.setMobileVerified(true);
            operatorRepository.save(operator);
            log.info("Updated mobileVerified=true for operator ID: {}", operator.getId());
        }

        // Never a session or reset token: resetToken stays null
        return OperatorOtpResponse.builder()
                .mobileNumber(mobile)
                .verified(true)
                .message("Mobile number verified successfully.")
                .build();
    }

    private void rejectRetiredPurpose(OtpPurpose purpose) {
        if (purpose == OtpPurpose.PASSWORD_RESET) {
            throw new BadRequestException(OTP_PASSWORD_RESET_RETIRED_MESSAGE);
        }
    }

    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 4) {
            return "******";
        }
        return "******" + mobile.substring(mobile.length() - 4);
    }

    @PostConstruct
    public void validateConfiguration() {
        if (isProductionOrStagingEnvironment()) {
            if (includeDevMock) {
                log.warn("CRITICAL SECURITY NOTICE: 'app.otp.include-dev-mock' was set to true, but dev mock OTP disclosure " +
                        "is strictly prohibited in staging/production profiles. Mock OTP will NOT be exposed.");
            }
            log.info("Operator OTP service initialized in secure mode (mock OTP disclosure strictly disabled).");
        } else {
            log.info("Operator OTP service initialized (devMockOtp enabled: {}).", includeDevMock);
        }
    }

    /**
     * Determines whether mock OTP disclosure in API responses is permitted.
     * Requires the explicit opt-in property AND an explicitly active "dev" profile; always false in
     * staging or production and when no profile was activated (implicit default), regardless of properties.
     */
    public boolean isDevMockOtpAllowed() {
        return DeploymentProfiles.isExplicitDevelopment(environment) && includeDevMock;
    }

    private boolean isProductionOrStagingEnvironment() {
        if (environment == null) {
            return false;
        }
        String[] activeProfilesArr = environment.getActiveProfiles();
        List<String> activeProfiles = activeProfilesArr != null ? Arrays.asList(activeProfilesArr) : Collections.emptyList();

        String[] defaultProfilesArr = environment.getDefaultProfiles();
        List<String> defaultProfiles = defaultProfilesArr != null ? Arrays.asList(defaultProfilesArr) : Collections.emptyList();

        return activeProfiles.contains("prod") || activeProfiles.contains("staging")
                || defaultProfiles.contains("prod") || defaultProfiles.contains("staging");
    }

    public void setIncludeDevMock(boolean includeDevMock) {
        this.includeDevMock = includeDevMock;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    public void setOtpExpiryMinutes(int otpExpiryMinutes) {
        this.otpExpiryMinutes = otpExpiryMinutes;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public void setResendCooldownSeconds(int resendCooldownSeconds) {
        this.resendCooldownSeconds = resendCooldownSeconds;
    }
}
