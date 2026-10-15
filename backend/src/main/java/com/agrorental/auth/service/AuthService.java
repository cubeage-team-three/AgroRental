package com.agrorental.auth.service;

import com.agrorental.admin.entity.Admin;
import com.agrorental.admin.repository.AdminRepository;
import com.agrorental.auth.dto.LoginRequest;
import com.agrorental.auth.dto.LoginResponse;
import com.agrorental.common.enums.Role;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.partner.entity.Partner;
import com.agrorental.partner.repository.PartnerRepository;
import com.agrorental.security.jwt.AccountType;
import com.agrorental.security.jwt.JwtService;
import com.agrorental.user.entity.User;
import com.agrorental.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Password login for every account type: {@code {mobileOrEmail, password, role}}.
 *
 * <ul>
 *   <li>Unknown accounts, accounts without a password and wrong passwords all fail with the same
 *       {@link #INVALID_CREDENTIALS_MESSAGE} (a dummy BCrypt check keeps the timing comparable).</li>
 *   <li>Approval, deactivation and rejection messages are only returned after the password was verified.</li>
 *   <li>OTP login is disabled ({@link #OTP_LOGIN_DISABLED_MESSAGE}).</li>
 *   <li>Each role authenticates only against its own account table and receives a token whose account-type
 *       claim names that table.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    public static final String INVALID_CREDENTIALS_MESSAGE = "Invalid mobile/email or password.";
    public static final String OTP_LOGIN_DISABLED_MESSAGE = "OTP login is no longer supported. Please use your password.";
    public static final String INVALID_ROLE_MESSAGE = "Invalid role specified. Allowed roles are: FARMER, PARTNER, OPERATOR, ADMIN.";

    static final String FARMER_DISABLED_MESSAGE = "Account is disabled. Please contact customer support.";
    static final String FARMER_DEACTIVATED_MESSAGE = "Account has been deactivated. Please contact customer support.";
    static final String PARTNER_DEACTIVATED_MESSAGE = "Partner account is currently deactivated. Please contact support.";
    static final String PARTNER_PENDING_MESSAGE = "Your account verification is currently pending admin approval. Please check back later.";
    static final String PARTNER_REJECTED_MESSAGE = "Your partner account verification has been rejected. Please contact customer support.";
    static final String OPERATOR_INACTIVE_MESSAGE = "Operator account is inactive. Please contact customer support.";
    static final String OPERATOR_PENDING_MESSAGE = "Operator account is pending admin approval.";
    static final String ADMIN_DEACTIVATED_MESSAGE = "Admin account is currently deactivated.";

    private static final Set<String> ALLOWED_ROLES = Set.of("FARMER", "PARTNER", "OPERATOR", "ADMIN");
    private static final Pattern BCRYPT_PATTERN = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");
    /** BCrypt hash of a random throwaway value: verifying against it costs the same as a real check. */
    private static final String DUMMY_PASSWORD_HASH = new BCryptPasswordEncoder().encode(UUID.randomUUID().toString());

    private final UserRepository userRepository;
    private final FarmerRepository farmerRepository;
    private final PartnerRepository partnerRepository;
    private final AdminRepository adminRepository;
    private final OperatorRepository operatorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginResponse login(LoginRequest request) {
        if (request.getLoginType() != null && "OTP".equalsIgnoreCase(request.getLoginType().trim())) {
            throw new BadRequestException(OTP_LOGIN_DISABLED_MESSAGE);
        }
        String input = request.getMobileOrEmail() == null ? "" : request.getMobileOrEmail().trim();
        if (input.isEmpty()) {
            throw new BadRequestException(INVALID_CREDENTIALS_MESSAGE);
        }

        if (request.getRole() != null && !request.getRole().trim().isEmpty()) {
            String requestedRole = request.getRole().trim().toUpperCase(Locale.ROOT);
            if (!ALLOWED_ROLES.contains(requestedRole)) {
                log.warn("Login rejected: unsupported role specified");
                throw new BadRequestException(INVALID_ROLE_MESSAGE);
            }
            return loginWithExplicitRole(requestedRole, input, request.getPassword());
        }

        // Backward compatibility: no role given, ordered search across the account tables
        return loginWithLegacyFallback(input, request.getPassword());
    }

    private LoginResponse loginWithExplicitRole(String role, String input, String password) {
        switch (role) {
            case "FARMER": {
                Optional<User> farmerUser = lookup(() -> userRepository.findByEmailAndRole(input, Role.FARMER)
                        .or(() -> userRepository.findByEmailIgnoreCaseAndRole(input, Role.FARMER)));
                if (farmerUser.isPresent()) {
                    return loginFarmerViaCoreUser(farmerUser.get(), password);
                }
                Optional<Farmer> farmer = findFarmer(input);
                if (farmer.isPresent()) {
                    return loginFarmer(farmer.get(), null, password);
                }
                throw failUnknownAccount(password);
            }
            case "PARTNER": {
                Optional<Partner> partner = findPartner(input);
                if (partner.isPresent()) {
                    return loginPartner(partner.get(), password);
                }
                // Partners authenticate only against the partners table (no users-table fallback)
                warnIfLegacyCoreUserAccount(input, Role.PARTNER);
                throw failUnknownAccount(password);
            }
            case "OPERATOR": {
                Optional<Operator> operator = findOperator(input);
                if (operator.isPresent()) {
                    return loginOperator(operator.get(), password);
                }
                // Operators authenticate only against the operators table (no users-table fallback)
                warnIfLegacyCoreUserAccount(input, Role.OPERATOR);
                throw failUnknownAccount(password);
            }
            case "ADMIN": {
                Optional<Admin> admin = lookup(() -> adminRepository.findByEmail(input)
                        .or(() -> adminRepository.findByEmailIgnoreCase(input)));
                if (admin.isPresent()) {
                    return loginAdmin(admin.get(), password);
                }
                Optional<User> adminUser = lookup(() -> userRepository.findByEmailAndRole(input, Role.ADMIN)
                        .or(() -> userRepository.findByEmailIgnoreCaseAndRole(input, Role.ADMIN)));
                if (adminUser.isPresent()) {
                    return loginCoreUserAdmin(adminUser.get(), password);
                }
                throw failUnknownAccount(password);
            }
            default:
                throw new BadRequestException(INVALID_ROLE_MESSAGE);
        }
    }

    private LoginResponse loginWithLegacyFallback(String input, String password) {
        // 1. users table: only FARMER and ADMIN core users may authenticate from it; partner/operator
        //    identities live in their own tables
        Optional<User> user = lookup(() -> userRepository.findByEmail(input)
                .or(() -> userRepository.findByEmailIgnoreCase(input)));
        if (user.isPresent() && user.get().getRole() == Role.FARMER) {
            return loginFarmerViaCoreUser(user.get(), password);
        }
        if (user.isPresent() && user.get().getRole() == Role.ADMIN) {
            return loginCoreUserAdmin(user.get(), password);
        }
        if (user.isPresent()) {
            log.warn("Login via users table refused for legacy {} account (User ID {}); this role must use its own account table",
                    user.get().getRole(), user.get().getId());
        }

        // 2. admins table
        Optional<Admin> admin = lookup(() -> adminRepository.findByEmail(input)
                .or(() -> adminRepository.findByEmailIgnoreCase(input)));
        if (admin.isPresent()) {
            return loginAdmin(admin.get(), password);
        }

        // 3. farmers table
        Optional<Farmer> farmer = lookup(() -> farmerRepository.findByMobileNumberOrEmail(input, input)
                .or(() -> farmerRepository.findByMobileNumberOrEmailIgnoreCase(input, input)));
        if (farmer.isPresent()) {
            return loginFarmer(farmer.get(), null, password);
        }

        // 4. partners table
        Optional<Partner> partner = lookup(() -> partnerRepository.findByMobileNumberOrEmail(input, input)
                .or(() -> partnerRepository.findByMobileNumberOrEmailIgnoreCase(input, input)));
        if (partner.isPresent()) {
            return loginPartner(partner.get(), password);
        }

        // 5. operators table
        Optional<Operator> operator = findOperator(input);
        if (operator.isPresent()) {
            return loginOperator(operator.get(), password);
        }

        throw failUnknownAccount(password);
    }

    // ---------------------------------------------------------------------------------------------
    // Account lookups (each confined to one account table)
    // ---------------------------------------------------------------------------------------------

    private Optional<Farmer> findFarmer(String input) {
        Optional<Farmer> farmer = lookup(() -> farmerRepository.findByMobileNumberOrEmail(input, input)
                .or(() -> farmerRepository.findByMobileNumberOrEmailIgnoreCase(input, input)));
        String mobile = AccountIdentifiers.normalizeMobile(input);
        if (farmer.isEmpty() && mobile != null) {
            farmer = lookup(() -> farmerRepository.findByMobileNumber(mobile)
                    .or(() -> farmerRepository.findByMobileNumberOrEmailIgnoreCase(mobile, input)));
        }
        return farmer;
    }

    private Optional<Partner> findPartner(String input) {
        Optional<Partner> partner = lookup(() -> partnerRepository.findByMobileNumberOrEmail(input, input)
                .or(() -> partnerRepository.findByMobileNumberOrEmailIgnoreCase(input, input)));
        String mobile = AccountIdentifiers.normalizeMobile(input);
        if (partner.isEmpty() && mobile != null) {
            partner = lookup(() -> partnerRepository.findByMobileNumber(mobile)
                    .or(() -> partnerRepository.findByMobileNumberOrEmailIgnoreCase(mobile, input)));
        }
        return partner;
    }

    private Optional<Operator> findOperator(String input) {
        Optional<Operator> operator = lookup(() -> operatorRepository.findByMobileNumberOrEmailIgnoreCase(input, input));
        String mobile = AccountIdentifiers.normalizeMobile(input);
        if (operator.isEmpty() && mobile != null) {
            operator = lookup(() -> operatorRepository.findByMobileNumber(mobile)
                    .or(() -> operatorRepository.findByMobileNumberOrEmailIgnoreCase(mobile, input)));
        }
        return operator;
    }

    /** An identifier matching several rows (e.g. duplicate operator emails) is treated as "no account". */
    private <T> Optional<T> lookup(Supplier<Optional<T>> query) {
        try {
            Optional<T> result = query.get();
            return result != null ? result : Optional.empty();
        } catch (IncorrectResultSizeDataAccessException e) {
            log.warn("Login identifier matched more than one account in a table; refusing to choose one");
            return Optional.empty();
        }
    }

    /**
     * Partner/operator logins never fall back to the users table. Surface any legacy users-table account of
     * that role in server logs (the caller still receives the generic credential error).
     */
    private void warnIfLegacyCoreUserAccount(String input, Role role) {
        lookup(() -> userRepository.findByEmailIgnoreCaseAndRole(input, role)).ifPresent(user ->
                log.warn("Login refused for legacy users-table {} account (User ID {}): {} accounts must exist in their own table",
                        role, user.getId(), role));
    }

    // ---------------------------------------------------------------------------------------------
    // Per-account-type authentication: password first, then account state
    // ---------------------------------------------------------------------------------------------

    /** A FARMER core user is only a lookup path to its farmer profile; the farmer's own password applies. */
    private LoginResponse loginFarmerViaCoreUser(User user, String password) {
        Farmer farmer = user.getFarmer();
        if (farmer == null) {
            log.warn("Login refused for Farmer User ID {}: no linked farmer profile", user.getId());
            throw failUnknownAccount(password);
        }
        return loginFarmer(farmer, user, password);
    }

    /**
     * Farmers authenticate with farmers.password only. A farmer without a password (former OTP-only account)
     * fails exactly like a wrong password and must use "Forgot password?". PENDING_OTP no longer blocks
     * login; a disabled core user, a deactivated profile or status INACTIVE still do.
     */
    private LoginResponse loginFarmer(Farmer farmer, User viaUser, String password) {
        if (!passwordMatches(password, farmer.getPassword(), upgraded -> {
            farmer.setPassword(upgraded);
            farmerRepository.save(farmer);
        })) {
            log.warn("Login failed for farmer ID {}: invalid credentials", farmer.getFarmerId());
            throw invalidCredentials();
        }
        if (viaUser != null && !viaUser.isEnabled()) {
            log.warn("Login blocked for farmer ID {}: linked user account is disabled", farmer.getFarmerId());
            throw new BadRequestException(FARMER_DISABLED_MESSAGE);
        }
        if (!farmer.isActive() || "INACTIVE".equalsIgnoreCase(farmer.getAccountStatus())) {
            log.warn("Login blocked for farmer ID {}: account is deactivated", farmer.getFarmerId());
            throw new BadRequestException(FARMER_DEACTIVATED_MESSAGE);
        }

        if (viaUser != null) {
            // Email login through the core user: the session is still bound to farmers.id
            String token = jwtService.generateToken(farmer.getFarmerId(), "FARMER", viaUser.getEmail(),
                    JwtService.accountTypeClaims(AccountType.FARMER));
            log.info("Login successful for farmer ID {} (core user ID {})", farmer.getFarmerId(), viaUser.getId());
            return LoginResponse.builder()
                    .token(token)
                    .userId(viaUser.getId())
                    .farmerId(farmer.getFarmerId())
                    .fullName(viaUser.getName())
                    .mobileNumber(farmer.getMobileNumber())
                    .preferredLanguage(farmer.getPreferredLanguage())
                    .email(viaUser.getEmail())
                    .role(Role.FARMER.name())
                    .accountStatus(viaUser.isEnabled() ? "ACTIVE" : "INACTIVE")
                    .message("Login successful. Welcome back, " + viaUser.getName() + "!")
                    .build();
        }

        String token = jwtService.generateToken(farmer.getFarmerId(), "FARMER", farmer.getMobileNumber(),
                JwtService.accountTypeClaims(AccountType.FARMER));
        log.info("Login successful for farmer ID {}", farmer.getFarmerId());
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

    private LoginResponse loginPartner(Partner partner, String password) {
        if (!passwordMatches(password, partner.getPassword(), upgraded -> {
            partner.setPassword(upgraded);
            partnerRepository.save(partner);
        })) {
            log.warn("Login failed for partner ID {}: invalid credentials", partner.getId());
            throw invalidCredentials();
        }
        if (!partner.isActive()) {
            log.warn("Login blocked for partner ID {}: account is deactivated", partner.getId());
            throw new BadRequestException(PARTNER_DEACTIVATED_MESSAGE);
        }
        if (partner.getVerificationStatus() == Partner.VerificationStatus.REJECTED) {
            log.warn("Login blocked for partner ID {}: verification rejected", partner.getId());
            throw new BadRequestException(PARTNER_REJECTED_MESSAGE);
        }
        if (partner.getVerificationStatus() != Partner.VerificationStatus.APPROVED) {
            log.warn("Login blocked for partner ID {}: verification pending", partner.getId());
            throw new BadRequestException(PARTNER_PENDING_MESSAGE);
        }

        String token = jwtService.generateToken(partner.getId(), "PARTNER", partner.getMobileNumber(),
                JwtService.accountTypeClaims(AccountType.PARTNER));
        log.info("Partner login successful for partner ID {}", partner.getId());
        return LoginResponse.builder()
                .token(token)
                .partnerId(partner.getId())
                .fullName(partner.getFullName())
                .businessName(partner.getBusinessName())
                .mobileNumber(partner.getMobileNumber())
                .email(partner.getEmail())
                .role("PARTNER")
                .accountStatus(partner.getVerificationStatus().name())
                .message("Partner login successful. Welcome back, " + partner.getFullName() + "!")
                .build();
    }

    /** Operators need an active, APPROVED account; mobile (OTP) verification is no longer required. */
    private LoginResponse loginOperator(Operator operator, String password) {
        if (!passwordMatches(password, operator.getPassword(), upgraded -> {
            operator.setPassword(upgraded);
            operatorRepository.save(operator);
        })) {
            log.warn("Login failed for operator ID {}: invalid credentials", operator.getId());
            throw invalidCredentials();
        }
        if (!operator.isActive()) {
            log.warn("Login blocked for operator ID {}: account is inactive", operator.getId());
            throw new BadRequestException(OPERATOR_INACTIVE_MESSAGE);
        }
        if (operator.getStatus() == OperatorStatus.PENDING) {
            log.warn("Login blocked for operator ID {}: pending admin approval", operator.getId());
            throw new BadRequestException(OPERATOR_PENDING_MESSAGE);
        }
        if (operator.getStatus() == OperatorStatus.REJECTED) {
            log.warn("Login blocked for operator ID {}: account rejected", operator.getId());
            String reasonSuffix = operator.getRejectionReason() != null && !operator.getRejectionReason().isBlank()
                    ? ": " + operator.getRejectionReason()
                    : "";
            throw new BadRequestException("Operator account has been rejected" + reasonSuffix);
        }
        if (operator.getStatus() != OperatorStatus.APPROVED) {
            log.warn("Login blocked for operator ID {}: status {}", operator.getId(), operator.getStatus());
            throw new BadRequestException("Operator account is not approved: " + operator.getStatus());
        }

        String token = jwtService.generateOperatorToken(operator.getId(), operator.getMobileNumber());
        log.info("Operator login successful for operator ID {}", operator.getId());
        return LoginResponse.builder()
                .token(token)
                .operatorId(operator.getId())
                .fullName(operator.getFullName())
                .mobileNumber(operator.getMobileNumber())
                .email(operator.getEmail())
                .role("OPERATOR")
                .accountStatus(operator.getStatus().name())
                .message("Operator login successful. Welcome back, " + operator.getFullName() + "!")
                .build();
    }

    private LoginResponse loginAdmin(Admin admin, String password) {
        if (!passwordMatches(password, admin.getPassword(), null)) {
            log.warn("Login failed for admin ID {}: invalid credentials", admin.getId());
            throw invalidCredentials();
        }
        if (!admin.isActive()) {
            log.warn("Login blocked for admin ID {}: account is deactivated", admin.getId());
            throw new BadRequestException(ADMIN_DEACTIVATED_MESSAGE);
        }

        String token = jwtService.generateToken(admin.getId(), "ADMIN", admin.getEmail(),
                JwtService.accountTypeClaims(AccountType.ADMIN));
        log.info("Admin login successful for admin ID {}", admin.getId());
        return LoginResponse.builder()
                .token(token)
                .fullName(admin.getFullName())
                .email(admin.getEmail())
                .role("ADMIN")
                .accountStatus("ACTIVE")
                .message("Admin login successful. Welcome back, " + admin.getFullName() + "!")
                .build();
    }

    /** users-table administrators (role ADMIN); the token is typed USER and resolved against users.id only. */
    private LoginResponse loginCoreUserAdmin(User user, String password) {
        if (user.getRole() != Role.ADMIN) {
            log.warn("Login via users table refused for {} account (User ID {})", user.getRole(), user.getId());
            throw failUnknownAccount(password);
        }
        if (!passwordMatches(password, user.getPassword(), upgraded -> {
            user.setPassword(upgraded);
            userRepository.save(user);
        })) {
            log.warn("Login failed for core admin user ID {}: invalid credentials", user.getId());
            throw invalidCredentials();
        }
        if (!user.isEnabled()) {
            log.warn("Login blocked for core admin user ID {}: account is disabled", user.getId());
            throw new BadRequestException("Account is disabled. Please contact customer support.");
        }

        String token = jwtService.generateToken(user.getId(), "ADMIN", user.getEmail(),
                JwtService.accountTypeClaims(AccountType.USER));
        log.info("Core admin login successful for user ID {}", user.getId());
        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .fullName(user.getName())
                .email(user.getEmail())
                .role(Role.ADMIN.name())
                .accountStatus("ACTIVE")
                .message("Login successful. Welcome back, " + user.getName() + "!")
                .build();
    }

    // ---------------------------------------------------------------------------------------------
    // Password verification
    // ---------------------------------------------------------------------------------------------

    /**
     * BCrypt verification of the password as typed, then with surrounding whitespace removed. A stored value
     * that is not a BCrypt hash is a legacy plaintext password: it is compared in constant time and, on a
     * match, upgraded to a BCrypt hash. An empty stored password never matches.
     */
    private boolean passwordMatches(String rawPassword, String storedPassword, Consumer<String> legacyUpgrade) {
        if (rawPassword == null || rawPassword.isEmpty() || storedPassword == null || storedPassword.isBlank()) {
            burnPasswordCheck(rawPassword);
            return false;
        }
        if (passwordEncoder.matches(rawPassword, storedPassword)) {
            return true;
        }
        String trimmed = rawPassword.trim();
        boolean hasPadding = !trimmed.equals(rawPassword) && !trimmed.isEmpty();
        if (hasPadding && passwordEncoder.matches(trimmed, storedPassword)) {
            return true;
        }
        if (legacyUpgrade != null && !BCRYPT_PATTERN.matcher(storedPassword).matches()) {
            boolean legacyMatch = constantTimeEquals(storedPassword, rawPassword)
                    || (hasPadding && constantTimeEquals(storedPassword, trimmed));
            if (legacyMatch) {
                if (PasswordPolicy.MAX_UTF8_BYTES >= trimmed.getBytes(StandardCharsets.UTF_8).length) {
                    legacyUpgrade.accept(passwordEncoder.encode(trimmed));
                    log.info("Upgraded a legacy unhashed password to a BCrypt hash");
                }
                return true;
            }
        }
        return false;
    }

    private void burnPasswordCheck(String rawPassword) {
        passwordEncoder.matches(rawPassword == null ? "" : rawPassword, DUMMY_PASSWORD_HASH);
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private BadRequestException failUnknownAccount(String password) {
        burnPasswordCheck(password);
        return invalidCredentials();
    }

    private static BadRequestException invalidCredentials() {
        return new BadRequestException(INVALID_CREDENTIALS_MESSAGE);
    }
}
