package com.agrorental.operator.service;

import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.operator.dto.OperatorOtpResponse;
import com.agrorental.operator.dto.OperatorOtpSendRequest;
import com.agrorental.operator.dto.OperatorOtpVerifyRequest;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorOtp;
import com.agrorental.operator.repository.OperatorOtpRepository;
import com.agrorental.operator.repository.OperatorRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class OperatorOtpService {

    private static final int MAX_ATTEMPTS = 3;
    private final SecureRandom secureRandom = new SecureRandom();

    private final OperatorRepository operatorRepository;
    private final OperatorOtpRepository operatorOtpRepository;

    @Value("${operator.otp.expiry-minutes:5}")
    private int expiryMinutes;

    @Value("${operator.otp.resend-cooldown-seconds:60}")
    private int cooldownSeconds;

    public OperatorOtpService(
            OperatorRepository operatorRepository,
            OperatorOtpRepository operatorOtpRepository) {
        this.operatorRepository = operatorRepository;
        this.operatorOtpRepository = operatorOtpRepository;
    }

    @Transactional
    public OperatorOtpResponse sendOtp(OperatorOtpSendRequest request) {
        String mobileNumber = request.getMobileNumber();

        Operator operator = operatorRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found with mobile number: " + mobileNumber));

        if (operator.isMobileVerified()) {
            throw new BadRequestException("Mobile number is already verified");
        }

        // Check resend cooldown
        Optional<OperatorOtp> latestOtpOpt = operatorOtpRepository
                .findTopByMobileNumberAndUsedFalseOrderByCreatedAtDesc(mobileNumber);

        if (latestOtpOpt.isPresent()) {
            OperatorOtp latestOtp = latestOtpOpt.get();
            LocalDateTime createdAt = latestOtp.getCreatedAt();
            if (createdAt != null) {
                long elapsedSeconds = Duration.between(createdAt, LocalDateTime.now()).getSeconds();
                if (elapsedSeconds < cooldownSeconds) {
                    long waitSeconds = cooldownSeconds - elapsedSeconds;
                    throw new BadRequestException("Please wait " + waitSeconds + " seconds before requesting a new OTP");
                }
            }
        }

        // Invalidate previous unused OTPs for this mobile
        List<OperatorOtp> previousOtps = operatorOtpRepository.findAllByMobileNumberAndUsedFalse(mobileNumber);
        for (OperatorOtp otp : previousOtps) {
            otp.setUsed(true);
        }
        operatorOtpRepository.saveAll(previousOtps);

        // Generate 6-digit OTP
        int randomPin = 100000 + secureRandom.nextInt(900000);
        String otpCode = String.valueOf(randomPin);

        OperatorOtp newOtp = OperatorOtp.builder()
                .operator(operator)
                .mobileNumber(mobileNumber)
                .otpCode(otpCode)
                .purpose("REGISTRATION")
                .expiresAt(LocalDateTime.now().plusMinutes(expiryMinutes))
                .used(false)
                .attemptCount(0)
                .build();

        operatorOtpRepository.save(newOtp);

        // Development log (safe logging for development environment testing)
        log.info("[DEVELOPMENT ONLY] OTP generated for operator mobile {}: {}", mobileNumber, otpCode);

        return OperatorOtpResponse.builder()
                .mobileNumber(mobileNumber)
                .mobileVerified(false)
                .message("OTP sent successfully to registered mobile number")
                .expiresInMinutes(expiryMinutes)
                .build();
    }

    @Transactional(noRollbackFor = BadRequestException.class)
    public OperatorOtpResponse verifyOtp(OperatorOtpVerifyRequest request) {
        String mobileNumber = request.getMobileNumber();
        String enteredOtp = request.getOtp();

        Operator operator = operatorRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found with mobile number: " + mobileNumber));

        if (operator.isMobileVerified()) {
            return OperatorOtpResponse.builder()
                    .mobileNumber(mobileNumber)
                    .mobileVerified(true)
                    .message("Mobile number is already verified")
                    .build();
        }

        OperatorOtp otpRecord = operatorOtpRepository
                .findTopByMobileNumberAndUsedFalseOrderByCreatedAtDesc(mobileNumber)
                .orElseThrow(() -> new BadRequestException("No active OTP found. Please request a new OTP."));

        // Check if OTP is expired
        if (otpRecord.getExpiresAt().isBefore(LocalDateTime.now())) {
            otpRecord.setUsed(true);
            operatorOtpRepository.save(otpRecord);
            throw new BadRequestException("OTP has expired. Please request a new OTP.");
        }

        // Check max attempts
        if (otpRecord.getAttemptCount() >= MAX_ATTEMPTS) {
            otpRecord.setUsed(true);
            operatorOtpRepository.save(otpRecord);
            throw new BadRequestException("Maximum verification attempts exceeded. Please request a new OTP.");
        }

        // Verify OTP code
        if (!otpRecord.getOtpCode().equals(enteredOtp)) {
            int newAttemptCount = otpRecord.getAttemptCount() + 1;
            otpRecord.setAttemptCount(newAttemptCount);

            if (newAttemptCount >= MAX_ATTEMPTS) {
                otpRecord.setUsed(true);
                operatorOtpRepository.save(otpRecord);
                throw new BadRequestException("Invalid OTP. Maximum attempts exceeded. Please request a new OTP.");
            }

            operatorOtpRepository.save(otpRecord);
            int remainingAttempts = MAX_ATTEMPTS - newAttemptCount;
            throw new BadRequestException("Invalid OTP. " + remainingAttempts + " attempt(s) remaining.");
        }

        // On successful verification:
        otpRecord.setUsed(true);
        operatorOtpRepository.save(otpRecord);

        operator.setMobileVerified(true);
        operatorRepository.save(operator);

        log.info("Operator mobile {} successfully verified", mobileNumber);

        return OperatorOtpResponse.builder()
                .mobileNumber(mobileNumber)
                .mobileVerified(true)
                .message("Mobile number verified successfully")
                .build();
    }

    @Transactional
    public OperatorOtpResponse resendOtp(OperatorOtpSendRequest request) {
        return sendOtp(request);
    }
}
