package com.agrorental.auth.service;

import com.agrorental.admin.entity.Admin;
import com.agrorental.admin.repository.AdminRepository;
import com.agrorental.auth.dto.LoginRequest;
import com.agrorental.auth.dto.LoginResponse;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.farmer.service.FarmerOtpService;
import com.agrorental.farmer.dto.VerifyOtpRequest;
import com.agrorental.partner.entity.Partner;
import com.agrorental.partner.repository.PartnerRepository;
import com.agrorental.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.agrorental.user.entity.User;
import com.agrorental.user.repository.UserRepository;
import java.util.HashMap;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final FarmerRepository farmerRepository;
    private final PartnerRepository partnerRepository;
    private final AdminRepository adminRepository;
    private final FarmerOtpService farmerOtpService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {
        String input = request.getMobileOrEmail().trim();
        log.info("Processing login request for user: {}", input);

        // 1. Try finding in UserRepository first (email account)
        Optional<User> userOpt = userRepository.findByEmail(input.toLowerCase());
        if (userOpt.isPresent()) {
            return loginUser(userOpt.get(), request);
        }

        // 2. Try finding in Admin repository (legacy email-only account)
        Optional<Admin> adminOpt = adminRepository.findByEmail(input);
        if (adminOpt.isPresent()) {
            return loginAdmin(adminOpt.get(), request);
        }

        // 3. Try finding in Farmer repository (legacy mobile/email account)
        Optional<Farmer> farmerOpt = farmerRepository.findByMobileNumberOrEmail(input, input);
        if (farmerOpt.isPresent()) {
            return loginFarmer(farmerOpt.get(), request);
        }

        // 4. Try finding in Partner repository
        Optional<Partner> partnerOpt = partnerRepository.findByMobileNumberOrEmailIgnoreCase(input, input);
        if (partnerOpt.isPresent()) {
            return loginPartner(partnerOpt.get(), request);
        }

        throw new BadRequestException("Invalid email or password.");
    }

    private LoginResponse loginUser(User user, LoginRequest request) {
        if (!user.isEnabled()) {
            log.warn("Login blocked for User ID {}: Account is disabled", user.getId());
            throw new BadRequestException("Account is disabled. Please contact customer support.");
        }

        if (request.getPassword() == null || request.getPassword().isEmpty()
                || (!passwordEncoder.matches(request.getPassword(), user.getPassword())
                    && !request.getPassword().equals(user.getPassword()))) {
            log.warn("Login failed for User ID {}: Invalid password", user.getId());
            throw new BadRequestException("Invalid email or password.");
        }

        Long farmerId = user.getFarmer() != null ? user.getFarmer().getId() : null;

        String token = jwtService.generateToken(user.getId(), user.getRole().name(), user.getEmail(), new HashMap<>());

        log.info("User authentication successful for User ID: {} ({})", user.getId(), user.getName());

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .farmerId(farmerId)
                .fullName(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .accountStatus(user.isEnabled() ? "ACTIVE" : "INACTIVE")
                .message("Login successful. Welcome back, " + user.getName() + "!")
                .build();
    }

    private LoginResponse loginAdmin(Admin admin, LoginRequest request) {
        if (!admin.isActive()) {
            log.warn("Login blocked for admin ID {}: Account is deactivated", admin.getId());
            throw new BadRequestException("Admin account is currently deactivated.");
        }

        if (request.getPassword() == null || request.getPassword().isEmpty()
                || !passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            log.warn("Login failed for admin ID {}: Invalid password provided", admin.getId());
            throw new BadRequestException("Invalid mobile/email or password.");
        }

        String token = jwtService.generateToken(admin.getId(), "ADMIN", admin.getEmail(), new HashMap<>());
        log.info("Admin login successful for admin ID: {} ({})", admin.getId(), admin.getFullName());

        return LoginResponse.builder()
                .token(token)
                .fullName(admin.getFullName())
                .email(admin.getEmail())
                .role("ADMIN")
                .accountStatus("ACTIVE")
                .message("Admin login successful. Welcome back, " + admin.getFullName() + "!")
                .build();
    }

    private LoginResponse loginFarmer(Farmer farmer, LoginRequest request) {
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

        // Generate a real signed JWT so JwtAuthenticationFilter can authenticate this farmer on later requests
        String token = jwtService.generateToken(farmer.getFarmerId(), "FARMER", farmer.getMobileNumber(), new HashMap<>());
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

    private LoginResponse loginPartner(Partner partner, LoginRequest request) {
        if (!partner.isActive()) {
            log.warn("Login blocked for partner ID {}: Account is deactivated", partner.getId());
            throw new BadRequestException("Partner account is currently deactivated. Please contact support.");
        }

        if (partner.getVerificationStatus() == Partner.VerificationStatus.PENDING) {
            log.warn("Login blocked for partner ID {}: Verification pending", partner.getId());
            throw new BadRequestException("Your account verification is currently pending admin approval. Please check back later.");
        }

        if (partner.getVerificationStatus() == Partner.VerificationStatus.REJECTED) {
            log.warn("Login blocked for partner ID {}: Verification rejected", partner.getId());
            throw new BadRequestException("Your partner account verification has been rejected. Please contact customer support.");
        }

        if (request.getPassword() == null || request.getPassword().isEmpty()
                || !passwordEncoder.matches(request.getPassword(), partner.getPassword())) {
            log.warn("Login failed for partner ID {}: Invalid password provided", partner.getId());
            throw new BadRequestException("Invalid mobile/email or password.");
        }

        String token = jwtService.generateToken(partner.getId(), "PARTNER", partner.getMobileNumber(), new HashMap<>());
        log.info("Partner login successful for partner ID: {} ({})", partner.getId(), partner.getFullName());

        return LoginResponse.builder()
                .token(token)
                .partnerId(partner.getId())
                .fullName(partner.getFullName())
                .businessName(partner.getBusinessName())
                .mobileNumber(partner.getMobileNumber())
                .email(partner.getEmail())
                .role("PARTNER")
                .accountStatus(partner.getVerificationStatus() != null ? partner.getVerificationStatus().name() : "APPROVED")
                .message("Partner login successful. Welcome back, " + partner.getFullName() + "!")
                .build();
    }
}
