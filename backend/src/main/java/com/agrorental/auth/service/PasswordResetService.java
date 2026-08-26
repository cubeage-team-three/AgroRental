package com.agrorental.auth.service;

import com.agrorental.auth.dto.ForgotPasswordRequest;
import com.agrorental.auth.dto.ResetPasswordRequest;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.user.entity.PasswordResetToken;
import com.agrorental.user.entity.User;
import com.agrorental.user.repository.PasswordResetTokenRepository;
import com.agrorental.user.repository.UserRepository;
import com.agrorental.user.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service handling forgot password requests and secure password resets.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final FarmerRepository farmerRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Processes forgot password request.
     * Always returns generic response message to prevent email enumeration attacks.
     */
    @Transactional
    public String processForgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        log.info("Processing forgot password request for email: {}", email);

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String tokenString = UUID.randomUUID().toString();

            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .token(tokenString)
                    .user(user)
                    .expiryDate(LocalDateTime.now().plusMinutes(30))
                    .used(false)
                    .build();

            resetTokenRepository.save(resetToken);
            emailService.sendPasswordResetEmail(user.getEmail(), tokenString);
        } else {
            log.info("Forgot password requested for non-existent email: {}", email);
        }

        // Generic non-disclosing message
        return "If an account with that email address exists, password reset instructions have been sent.";
    }

    /**
     * Resets user password using a valid, non-expired reset token.
     */
    @Transactional
    public String processResetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirm password do not match.");
        }

        String tokenString = request.getToken().trim();
        Optional<PasswordResetToken> tokenOpt = resetTokenRepository.findByToken(tokenString);

        if (tokenOpt.isEmpty()) {
            log.warn("Password reset failed: Token not found");
            throw new BadRequestException("Invalid or expired password reset token.");
        }

        PasswordResetToken resetToken = tokenOpt.get();

        if (resetToken.isUsed()) {
            log.warn("Password reset failed: Token has already been used");
            throw new BadRequestException("Invalid or expired password reset token.");
        }

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            log.warn("Password reset failed: Token expired at {}", resetToken.getExpiryDate());
            throw new BadRequestException("Invalid or expired password reset token.");
        }

        User user = resetToken.getUser();
        String hashedPassword = passwordEncoder.encode(request.getNewPassword());
        user.setPassword(hashedPassword);

        // If user has linked Farmer record, sync encoded password
        if (user.getFarmer() != null) {
            Farmer farmer = user.getFarmer();
            farmer.setPassword(hashedPassword);
            farmerRepository.save(farmer);
        }

        userRepository.save(user);

        resetToken.setUsed(true);
        resetTokenRepository.save(resetToken);

        log.info("Password reset successfully completed for User ID: {} ({})", user.getId(), user.getEmail());
        return "Password reset successfully. You can now log in with your new password.";
    }
}
