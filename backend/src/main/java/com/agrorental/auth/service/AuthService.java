package com.agrorental.auth.service;

import com.agrorental.admin.entity.Admin;
import com.agrorental.admin.repository.AdminRepository;
import com.agrorental.auth.dto.LoginRequest;
import com.agrorental.auth.dto.LoginResponse;
import com.agrorental.common.enums.Role;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.ForbiddenException;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.farmer.service.FarmerOtpService;
import com.agrorental.farmer.dto.VerifyOtpRequest;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.repository.OperatorRepository;
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
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Set<String> ALLOWED_ROLES = Set.of("FARMER", "PARTNER", "OPERATOR", "ADMIN");

    private final UserRepository userRepository;
    private final FarmerRepository farmerRepository;
    private final PartnerRepository partnerRepository;
    private final AdminRepository adminRepository;
    private final OperatorRepository operatorRepository;
    private final FarmerOtpService farmerOtpService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {
        String input = request.getMobileOrEmail().trim();
        log.info("Processing login request for user: {}", input);

        String requestedRole = null;
        if (request.getRole() != null && !request.getRole().trim().isEmpty()) {
            requestedRole = request.getRole().trim().toUpperCase();
            if (!ALLOWED_ROLES.contains(requestedRole)) {
                log.warn("Login rejected: Invalid role '{}' specified", request.getRole());
                throw new BadRequestException("Invalid role specified. Allowed roles are: FARMER, PARTNER, OPERATOR, ADMIN.");
            }
        }

        // When an explicit role is specified, resolve exclusively against the requested role model
        if (requestedRole != null) {
            return loginWithExplicitRole(requestedRole, input, request);
        }

        // Backward compatibility fallback: ordered search across repositories
        return loginWithLegacyFallback(input, request);
    }

    private LoginResponse loginWithExplicitRole(String role, String input, LoginRequest request) {
        switch (role) {
            case "FARMER":
                // 1. Core User with FARMER role
                Optional<User> farmerUserOpt = userRepository.findByEmailAndRole(input, Role.FARMER)
                        .or(() -> userRepository.findByEmailIgnoreCaseAndRole(input, Role.FARMER));
                if (farmerUserOpt.isPresent()) {
                    return loginUser(farmerUserOpt.get(), request);
                }
                // 2. Farmer profile by mobile or email
                String cleanFarmerMobile = cleanMobileNumber(input);
                Optional<Farmer> farmerOpt = farmerRepository.findByMobileNumberOrEmail(input, input)
                        .or(() -> farmerRepository.findByMobileNumberOrEmailIgnoreCase(input, input));
                if (farmerOpt.isEmpty() && cleanFarmerMobile != null) {
                    farmerOpt = farmerRepository.findByMobileNumber(cleanFarmerMobile)
                            .or(() -> farmerRepository.findByMobileNumberOrEmailIgnoreCase(cleanFarmerMobile, input));
                }
                if (farmerOpt.isPresent()) {
                    return loginFarmer(farmerOpt.get(), request);
                }
                throw new BadRequestException("Invalid email or password.");

            case "PARTNER":
                if ("OTP".equalsIgnoreCase(request.getLoginType())) {
                    throw new BadRequestException("OTP login is only supported for Farmer accounts. Please use password login.");
                }
                // 1. Partner profile by mobile or email
                String cleanPartnerMobile = cleanMobileNumber(input);
                Optional<Partner> partnerOpt = partnerRepository.findByMobileNumberOrEmail(input, input)
                        .or(() -> partnerRepository.findByMobileNumberOrEmailIgnoreCase(input, input));
                if (partnerOpt.isEmpty() && cleanPartnerMobile != null) {
                    partnerOpt = partnerRepository.findByMobileNumber(cleanPartnerMobile)
                            .or(() -> partnerRepository.findByMobileNumberOrEmailIgnoreCase(cleanPartnerMobile, input));
                }
                if (partnerOpt.isPresent()) {
                    return loginPartner(partnerOpt.get(), request);
                }
                // 2. Core User with PARTNER role
                Optional<User> partnerUserOpt = userRepository.findByEmailAndRole(input, Role.PARTNER)
                        .or(() -> userRepository.findByEmailIgnoreCaseAndRole(input, Role.PARTNER));
                if (partnerUserOpt.isPresent()) {
                    return loginUser(partnerUserOpt.get(), request);
                }
                throw new BadRequestException("Invalid email or password.");

            case "OPERATOR":
                if ("OTP".equalsIgnoreCase(request.getLoginType())) {
                    throw new BadRequestException("OTP login is only supported for Farmer accounts. Please use password login.");
                }
                String cleanMobile = cleanMobileNumber(input);
                Optional<Operator> operatorOpt = operatorRepository.findByMobileNumberOrEmailIgnoreCase(input, input);
                if (operatorOpt.isEmpty() && cleanMobile != null) {
                    operatorOpt = operatorRepository.findByMobileNumber(cleanMobile)
                            .or(() -> operatorRepository.findByMobileNumberOrEmailIgnoreCase(cleanMobile, input));
                }
                if (operatorOpt.isPresent()) {
                    return loginOperator(operatorOpt.get(), request);
                }
                // Check if user is registered in UserRepository with Role.OPERATOR
                Optional<User> operatorUserOpt = userRepository.findByEmailAndRole(input, Role.OPERATOR)
                        .or(() -> userRepository.findByEmailIgnoreCaseAndRole(input, Role.OPERATOR));
                if (operatorUserOpt.isPresent()) {
                    return loginUser(operatorUserOpt.get(), request);
                }
                throw new BadRequestException("Invalid email or password.");

            case "ADMIN":
                if ("OTP".equalsIgnoreCase(request.getLoginType())) {
                    throw new BadRequestException("OTP login is only supported for Farmer accounts. Please use password login.");
                }
                // 1. Admin repository by email
                Optional<Admin> adminOpt = adminRepository.findByEmail(input)
                        .or(() -> adminRepository.findByEmailIgnoreCase(input));
                if (adminOpt.isPresent()) {
                    return loginAdmin(adminOpt.get(), request);
                }
                // 2. Core User with ADMIN role
                Optional<User> adminUserOpt = userRepository.findByEmailAndRole(input, Role.ADMIN)
                        .or(() -> userRepository.findByEmailIgnoreCaseAndRole(input, Role.ADMIN));
                if (adminUserOpt.isPresent()) {
                    return loginUser(adminUserOpt.get(), request);
                }
                throw new BadRequestException("Invalid email or password.");

            default:
                throw new BadRequestException("Invalid role specified. Allowed roles are: FARMER, PARTNER, OPERATOR, ADMIN.");
        }
    }

    private LoginResponse loginWithLegacyFallback(String input, LoginRequest request) {
        // 1. Try finding in UserRepository first (email account)
        Optional<User> userOpt = userRepository.findByEmail(input)
                .or(() -> userRepository.findByEmailIgnoreCase(input));
        if (userOpt.isPresent()) {
            return loginUser(userOpt.get(), request);
        }

        // 2. Try finding in Admin repository
        Optional<Admin> adminOpt = adminRepository.findByEmail(input)
                .or(() -> adminRepository.findByEmailIgnoreCase(input));
        if (adminOpt.isPresent()) {
            return loginAdmin(adminOpt.get(), request);
        }

        // 3. Try finding in Farmer repository
        Optional<Farmer> farmerOpt = farmerRepository.findByMobileNumberOrEmail(input, input)
                .or(() -> farmerRepository.findByMobileNumberOrEmailIgnoreCase(input, input));
        if (farmerOpt.isPresent()) {
            return loginFarmer(farmerOpt.get(), request);
        }

        // 4. Try finding in Partner repository
        Optional<Partner> partnerOpt = partnerRepository.findByMobileNumberOrEmail(input, input)
                .or(() -> partnerRepository.findByMobileNumberOrEmailIgnoreCase(input, input));
        if (partnerOpt.isPresent()) {
            return loginPartner(partnerOpt.get(), request);
        }

        // 5. Try finding in Operator repository
        String cleanMobile = cleanMobileNumber(input);
        Optional<Operator> operatorOpt = operatorRepository.findByMobileNumberOrEmailIgnoreCase(input, input);
        if (operatorOpt.isEmpty() && cleanMobile != null) {
            operatorOpt = operatorRepository.findByMobileNumber(cleanMobile)
                    .or(() -> operatorRepository.findByMobileNumberOrEmailIgnoreCase(cleanMobile, input));
        }
        if (operatorOpt.isPresent()) {
            return loginOperator(operatorOpt.get(), request);
        }

        throw new BadRequestException("Invalid email or password.");
    }

    private LoginResponse loginUser(User user, LoginRequest request) {
        if (!user.isEnabled()) {
            log.warn("Login blocked for User ID {}: Account is disabled", user.getId());
            throw new BadRequestException("Account is disabled. Please contact customer support.");
        }

        if (user.getRole() == Role.FARMER && user.getFarmer() != null) {
            Farmer farmer = user.getFarmer();
            if ("PENDING_OTP".equalsIgnoreCase(farmer.getAccountStatus())) {
                log.warn("Login blocked for Farmer User ID {}: Account is pending OTP verification", user.getId());
                throw new BadRequestException("PENDING_OTP: Account is not activated yet. Please complete OTP verification first.");
            }
            if ("INACTIVE".equalsIgnoreCase(farmer.getAccountStatus())) {
                log.warn("Login blocked for Farmer User ID {}: Account is INACTIVE", user.getId());
                throw new BadRequestException("Account has been deactivated. Please contact customer support.");
            }
        }

        if (!verifyUserPassword(request.getPassword(), user)) {
            log.warn("Login failed for User ID {}: Invalid password", user.getId());
            throw new BadRequestException("Invalid email or password.");
        }

        Long farmerId = user.getFarmer() != null ? user.getFarmer().getId() : null;
        String mobileNumber = user.getFarmer() != null ? user.getFarmer().getMobileNumber() : null;
        String preferredLanguage = user.getFarmer() != null ? user.getFarmer().getPreferredLanguage() : null;

        String token = jwtService.generateToken(user.getId(), user.getRole().name(), user.getEmail(), new HashMap<>());

        log.info("User authentication successful for User ID: {} ({})", user.getId(), user.getName());

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .farmerId(farmerId)
                .fullName(user.getName())
                .mobileNumber(mobileNumber)
                .preferredLanguage(preferredLanguage)
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

            if (!verifyFarmerPassword(request.getPassword(), farmer)) {
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

        if (!verifyPartnerPassword(request.getPassword(), partner)) {
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

    private LoginResponse loginOperator(Operator operator, LoginRequest request) {
        if (!operator.isActive()) {
            log.warn("Login blocked for operator ID {}: Account is inactive", operator.getId());
            throw new BadRequestException("Operator account is inactive. Please contact customer support.");
        }

        if (!operator.isMobileVerified()) {
            log.warn("Login blocked for operator ID {}: Mobile is unverified", operator.getId());
            throw new BadRequestException("Mobile number is not verified. Please verify mobile before logging in.");
        }

        if (operator.getStatus() == OperatorStatus.PENDING) {
            log.warn("Login blocked for operator ID {}: Account is pending admin approval", operator.getId());
            throw new BadRequestException("Operator account is pending admin approval.");
        }

        if (operator.getStatus() == OperatorStatus.REJECTED) {
            log.warn("Login blocked for operator ID {}: Account is rejected", operator.getId());
            String reasonSuffix = (operator.getRejectionReason() != null && !operator.getRejectionReason().isBlank())
                    ? ": " + operator.getRejectionReason()
                    : "";
            throw new BadRequestException("Operator account has been rejected" + reasonSuffix);
        }

        if (operator.getStatus() != OperatorStatus.APPROVED) {
            log.warn("Login blocked for operator ID {}: Status is {}", operator.getId(), operator.getStatus());
            throw new BadRequestException("Operator account is not approved: " + operator.getStatus());
        }

        if (!verifyOperatorPassword(request.getPassword(), operator)) {
            log.warn("Login failed for operator ID {}: Invalid password provided", operator.getId());
            throw new BadRequestException("Invalid mobile/email or password.");
        }

        String token = jwtService.generateOperatorToken(operator.getId(), operator.getMobileNumber());
        log.info("Operator login successful for operator ID: {} ({})", operator.getId(), operator.getFullName());

        return LoginResponse.builder()
                .token(token)
                .operatorId(operator.getId())
                .fullName(operator.getFullName())
                .mobileNumber(operator.getMobileNumber())
                .email(operator.getEmail())
                .role("OPERATOR")
                .accountStatus(operator.getStatus() != null ? operator.getStatus().name() : "APPROVED")
                .message("Operator login successful. Welcome back, " + operator.getFullName() + "!")
                .build();
    }

    private static final java.util.regex.Pattern BCRYPT_PATTERN =
            java.util.regex.Pattern.compile("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");

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

    private boolean verifyOperatorPassword(String rawPassword, Operator operator) {
        if (rawPassword == null || rawPassword.isEmpty() || operator.getPassword() == null) {
            return false;
        }

        String storedPassword = operator.getPassword();

        // 1. Primary check: Use BCrypt PasswordEncoder
        if (passwordEncoder.matches(rawPassword, storedPassword)) {
            return true;
        }

        // 1b. Check trimmed password if leading/trailing whitespace was accidentally passed
        if (!rawPassword.equals(rawPassword.trim()) && passwordEncoder.matches(rawPassword.trim(), storedPassword)) {
            return true;
        }

        // 2. Safe legacy check: ONLY if stored password is NOT a BCrypt hash
        if (!BCRYPT_PATTERN.matcher(storedPassword).matches()) {
            boolean legacyMatches = java.security.MessageDigest.isEqual(
                    storedPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    rawPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8)
            ) || (!rawPassword.equals(rawPassword.trim()) && java.security.MessageDigest.isEqual(
                    storedPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    rawPassword.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8)
            ));

            if (legacyMatches) {
                log.info("Safely upgrading legacy unhashed password to BCrypt hash for Operator ID: {}", operator.getId());
                operator.setPassword(passwordEncoder.encode(rawPassword.trim()));
                operatorRepository.save(operator);
                return true;
            }
        }

        return false;
    }

    private boolean verifyUserPassword(String rawPassword, User user) {
        if (rawPassword == null || rawPassword.isEmpty() || user.getPassword() == null) {
            return false;
        }

        String storedPassword = user.getPassword();

        if (passwordEncoder.matches(rawPassword, storedPassword)) {
            return true;
        }

        if (!rawPassword.equals(rawPassword.trim()) && passwordEncoder.matches(rawPassword.trim(), storedPassword)) {
            return true;
        }

        if (!BCRYPT_PATTERN.matcher(storedPassword).matches()) {
            boolean legacyMatches = java.security.MessageDigest.isEqual(
                    storedPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    rawPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8)
            ) || (!rawPassword.equals(rawPassword.trim()) && java.security.MessageDigest.isEqual(
                    storedPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    rawPassword.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8)
            ));

            if (legacyMatches) {
                log.info("Safely upgrading legacy unhashed password to BCrypt hash for User ID: {}", user.getId());
                user.setPassword(passwordEncoder.encode(rawPassword.trim()));
                userRepository.save(user);
                return true;
            }
        }

        return false;
    }

    private boolean verifyFarmerPassword(String rawPassword, Farmer farmer) {
        if (rawPassword == null || rawPassword.isEmpty() || farmer.getPassword() == null) {
            return false;
        }

        String storedPassword = farmer.getPassword();

        if (passwordEncoder.matches(rawPassword, storedPassword)) {
            return true;
        }

        if (!rawPassword.equals(rawPassword.trim()) && passwordEncoder.matches(rawPassword.trim(), storedPassword)) {
            return true;
        }

        if (!BCRYPT_PATTERN.matcher(storedPassword).matches()) {
            boolean legacyMatches = java.security.MessageDigest.isEqual(
                    storedPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    rawPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8)
            ) || (!rawPassword.equals(rawPassword.trim()) && java.security.MessageDigest.isEqual(
                    storedPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    rawPassword.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8)
            ));

            if (legacyMatches) {
                log.info("Safely upgrading legacy unhashed password to BCrypt hash for Farmer ID: {}", farmer.getFarmerId());
                farmer.setPassword(passwordEncoder.encode(rawPassword.trim()));
                farmerRepository.save(farmer);
                return true;
            }
        }

        return false;
    }

    private boolean verifyPartnerPassword(String rawPassword, Partner partner) {
        if (rawPassword == null || rawPassword.isEmpty() || partner.getPassword() == null) {
            return false;
        }

        String storedPassword = partner.getPassword();

        if (passwordEncoder.matches(rawPassword, storedPassword)) {
            return true;
        }

        if (!rawPassword.equals(rawPassword.trim()) && passwordEncoder.matches(rawPassword.trim(), storedPassword)) {
            return true;
        }

        if (!BCRYPT_PATTERN.matcher(storedPassword).matches()) {
            boolean legacyMatches = java.security.MessageDigest.isEqual(
                    storedPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    rawPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8)
            ) || (!rawPassword.equals(rawPassword.trim()) && java.security.MessageDigest.isEqual(
                    storedPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    rawPassword.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8)
            ));

            if (legacyMatches) {
                log.info("Safely upgrading legacy unhashed password to BCrypt hash for Partner ID: {}", partner.getId());
                partner.setPassword(passwordEncoder.encode(rawPassword.trim()));
                partnerRepository.save(partner);
                return true;
            }
        }

        return false;
    }
}
