package com.agrorental.auth.service;

import com.agrorental.auth.dto.LoginRequest;
import com.agrorental.auth.dto.LoginResponse;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.farmer.service.FarmerOtpService;
import com.agrorental.farmer.dto.VerifyOtpRequest;
<<<<<<< HEAD
=======
import com.agrorental.partner.entity.Partner;
import com.agrorental.partner.repository.PartnerRepository;
>>>>>>> origin/development
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

<<<<<<< HEAD
=======
import java.util.Optional;
>>>>>>> origin/development
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final FarmerRepository farmerRepository;
<<<<<<< HEAD
=======
    private final PartnerRepository partnerRepository;
>>>>>>> origin/development
    private final FarmerOtpService farmerOtpService;
    private final PasswordEncoder passwordEncoder;

    public LoginResponse login(LoginRequest request) {
        String input = request.getMobileOrEmail().trim();
        log.info("Processing login request for user: {}", input);

<<<<<<< HEAD
        // Find farmer by mobile number or email using DB query
        Farmer farmer = farmerRepository.findByMobileNumberOrEmail(input, input)
                .orElseThrow(() -> new BadRequestException("Farmer account not found with provided mobile or email: " + input));

=======
        // 1. Try finding in Farmer repository first
        Optional<Farmer> farmerOpt = farmerRepository.findByMobileNumberOrEmail(input, input);
        if (farmerOpt.isPresent()) {
            return loginFarmer(farmerOpt.get(), request);
        }

        // 2. Try finding in Partner repository
        Optional<Partner> partnerOpt = partnerRepository.findByMobileNumber(input);
        if (partnerOpt.isEmpty()) {
            partnerOpt = partnerRepository.findByEmail(input);
        }

        if (partnerOpt.isPresent()) {
            return loginPartner(partnerOpt.get(), request);
        }

        throw new BadRequestException("Account not found with provided mobile or email: " + input);
    }

    private LoginResponse loginFarmer(Farmer farmer, LoginRequest request) {
>>>>>>> origin/development
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
<<<<<<< HEAD
}

=======

    private LoginResponse loginPartner(Partner partner, LoginRequest request) {
        if (!partner.isActive()) {
            log.warn("Login blocked for partner ID {}: Account is deactivated", partner.getId());
            throw new BadRequestException("Partner account is currently deactivated. Please contact support.");
        }

        if (request.getPassword() == null || request.getPassword().isEmpty()
                || !passwordEncoder.matches(request.getPassword(), partner.getPassword())) {
            log.warn("Login failed for partner ID {}: Invalid password provided", partner.getId());
            throw new BadRequestException("Invalid mobile/email or password.");
        }

        String token = "agro-token-partner-" + partner.getId() + "-" + UUID.randomUUID().toString().substring(0, 8);
        log.info("Partner login successful for partner ID: {} ({})", partner.getId(), partner.getFullName());

        return LoginResponse.builder()
                .token(token)
                .partnerId(partner.getId())
                .fullName(partner.getFullName())
                .businessName(partner.getBusinessName())
                .mobileNumber(partner.getMobileNumber())
                .email(partner.getEmail())
                .role("PARTNER")
                .accountStatus(partner.getVerificationStatus() != null ? partner.getVerificationStatus().name() : "PENDING")
                .message("Partner login successful. Welcome back, " + partner.getFullName() + "!")
                .build();
    }
}
>>>>>>> origin/development
