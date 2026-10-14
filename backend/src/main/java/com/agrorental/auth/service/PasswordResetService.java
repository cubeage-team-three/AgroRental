package com.agrorental.auth.service;

import com.agrorental.auth.dto.ForgotPasswordRequest;
import com.agrorental.auth.dto.ResetPasswordRequest;
import com.agrorental.common.enums.Role;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.operator.entity.Operator;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service handling forgot password requests and secure password resets across
 * public roles: FARMER, PARTNER, and OPERATOR.
 *
 * Enforces role isolation, account domain boundary verification, rate limiting,
 * expiration, single-use token invalidation, attempt limits, and non-enumerating responses.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final FarmerRepository farmerRepository;
    private final PartnerRepository partnerRepository;
    private final OperatorRepository operatorRepository;
    private final OperatorOtpRepository operatorOtpRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    private static final int MAX_REQUESTS_PER_WINDOW = 5;
    private static final int RATE_LIMIT_WINDOW_MINUTES = 15;
    private static final int COOLDOWN_SECONDS = 30;
    private static final int MAX_VERIFICATION_ATTEMPTS = 5;
    private static final int TOKEN_EXPIRY_MINUTES = 30;

    /**
     * In-memory token metadata store for cross-role token tracking, attempt counting, and single-use validation.
     */
    private final Map<String, RecoveryTokenRecord> tokenRegistry = new ConcurrentHashMap<>();

    /**
     * Rate limit request tracking by (role + ":" + identifier).
     */
    private final Map<String, List<LocalDateTime>> requestHistory = new ConcurrentHashMap<>();

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RecoveryTokenRecord {
        private String token;
        private String role; // "FARMER", "PARTNER", "OPERATOR"
        private Long accountId;
        private String identifier;
        private LocalDateTime expiryDate;
        private boolean used;
        private int attemptCount;
        private LocalDateTime createdAt;
    }

    /**
     * Processes forgot password request for FARMER, PARTNER, or OPERATOR.
     * Always returns generic response message to prevent account enumeration attacks.
     */
    @Transactional
    public String processForgotPassword(ForgotPasswordRequest request) {
        String roleStr = request.getRole() != null && !request.getRole().isBlank()
                ? request.getRole().trim().toUpperCase()
                : "FARMER";

        // 1. Validate role against FARMER, PARTNER, and OPERATOR
        if (!"FARMER".equals(roleStr) && !"PARTNER".equals(roleStr) && !"OPERATOR".equals(roleStr)) {
            log.warn("Password recovery rejected: Invalid role '{}' specified", roleStr);
            throw new BadRequestException("Invalid role specified for password recovery. Allowed roles: FARMER, PARTNER, OPERATOR.");
        }

        String rawIdentifier = request.getAccountIdentifier();
        if (rawIdentifier == null || rawIdentifier.isBlank()) {
            throw new BadRequestException("Account identifier (email or mobile number) is required.");
        }

        String identifier = rawIdentifier.trim();
        log.info("Processing forgot password request for role: {} (identifier masked)", roleStr);

        // 2. Enforce rate limiting per role + identifier
        checkRateLimit(roleStr + ":" + identifier.toLowerCase());

        // Generic non-disclosing message
        String genericSuccessMessage = "If an account with that identifier exists, password reset instructions have been sent.";

        if ("FARMER".equals(roleStr)) {
            processFarmerForgotPassword(identifier);
        } else if ("PARTNER".equals(roleStr)) {
            processPartnerForgotPassword(identifier);
        } else if ("OPERATOR".equals(roleStr)) {
            processOperatorForgotPassword(identifier);
        }

        return genericSuccessMessage;
    }

    private void processFarmerForgotPassword(String identifier) {
        Optional<User> userOpt = Optional.empty();
        if (identifier.contains("@")) {
            userOpt = userRepository.findByEmailIgnoreCase(identifier)
                    .filter(u -> u.getRole() == Role.FARMER);
            if (userOpt.isEmpty()) {
                userOpt = farmerRepository.findByEmailIgnoreCase(identifier)
                        .map(Farmer::getUser)
                        .filter(u -> u != null && u.getRole() == Role.FARMER);
            }
        } else {
            Optional<Farmer> farmerOpt = farmerRepository.findByMobileNumber(identifier);
            if (farmerOpt.isPresent() && farmerOpt.get().getUser() != null) {
                userOpt = Optional.of(farmerOpt.get().getUser())
                        .filter(u -> u.getRole() == Role.FARMER);
            }
        }

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String tokenString = UUID.randomUUID().toString();
            LocalDateTime expiry = LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES);

            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token(tokenString)
                    .user(user)
                    .expiryDate(expiry)
                    .used(false)
                    .build();

            resetTokenRepository.save(resetToken);
            registerToken("FARMER", user.getId(), user.getEmail(), tokenString, expiry);
            emailService.sendPasswordResetEmail(user.getEmail(), tokenString, "FARMER");
            log.info("Dispatched password reset instructions for Farmer User ID: {}", user.getId());
        } else {
            log.info("Farmer forgot password requested for non-existent identifier");
        }
    }

    private void processPartnerForgotPassword(String identifier) {
        Optional<Partner> partnerOpt = Optional.empty();
        if (identifier.contains("@")) {
            partnerOpt = partnerRepository.findByEmailIgnoreCase(identifier);
        } else {
            partnerOpt = partnerRepository.findByMobileNumber(identifier);
        }

        if (partnerOpt.isPresent()) {
            Partner partner = partnerOpt.get();
            String tokenString = UUID.randomUUID().toString();
            LocalDateTime expiry = LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES);

            partner.setOtpCode(tokenString);
            partner.setOtpExpiry(expiry);
            partnerRepository.save(partner);

            registerToken("PARTNER", partner.getId(), partner.getEmail(), tokenString, expiry);

            if (partner.getEmail() != null && !partner.getEmail().isBlank()) {
                emailService.sendPasswordResetEmail(partner.getEmail(), tokenString, "PARTNER");
                log.info("Dispatched password reset instructions for Partner ID: {}", partner.getId());
            } else {
                log.warn("Partner ID {} has no email address configured for reset dispatch", partner.getId());
            }
        } else {
            log.info("Partner forgot password requested for non-existent identifier");
        }
    }

    private void processOperatorForgotPassword(String identifier) {
        String cleanMobile = cleanMobileNumber(identifier);
        Optional<Operator> operatorOpt = Optional.empty();
        if (identifier.contains("@")) {
            operatorOpt = operatorRepository.findByEmailIgnoreCase(identifier)
                    .or(() -> operatorRepository.findByEmail(identifier));
        } else {
            if (cleanMobile != null && cleanMobile.length() == 10) {
                operatorOpt = operatorRepository.findByMobileNumber(cleanMobile);
            }
            if (operatorOpt.isEmpty()) {
                operatorOpt = operatorRepository.findByMobileNumber(identifier);
            }
        }

        if (operatorOpt.isPresent()) {
            Operator operator = operatorOpt.get();
            String tokenString = UUID.randomUUID().toString();
            LocalDateTime expiry = LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES);

            registerToken("OPERATOR", operator.getId(), operator.getEmail(), tokenString, expiry);

            if (operator.getEmail() != null && !operator.getEmail().isBlank()) {
                emailService.sendPasswordResetEmail(operator.getEmail(), tokenString, "OPERATOR");
                log.info("Dispatched password reset instructions for Operator ID: {}", operator.getId());
            } else {
                log.warn("Operator ID {} has no email address configured for reset dispatch", operator.getId());
            }
        } else if (identifier.contains("@")) {
            Optional<User> userOpt = userRepository.findByEmailIgnoreCaseAndRole(identifier, Role.OPERATOR)
                    .or(() -> userRepository.findByEmailAndRole(identifier, Role.OPERATOR));
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                String tokenString = UUID.randomUUID().toString();
                LocalDateTime expiry = LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES);

                registerToken("OPERATOR", user.getId(), user.getEmail(), tokenString, expiry);
                emailService.sendPasswordResetEmail(user.getEmail(), tokenString, "OPERATOR");
                log.info("Dispatched password reset instructions for Operator User ID: {}", user.getId());
            } else {
                log.info("Operator forgot password requested for non-existent identifier");
            }
        } else {
            log.info("Operator forgot password requested for non-existent identifier");
        }
    }

    /**
     * Resets account password using a valid, non-expired recovery token or verified OTP.
     */
    @Transactional
    public String processResetPassword(ResetPasswordRequest request) {
        if (request.getNewPassword() == null || request.getConfirmPassword() == null) {
            throw new BadRequestException("New password and confirm password are required.");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirm password do not match.");
        }

        if (request.getNewPassword().length() < 6) {
            throw new BadRequestException("New password must be at least 6 characters long.");
        }

        String tokenString = request.getToken() != null ? request.getToken().trim() : "";

        // Check if token was provided
        if (tokenString.isEmpty()) {
            // Check direct OTP reset fallback
            if (request.getMobileNumber() != null && !request.getMobileNumber().isBlank()
                    && request.getOtp() != null && !request.getOtp().isBlank()) {
                return processDirectOtpPasswordReset(request);
            }
            throw new BadRequestException("Password reset token is required.");
        }

        // 1. Look up token in registry or database
        RecoveryTokenRecord tokenRecord = resolveTokenRecord(tokenString);

        if (tokenRecord == null) {
            log.warn("Password reset rejected: Token not found");
            throw new BadRequestException("Invalid or expired password reset token.");
        }

        // 2. Enforce verification attempt limits
        tokenRecord.setAttemptCount(tokenRecord.getAttemptCount() + 1);
        if (tokenRecord.getAttemptCount() > MAX_VERIFICATION_ATTEMPTS) {
            tokenRecord.setUsed(true);
            log.warn("Password reset rejected: Maximum attempts exceeded for token");
            throw new BadRequestException("Maximum verification attempts exceeded. Please request a new password reset link.");
        }

        // 3. Check if already used
        if (tokenRecord.isUsed()) {
            log.warn("Password reset rejected: Token has already been used");
            throw new BadRequestException("Invalid or expired password reset token.");
        }

        // 4. Check if expired
        if (tokenRecord.getExpiryDate().isBefore(LocalDateTime.now())) {
            log.warn("Password reset rejected: Token expired at {}", tokenRecord.getExpiryDate());
            throw new BadRequestException("Invalid or expired password reset token.");
        }

        // 5. Cross-role enforcement: If request provides explicit role, must match token role
        if (request.getRole() != null && !request.getRole().isBlank()) {
            String reqRole = request.getRole().trim().toUpperCase();
            if (!tokenRecord.getRole().equalsIgnoreCase(reqRole)) {
                log.warn("Password reset rejected: Cross-role attempt. Token role: {}, Request role: {}",
                        tokenRecord.getRole(), reqRole);
                throw new BadRequestException("Invalid or expired password reset token.");
            }
        }

        // 6. Securely hash password and update intended entity
        String hashedPassword = passwordEncoder.encode(request.getNewPassword());
        String role = tokenRecord.getRole();
        Long accountId = tokenRecord.getAccountId();

        if ("FARMER".equalsIgnoreCase(role)) {
            User user = userRepository.findById(accountId)
                    .orElseThrow(() -> new BadRequestException("Account not found for password reset."));
            user.setPassword(hashedPassword);
            if (user.getFarmer() != null) {
                Farmer farmer = user.getFarmer();
                farmer.setPassword(hashedPassword);
                farmerRepository.save(farmer);
            }
            userRepository.save(user);

            // Invalidate DB token if present
            resetTokenRepository.findByToken(tokenString).ifPresent(prt -> {
                prt.setUsed(true);
                resetTokenRepository.save(prt);
            });
            log.info("Password reset completed successfully for Farmer User ID: {}", user.getId());

        } else if ("PARTNER".equalsIgnoreCase(role)) {
            Partner partner = partnerRepository.findById(accountId)
                    .orElseThrow(() -> new BadRequestException("Partner account not found for password reset."));
            partner.setPassword(hashedPassword);
            partner.setOtpCode(null);
            partner.setOtpExpiry(null);
            // Strictly preserve verificationStatus, otpVerified, and active status
            partnerRepository.save(partner);
            log.info("Password reset completed successfully for Partner ID: {}", partner.getId());

        } else if ("OPERATOR".equalsIgnoreCase(role)) {
            Optional<Operator> operatorOpt = operatorRepository.findById(accountId);
            if (operatorOpt.isPresent()) {
                Operator operator = operatorOpt.get();
                operator.setPassword(hashedPassword);
                // Strictly preserve status, mobileVerified, and active status
                operatorRepository.save(operator);
                log.info("Password reset completed successfully for Operator ID: {}", operator.getId());
            } else {
                User user = userRepository.findById(accountId)
                        .orElseThrow(() -> new BadRequestException("Operator account not found for password reset."));
                user.setPassword(hashedPassword);
                userRepository.save(user);
                log.info("Password reset completed successfully for Operator User ID: {}", user.getId());
            }

        } else {
            throw new BadRequestException("Invalid role associated with token.");
        }

        // 7. Mark token as used
        tokenRecord.setUsed(true);

        return "Password reset successfully. You can now log in with your new password.";
    }

    private String processDirectOtpPasswordReset(ResetPasswordRequest request) {
        String roleStr = request.getRole() != null && !request.getRole().isBlank()
                ? request.getRole().trim().toUpperCase()
                : "OPERATOR";
        String mobile = request.getMobileNumber().trim();
        String cleanMobile = cleanMobileNumber(mobile);
        String searchMobile = (cleanMobile != null && cleanMobile.length() == 10) ? cleanMobile : mobile;
        String otp = request.getOtp().trim();

        if ("OPERATOR".equals(roleStr)) {
            Optional<OperatorOtp> otpOpt = operatorOtpRepository
                    .findTopByMobileNumberAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(searchMobile, OtpPurpose.PASSWORD_RESET)
                    .or(() -> operatorOtpRepository.findTopByMobileNumberAndPurposeAndVerifiedFalseOrderByCreatedAtDesc(mobile, OtpPurpose.PASSWORD_RESET));

            if (otpOpt.isEmpty() || LocalDateTime.now().isAfter(otpOpt.get().getExpiresAt())) {
                throw new BadRequestException("Invalid or expired verification code.");
            }

            OperatorOtp otpRecord = otpOpt.get();
            otpRecord.setAttemptCount(otpRecord.getAttemptCount() + 1);

            if (otpRecord.getAttemptCount() > otpRecord.getMaxAttempts()) {
                operatorOtpRepository.save(otpRecord);
                throw new BadRequestException("Maximum verification attempts exceeded. Please request a new OTP.");
            }

            if (!passwordEncoder.matches(otp, otpRecord.getOtpHash())) {
                operatorOtpRepository.save(otpRecord);
                throw new BadRequestException("Invalid verification code.");
            }

            otpRecord.setVerified(true);
            operatorOtpRepository.save(otpRecord);

            Operator operator = operatorRepository.findByMobileNumber(searchMobile)
                    .or(() -> operatorRepository.findByMobileNumber(mobile))
                    .orElseThrow(() -> new BadRequestException("Operator account not found."));
            operator.setPassword(passwordEncoder.encode(request.getNewPassword()));
            operatorRepository.save(operator);

            log.info("Password reset via OTP completed for operator ID: {}", operator.getId());
            return "Password reset successfully. You can now log in with your new password.";

        } else if ("PARTNER".equals(roleStr)) {
            Partner partner = partnerRepository.findByMobileNumber(mobile)
                    .orElseThrow(() -> new BadRequestException("Partner account not found."));

            if (partner.getOtpCode() == null || partner.getOtpExpiry() == null
                    || LocalDateTime.now().isAfter(partner.getOtpExpiry())) {
                throw new BadRequestException("Invalid or expired verification code.");
            }

            if (!partner.getOtpCode().equals(otp)) {
                throw new BadRequestException("Invalid verification code.");
            }

            partner.setPassword(passwordEncoder.encode(request.getNewPassword()));
            partner.setOtpCode(null);
            partner.setOtpExpiry(null);
            partnerRepository.save(partner);

            log.info("Password reset via OTP completed for partner ID: {}", partner.getId());
            return "Password reset successfully. You can now log in with your new password.";
        }

        throw new BadRequestException("OTP-based reset is not supported for role: " + roleStr);
    }

    private RecoveryTokenRecord resolveTokenRecord(String tokenString) {
        RecoveryTokenRecord record = tokenRegistry.get(tokenString);
        if (record != null) {
            return record;
        }

        // Fallback 1: check PasswordResetToken table for Farmer
        Optional<PasswordResetToken> prtOpt = resetTokenRepository.findByToken(tokenString);
        if (prtOpt.isPresent()) {
            PasswordResetToken prt = prtOpt.get();
            return RecoveryTokenRecord.builder()
                    .token(prt.getToken())
                    .role("FARMER")
                    .accountId(prt.getUser().getId())
                    .identifier(prt.getUser().getEmail())
                    .expiryDate(prt.getExpiryDate())
                    .used(prt.isUsed())
                    .attemptCount(0)
                    .createdAt(prt.getCreatedAt() != null ? prt.getCreatedAt() : LocalDateTime.now())
                    .build();
        }

        // Fallback 2: check Partner table for otpCode matching tokenString
        Optional<Partner> partnerOpt = partnerRepository.findByOtpCode(tokenString);
        if (partnerOpt.isPresent()) {
            Partner partner = partnerOpt.get();
            return RecoveryTokenRecord.builder()
                    .token(tokenString)
                    .role("PARTNER")
                    .accountId(partner.getId())
                    .identifier(partner.getEmail())
                    .expiryDate(partner.getOtpExpiry() != null ? partner.getOtpExpiry() : LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES))
                    .used(false)
                    .attemptCount(0)
                    .createdAt(LocalDateTime.now())
                    .build();
        }

        return null;
    }

    public String createOperatorResetToken(Long operatorId, String mobileNumber) {
        String tokenString = UUID.randomUUID().toString();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES);
        registerToken("OPERATOR", operatorId, mobileNumber, tokenString, expiry);
        return tokenString;
    }

    public void registerToken(String role, Long accountId, String identifier, String token, LocalDateTime expiry) {
        // Invalidate previous unexpired/unused tokens for this role and accountId
        tokenRegistry.values().forEach(rec -> {
            if (rec.getRole().equalsIgnoreCase(role) && rec.getAccountId().equals(accountId) && !rec.isUsed()) {
                rec.setUsed(true);
            }
        });

        RecoveryTokenRecord record = RecoveryTokenRecord.builder()
                .token(token)
                .role(role)
                .accountId(accountId)
                .identifier(identifier)
                .expiryDate(expiry)
                .used(false)
                .attemptCount(0)
                .createdAt(LocalDateTime.now())
                .build();

        tokenRegistry.put(token, record);
    }

    private void checkRateLimit(String key) {
        LocalDateTime now = LocalDateTime.now();
        List<LocalDateTime> history = requestHistory.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>());

        // Evict expired entries older than RATE_LIMIT_WINDOW_MINUTES
        history.removeIf(timestamp -> timestamp.isBefore(now.minusMinutes(RATE_LIMIT_WINDOW_MINUTES)));

        // Enforce cooldown
        if (!history.isEmpty()) {
            LocalDateTime lastRequest = history.get(history.size() - 1);
            if (lastRequest.isAfter(now.minusSeconds(COOLDOWN_SECONDS))) {
                throw new BadRequestException("Please wait " + COOLDOWN_SECONDS + " seconds before requesting another password reset.");
            }
        }

        // Enforce max requests per window
        if (history.size() >= MAX_REQUESTS_PER_WINDOW) {
            throw new BadRequestException("Too many password reset requests. Please try again after 15 minutes.");
        }

        history.add(now);
    }

    // Test support methods
    public void registerTestToken(String token, String role, Long accountId, String identifier, LocalDateTime expiry, boolean used) {
        RecoveryTokenRecord record = RecoveryTokenRecord.builder()
                .token(token)
                .role(role)
                .accountId(accountId)
                .identifier(identifier)
                .expiryDate(expiry)
                .used(used)
                .attemptCount(0)
                .createdAt(LocalDateTime.now())
                .build();
        tokenRegistry.put(token, record);
    }

    public void clearTokens() {
        tokenRegistry.clear();
        requestHistory.clear();
    }

    private String cleanMobileNumber(String input) {
        if (input == null || input.contains("@")) {
            return null;
        }
        String digits = input.replaceAll("\\D", "");
        if (digits.length() == 10) {
            return digits;
        }
        if (digits.length() == 12 && digits.startsWith("91")) {
            return digits.substring(2);
        }
        if (digits.length() == 11 && digits.startsWith("0")) {
            return digits.substring(1);
        }
        return null;
    }
}
