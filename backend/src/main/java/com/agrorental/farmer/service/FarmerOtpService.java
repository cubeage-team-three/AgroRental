package com.agrorental.farmer.service;

import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.farmer.dto.OtpResponse;
import com.agrorental.farmer.dto.SendOtpRequest;
import com.agrorental.farmer.dto.VerifyOtpRequest;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.entity.FarmerOtp;
import com.agrorental.farmer.repository.FarmerOtpRepository;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class FarmerOtpService {

    private final FarmerOtpRepository farmerOtpRepository;
    private final FarmerRepository farmerRepository;
    private final JwtService jwtService;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public OtpResponse sendOtp(SendOtpRequest request) {
        String mobile = request.getMobileNumber().trim();
        log.info("Sending OTP for mobile: {}", mobile);

        Farmer farmer = farmerRepository.findByMobileNumber(mobile)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer not found with mobile number: " + mobile));

        // Generate a 6-digit numeric OTP (e.g. 123456 in dev/test for easy
        // verification)
        String generatedOtp = String.format("%06d", RANDOM.nextInt(900000) + 100000);

        FarmerOtp otpRecord = FarmerOtp.builder()
                .farmerId(farmer.getFarmerId())
                .mobileNumber(mobile)
                .otp(generatedOtp)
                .expiresAt(LocalDateTime.now().plusMinutes(5)) // 5 minutes validity
                .attemptCount(0)
                .verified(false)
                .build();

        farmerOtpRepository.save(otpRecord);
        log.info("Generated OTP for mobile {}: {} (Expires at: {})", mobile, generatedOtp, otpRecord.getExpiresAt());

        return OtpResponse.builder()
                .mobileNumber(mobile)
                .message("OTP sent successfully to +91 " + mobile + ". Valid for 5 minutes.")
                .verified(false)
                .attemptsRemaining(3)
                .expiresAt(otpRecord.getExpiresAt())
                .devMockOtp(generatedOtp)
                .build();
    }

    @Transactional
    public OtpResponse verifyOtp(VerifyOtpRequest request) {
        String mobile = request.getMobileNumber().trim();
        String submittedOtp = request.getOtp().trim();
        log.info("Verifying OTP for mobile: {}", mobile);

        FarmerOtp otpRecord = farmerOtpRepository.findTopByMobileNumberOrderByCreatedAtDesc(mobile)
                .orElseThrow(() -> new BadRequestException(
                        "No active OTP request found for this mobile number. Please request a new OTP."));

        if (Boolean.TRUE.equals(otpRecord.getVerified())) {
            log.info("OTP already verified for mobile: {}", mobile);
            Farmer alreadyVerifiedFarmer = farmerRepository.findByMobileNumber(mobile)
                    .orElseThrow(() -> new ResourceNotFoundException("Farmer not found for mobile: " + mobile));
            return buildSessionResponse(alreadyVerifiedFarmer, "Mobile number is already verified.");
        }

        // Rule 1: Check Expiry
        if (LocalDateTime.now().isAfter(otpRecord.getExpiresAt())) {
            log.warn("OTP expired for mobile {}. Expiration was: {}", mobile, otpRecord.getExpiresAt());
            throw new BadRequestException("OTP has expired. Please click 'Resend OTP' to receive a new code.");
        }

        // Rule 2: Check Attempt Count Lockout (Max 3 attempts)
        if (otpRecord.getAttemptCount() >= 3) {
            log.warn("Maximum OTP attempts exceeded for mobile: {}", mobile);
            throw new BadRequestException("Maximum OTP attempts exceeded (3/3). Please request a new OTP.");
        }

        // Rule 3: Match OTP
        if (!otpRecord.getOtp().equals(submittedOtp)) {
            int newAttemptCount = otpRecord.getAttemptCount() + 1;
            otpRecord.setAttemptCount(newAttemptCount);
            farmerOtpRepository.save(otpRecord);

            int remaining = Math.max(0, 3 - newAttemptCount);
            log.warn("Incorrect OTP entered for mobile {}. Attempts remaining: {}", mobile, remaining);

            if (remaining == 0) {
                throw new BadRequestException(
                        "Invalid OTP. Maximum attempts exceeded (3/3). Please request a new OTP.");
            } else {
                throw new BadRequestException("Invalid OTP code. You have " + remaining + " attempt(s) remaining.");
            }
        }

        // OTP Validated Successfully!
        otpRecord.setVerified(true);
        farmerOtpRepository.save(otpRecord);

        // Update Farmer account status to ACTIVE
        Farmer farmer = farmerRepository.findByMobileNumber(mobile)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer not found for mobile: " + mobile));
        farmer.setAccountStatus("ACTIVE");
        farmerRepository.save(farmer);

        log.info("Account activated successfully for farmer ID: {}", farmer.getFarmerId());

        return buildSessionResponse(farmer, "Mobile number verified successfully! Farmer account activated.");
    }

    /**
     * OTP verification for a freshly-registered farmer is itself an authentication
     * event, so it issues a real session token here rather than leaving the frontend
     * to land on the dashboard with no way to identify who's logged in.
     */
    private OtpResponse buildSessionResponse(Farmer farmer, String message) {
        String token = jwtService.generateToken(farmer.getFarmerId(), "FARMER", farmer.getMobileNumber(), new HashMap<>());
        return OtpResponse.builder()
                .mobileNumber(farmer.getMobileNumber())
                .message(message)
                .verified(true)
                .attemptsRemaining(0)
                .token(token)
                .farmerId(farmer.getFarmerId())
                .fullName(farmer.getFullName())
                .email(farmer.getEmail())
                .role("FARMER")
                .build();
    }

    @Transactional
    public OtpResponse resendOtp(SendOtpRequest request) {
        log.info("Resending OTP for mobile: {}", request.getMobileNumber());
        return sendOtp(request);
    }
}
