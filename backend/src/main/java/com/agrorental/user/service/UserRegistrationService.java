package com.agrorental.user.service;

import com.agrorental.common.enums.Role;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.user.dto.UserRegistrationRequest;
import com.agrorental.user.dto.UserRegistrationResponse;
import com.agrorental.user.entity.EmailVerificationToken;
import com.agrorental.user.entity.User;
import com.agrorental.user.repository.EmailVerificationTokenRepository;
import com.agrorental.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Business service handling User Registration, email verification tokens, and profile linkage.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final FarmerRepository farmerRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserRegistrationResponse registerUser(UserRegistrationRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        log.info("Processing user registration request for email: {}", email);

        // 1. Check if email already exists
        if (userRepository.existsByEmail(email)) {
            log.warn("Registration rejected: Email {} is already registered", email);
            throw new BadRequestException("Email address is already registered: " + email);
        }

        // Determine user role (defaults to FARMER if unspecified)
        Role targetRole = request.getRole() != null ? request.getRole() : Role.FARMER;

        // 2. Hash raw password securely using BCrypt PasswordEncoder
        String rawPassword = request.getPassword();
        String storedPassword = processPassword(rawPassword);

        // 3. Create core User entity record
        User user = User.builder()
                .name(request.getName().trim())
                .email(email)
                .password(storedPassword)
                .role(targetRole)
                .enabled(true)
                .verified(false)
                .build();

        Long farmerId = null;

        // 4. Establish User-Farmer relationship if target role is FARMER
        if (targetRole == Role.FARMER) {
            String mobile = request.getMobileNumber() != null && !request.getMobileNumber().trim().isEmpty()
                    ? request.getMobileNumber().trim()
                    : "0000000000";

            Farmer farmer = Farmer.builder()
                    .user(user)
                    .fullName(user.getName())
                    .email(user.getEmail())
                    .password(storedPassword)
                    .mobileNumber(mobile)
                    .address(request.getAddress())
                    .preferredLanguage(request.getPreferredLanguage() != null ? request.getPreferredLanguage() : "English")
                    .accountStatus("PENDING_OTP")
                    .build();

            user.setFarmer(farmer);
        }

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {} and Role: {}", savedUser.getId(), savedUser.getRole());

        // 5. Generate Email Verification Token (valid for 24 hours)
        String tokenString = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .token(tokenString)
                .user(savedUser)
                .expiryDate(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();
        tokenRepository.save(verificationToken);

        // 6. Send Email Verification Notification
        emailService.sendVerificationEmail(savedUser.getEmail(), tokenString);

        if (savedUser.getFarmer() != null) {
            farmerId = savedUser.getFarmer().getId();
            log.info("Linked Farmer profile created with ID: {}", farmerId);
        }

        return UserRegistrationResponse.builder()
                .id(savedUser.getId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .enabled(savedUser.isEnabled())
                .verified(savedUser.isVerified())
                .farmerId(farmerId)
                .createdAt(savedUser.getCreatedAt())
                .message("User registered successfully. Verification email sent.")
                .build();
    }

    /**
     * Verifies an email verification token.
     *
     * @param token Compact UUID verification token.
     * @return UserRegistrationResponse with updated verification status.
     */
    @Transactional
    public String verifyEmailToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new BadRequestException("Verification token is required.");
        }

        Optional<EmailVerificationToken> tokenOpt = tokenRepository.findByToken(token.trim());
        if (tokenOpt.isEmpty()) {
            log.warn("Email verification failed: Token {} not found", token);
            throw new BadRequestException("Invalid or expired email verification token.");
        }

        EmailVerificationToken verificationToken = tokenOpt.get();

        if (verificationToken.isUsed() || verificationToken.getUser().isVerified()) {
            log.info("Email already verified for user: {}", verificationToken.getUser().getEmail());
            return "Email is already verified. You can log in.";
        }

        if (verificationToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            log.warn("Email verification failed: Token {} expired at {}", token, verificationToken.getExpiryDate());
            throw new BadRequestException("Invalid or expired email verification token.");
        }

        User user = verificationToken.getUser();
        user.setVerified(true);
        userRepository.save(user);

        verificationToken.setUsed(true);
        tokenRepository.save(verificationToken);

        log.info("Email verified successfully for User ID: {} ({})", user.getId(), user.getEmail());
        return "Email verified successfully! You can now log in to your account.";
    }

    private String processPassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
}
