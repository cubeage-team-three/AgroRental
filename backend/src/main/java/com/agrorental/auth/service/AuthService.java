package com.agrorental.auth.service;

import com.agrorental.auth.dto.LoginRequest;
import com.agrorental.auth.dto.LoginResponse;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.farmer.service.FarmerOtpService;
import com.agrorental.farmer.dto.VerifyOtpRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final FarmerRepository farmerRepository;
    private final FarmerOtpService farmerOtpService;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(LoginRequest request) {
        String input = request.getMobileOrEmail().trim();
        log.info("Processing login request for user: {}", input);

        // Find farmer by mobile number or email using DB query
        Farmer farmer = farmerRepository.findByMobileNumberOrEmail(input, input)
                .orElseThrow(() -> new BadRequestException("Farmer account not found with provided mobile or email: " + input));

        // Rule: Check Account Status
        if ("PENDING_OTP".equalsIgnoreCase(farmer.getAccountStatus())) {
            log.warn("Login blocked for farmer ID {}: Account is pending OTP verification", farmer.getFarmerId());
            throw new BadRequestException("PENDING_OTP: Account is not activated yet. Please complete OTP verification first.");
        }

        if ("INACTIVE".equalsIgnoreCase(farmer.getAccountStatus())) {
            log.warn("Login blocked for farmer ID {}: Account is INACTIVE", farmer.getFarmerId());
            throw new BadRequestException("Account has been deactivated. Please contact customer support.");
        }

        // Rule: Password vs OTP authentication mode
        if ("OTP".equalsIgnoreCase(request.getLoginType())) {
            if (request.getOtp() == null || request.getOtp().trim().isEmpty()) {
                throw new BadRequestException("OTP code is required for OTP login mode.");
            }
            // Verify OTP using FarmerOtpService
            farmerOtpService.verifyOtp(VerifyOtpRequest.builder()
                    .mobileNumber(farmer.getMobileNumber())
                    .otp(request.getOtp().trim())
                    .build());
        } else {
            // Password login mode
            if (farmer.getPassword() == null || farmer.getPassword().trim().isEmpty()) {
                log.warn("Login failed for farmer ID {}: No password set on account", farmer.getFarmerId());
                throw new BadRequestException("No password has been set for this account. Please log in using OTP quick login.");
            }

            if (request.getPassword() == null || request.getPassword().isEmpty()
                    || !passwordEncoder.matches(request.getPassword(), farmer.getPassword())) {
                log.warn("Login failed for farmer ID {}: Invalid password provided", farmer.getFarmerId());
                throw new BadRequestException("Invalid mobile/email or password.");
            }
        }

        // Generate session bearer token
        String token = "agro-token-" + farmer.getFarmerId() + "-" + UUID.randomUUID().toString().substring(0, 8);
        log.info("Login successful for farmer ID: {} ({})", farmer.getFarmerId(), farmer.getFullName());

        return LoginResponse.builder()
                .token(token)
                .farmerId(farmer.getFarmerId())
                .fullName(farmer.getFullName())
                .mobileNumber(farmer.getMobileNumber())
                .email(farmer.getEmail())
                .preferredLanguage(farmer.getPreferredLanguage())
                .role("FARMER")
                .accountStatus(farmer.getAccountStatus())
                .message("Login successful. Welcome back, " + farmer.getFullName() + "!")
                .build();
    }
}

