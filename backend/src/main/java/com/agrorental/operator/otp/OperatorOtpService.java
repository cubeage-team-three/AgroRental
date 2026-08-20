package com.agrorental.operator.otp;

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

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service managing the Operator OTP lifecycle: generation, delivery, secure hashing,
 * attempt counting, expiration enforcement, and mobile verification updates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperatorOtpService {

    private final OperatorOtpRepository operatorOtpRepository;
    private final OperatorRepository operatorRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpDeliveryService otpDeliveryService;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.otp.expiry-minutes:5}")
    private int otpExpiryMinutes = 5;

    @Value("${app.otp.max-attempts:3}")
    private int maxAttempts = 3;

    @Value("${app.otp.resend-cooldown-seconds:30}")
    private int resendCooldownSeconds = 30;

    @Value("${app.otp.include-dev-mock:true}")
    private boolean includeDevMock = true;

    /**
     * Generates and dispatches a secure 6-digit OTP for the given mobile number.
     * Invalidates any prior active OTPs for the mobile and purpose.
     */
    @Transactional
    public OperatorOtpResponse sendOtp(OperatorOtpSendRequest request) {
        String mobile = request.getMobileNumber().trim();
        OtpPurpose purpose = request.getPurpose() != null ? request.getPurpose() : OtpPurpose.MOBILE_VERIFICATION;

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
                .devMockOtp(includeDevMock ? rawOtp : null)
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

        // Mark Operator mobileVerified = true if account exists
        Optional<Operator> operatorOpt = operatorRepository.findByMobileNumber(mobile);
        if (operatorOpt.isPresent()) {
            Operator operator = operatorOpt.get();
            operator.setMobileVerified(true);
            operatorRepository.save(operator);
            log.info("Updated mobileVerified=true for operator ID: {}", operator.getId());
        }

        return OperatorOtpResponse.builder()
                .mobileNumber(mobile)
                .verified(true)
                .message("Mobile number verified successfully.")
                .build();
    }

    private String maskMobile(String mobile) {
        if (mobile == null || mobile.length() < 4) {
            return "******";
        }
        return "******" + mobile.substring(mobile.length() - 4);
    }
}
