package com.agrorental.farmer.service;

import com.agrorental.auth.service.AccountIdentifiers;
import com.agrorental.common.config.DeploymentProfiles;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.farmer.dto.OtpResponse;
import com.agrorental.farmer.dto.SendOtpRequest;
import com.agrorental.farmer.dto.VerifyOtpRequest;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.entity.FarmerOtp;
import com.agrorental.farmer.repository.FarmerOtpRepository;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.operator.otp.OtpDeliveryService;
import com.agrorental.operator.otp.OtpPurpose;
import com.agrorental.security.jwt.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

/**
 * Farmer mobile OTP (send / verify / resend). OTP is no longer part of farmer registration or login:
 * verification never issues a session token. Verifying still activates a legacy PENDING_OTP account, which
 * is harmless because PENDING_OTP no longer blocks password login.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FarmerOtpService {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final byte[] OTP_HASH_KEY_CONTEXT = "agrorent:farmer-otp:v1".getBytes(StandardCharsets.UTF_8);
    // 7 bytes encode to exactly 10 Base64url characters, matching farmer_otp.otp VARCHAR(10)
    private static final int OTP_HASH_BYTES = 7;

    private final FarmerOtpRepository farmerOtpRepository;
    private final FarmerRepository farmerRepository;
    private final JwtProperties jwtProperties;
    private final OtpDeliveryService otpDeliveryService;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("${app.otp.expiry-minutes:5}")
    private int otpExpiryMinutes = 5;

    @Value("${app.otp.max-attempts:3}")
    private int maxAttempts = 3;

    @Value("${app.otp.resend-cooldown-seconds:30}")
    private int resendCooldownSeconds = 30;

    @Value("${app.otp.max-sends-per-hour:5}")
    private int maxSendsPerHour = 5;

    @Value("${app.otp.include-dev-mock:false}")
    private boolean includeDevMock = false;

    @Autowired(required = false)
    private Environment environment;

    @Transactional
    public OtpResponse sendOtp(SendOtpRequest request) {
        String mobile = request.getMobileNumber().trim();
        log.info("Sending OTP for mobile: {}", AccountIdentifiers.mask(mobile));

        Farmer farmer = farmerRepository.findByMobileNumber(mobile)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer not found with mobile number: " + mobile));

        // Throttle OTP requests per mobile number (persisted, so it holds across restarts and instances)
        LocalDateTime now = LocalDateTime.now();
        Optional<FarmerOtp> latestOtp = farmerOtpRepository.findTopByMobileNumberOrderByCreatedAtDesc(mobile);
        if (latestOtp.isPresent() && now.isBefore(latestOtp.get().getCreatedAt().plusSeconds(resendCooldownSeconds))) {
            throw new BadRequestException("Please wait " + resendCooldownSeconds + " seconds before requesting a new OTP.");
        }
        if (farmerOtpRepository.countByMobileNumberAndCreatedAtAfter(mobile, now.minusHours(1)) >= maxSendsPerHour) {
            log.warn("OTP request limit reached for mobile: {}", AccountIdentifiers.mask(mobile));
            throw new BadRequestException("Too many OTP requests. Please try again later.");
        }

        String generatedOtp = String.format("%06d", RANDOM.nextInt(900000) + 100000);

        FarmerOtp otpRecord = FarmerOtp.builder()
                .farmerId(farmer.getFarmerId())
                .mobileNumber(mobile)
                .otpHash(hashOtp(mobile, generatedOtp))
                .expiresAt(now.plusMinutes(otpExpiryMinutes))
                .attemptCount(0)
                .verified(false)
                .build();

        farmerOtpRepository.save(otpRecord);
        log.info("Generated OTP for mobile {} (Expires at: {})", AccountIdentifiers.mask(mobile), otpRecord.getExpiresAt());

        otpDeliveryService.deliverOtp(mobile, generatedOtp, OtpPurpose.MOBILE_VERIFICATION);

        return OtpResponse.builder()
                .mobileNumber(mobile)
                .message("OTP sent successfully to +91 " + mobile + ". Valid for " + otpExpiryMinutes + " minutes.")
                .verified(false)
                .attemptsRemaining(maxAttempts)
                .expiresAt(otpRecord.getExpiresAt())
                .devMockOtp(isDevMockOtpAllowed() ? generatedOtp : null)
                .build();
    }

    /**
     * Failed attempts must persist even though a BadRequestException is returned to the caller,
     * so validation failures do not roll back the attempt counter.
     */
    @Transactional(noRollbackFor = BadRequestException.class)
    public OtpResponse verifyOtp(VerifyOtpRequest request) {
        String mobile = request.getMobileNumber().trim();
        String submittedOtp = request.getOtp().trim();
        log.info("Verifying OTP for mobile: {}", AccountIdentifiers.mask(mobile));

        FarmerOtp otpRecord = farmerOtpRepository.findTopByMobileNumberOrderByCreatedAtDesc(mobile)
                .orElseThrow(() -> new BadRequestException(
                        "No active OTP request found for this mobile number. Please request a new OTP."));

        // Rule 1: Single use
        if (Boolean.TRUE.equals(otpRecord.getVerified())) {
            log.warn("Attempt to reuse an already verified OTP for mobile: {}", AccountIdentifiers.mask(mobile));
            throw new BadRequestException("OTP has already been used. Please request a new OTP.");
        }

        // Rule 2: Check Expiry
        if (LocalDateTime.now().isAfter(otpRecord.getExpiresAt())) {
            log.warn("OTP expired for mobile {}. Expiration was: {}", AccountIdentifiers.mask(mobile), otpRecord.getExpiresAt());
            throw new BadRequestException("OTP has expired. Please click 'Resend OTP' to receive a new code.");
        }

        // Rule 3: Consume one attempt atomically before comparing (lockout after maxAttempts)
        if (farmerOtpRepository.incrementAttemptCountIfBelowLimit(otpRecord.getId(), maxAttempts) == 0) {
            log.warn("Maximum OTP attempts exceeded for mobile: {}", AccountIdentifiers.mask(mobile));
            throw new BadRequestException("Maximum OTP attempts exceeded (" + maxAttempts + "/" + maxAttempts
                    + "). Please request a new OTP.");
        }

        // Rule 4: Match OTP in constant time
        if (!otpMatches(mobile, submittedOtp, otpRecord.getOtpHash())) {
            int remaining = Math.max(0, maxAttempts - (otpRecord.getAttemptCount() + 1));
            log.warn("Incorrect OTP entered for mobile {}. Attempts remaining: {}", AccountIdentifiers.mask(mobile), remaining);

            if (remaining == 0) {
                throw new BadRequestException(
                        "Invalid OTP. Maximum attempts exceeded (" + maxAttempts + "/" + maxAttempts
                                + "). Please request a new OTP.");
            } else {
                throw new BadRequestException("Invalid OTP code. You have " + remaining + " attempt(s) remaining.");
            }
        }

        if (farmerOtpRepository.markVerifiedIfUnused(otpRecord.getId()) == 0) {
            throw new BadRequestException("OTP has already been used. Please request a new OTP.");
        }

        Farmer farmer = farmerRepository.findByMobileNumber(mobile)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer not found for mobile: " + mobile));

        if ("INACTIVE".equalsIgnoreCase(farmer.getAccountStatus())) {
            log.warn("OTP verified but farmer ID {} is INACTIVE; no session issued", farmer.getFarmerId());
            throw new BadRequestException("Account has been deactivated. Please contact customer support.");
        }

        // Only a pending registration is activated here; other statuses (e.g. admin-set) are left unchanged
        if ("PENDING_OTP".equalsIgnoreCase(farmer.getAccountStatus())) {
            farmer.setAccountStatus("ACTIVE");
            farmerRepository.save(farmer);
            log.info("Account activated successfully for farmer ID: {}", farmer.getFarmerId());
            return verifiedResponse(farmer, "Mobile number verified successfully! Farmer account activated. Please log in with your password.");
        }

        log.info("OTP verified for farmer ID: {}", farmer.getFarmerId());
        return verifiedResponse(farmer, "Mobile number verified successfully!");
    }

    /** Verification result only: an OTP never yields a session token (farmers log in with their password). */
    private OtpResponse verifiedResponse(Farmer farmer, String message) {
        return OtpResponse.builder()
                .mobileNumber(farmer.getMobileNumber())
                .message(message)
                .verified(true)
                .attemptsRemaining(0)
                .build();
    }

    @Transactional
    public OtpResponse resendOtp(SendOtpRequest request) {
        log.info("Resending OTP for mobile: {}", AccountIdentifiers.mask(request.getMobileNumber()));
        return sendOtp(request);
    }

    /**
     * Keyed one-way hash of the OTP, bound to the mobile number. The key is derived from the
     * application's JWT secret, so a database leak alone does not reveal or allow brute-forcing OTPs.
     */
    String hashOtp(String mobile, String otp) {
        try {
            Mac keyDerivation = Mac.getInstance(HMAC_SHA256);
            keyDerivation.init(new SecretKeySpec(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] otpKey = keyDerivation.doFinal(OTP_HASH_KEY_CONTEXT);

            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(otpKey, HMAC_SHA256));
            byte[] digest = mac.doFinal((mobile + ":" + otp).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(Arrays.copyOf(digest, OTP_HASH_BYTES));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to hash OTP", e);
        }
    }

    private boolean otpMatches(String mobile, String submittedOtp, String storedHash) {
        if (storedHash == null) {
            return false;
        }
        return MessageDigest.isEqual(
                hashOtp(mobile, submittedOtp).getBytes(StandardCharsets.US_ASCII),
                storedHash.getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * Mock OTP disclosure requires the explicit opt-in property AND an explicitly active "dev" profile;
     * never in staging, production or an implicit default profile. Mirrors OperatorOtpService.
     */
    public boolean isDevMockOtpAllowed() {
        return DeploymentProfiles.isExplicitDevelopment(environment) && includeDevMock;
    }

    public void setIncludeDevMock(boolean includeDevMock) {
        this.includeDevMock = includeDevMock;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}
