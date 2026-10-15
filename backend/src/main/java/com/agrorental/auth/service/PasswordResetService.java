package com.agrorental.auth.service;

import com.agrorental.auth.dto.ForgotPasswordRequest;
import com.agrorental.auth.dto.ForgotPasswordResponse;
import com.agrorental.auth.dto.ResetPasswordRequest;
import com.agrorental.auth.entity.AccountPasswordResetToken;
import com.agrorental.auth.entity.PasswordResetAccountType;
import com.agrorental.auth.repository.AccountPasswordResetTokenRepository;
import com.agrorental.common.config.DeploymentProfiles;
import com.agrorental.common.enums.Role;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.partner.entity.Partner;
import com.agrorental.partner.repository.PartnerRepository;
import com.agrorental.user.entity.User;
import com.agrorental.user.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * Self-service password reset for FARMER, PARTNER and OPERATOR accounts (admins excluded).
 *
 * <ul>
 *   <li>Forgot password always answers with the same status and body for existing and non-existing
 *       accounts; the message depends only on server configuration (delivery configured or not).</li>
 *   <li>Requests are rate limited per (role + identifier) and per client address before any lookup.</li>
 *   <li>Tokens carry 256 bits from {@link SecureRandom}; only their SHA-256 hash is stored
 *       (table account_password_reset_tokens). They expire after 30 minutes, are redeemed atomically
 *       (exactly one success under concurrent submission) and a new request or a completed reset
 *       invalidates the account's other outstanding tokens.</li>
 *   <li>A reset writes the BCrypt hash into the one account table named by the token's account type.</li>
 *   <li>Tokens, reset URLs and passwords are never logged. The raw token is returned in the response only
 *       in explicit development (active 'dev' profile AND app.password-reset.dev-token-in-response=true).</li>
 * </ul>
 */
@Slf4j
@Service
public class PasswordResetService {

    public static final Duration TOKEN_TTL = Duration.ofMinutes(30);
    static final int TOKEN_BYTES = 32;
    private static final Pattern TOKEN_FORMAT = Pattern.compile("^[A-Za-z0-9_-]{20,128}$");

    public static final String INVALID_TOKEN_MESSAGE = "Invalid or expired password reset token.";
    public static final String INVALID_ROLE_MESSAGE =
            "Invalid role specified for password recovery. Allowed roles: FARMER, PARTNER, OPERATOR.";
    public static final String IDENTIFIER_REQUIRED_MESSAGE = "Account identifier (email or mobile number) is required.";
    public static final String DELIVERY_CONFIGURED_MESSAGE =
            "If an account matches these details, password reset instructions have been sent.";
    public static final String DELIVERY_UNAVAILABLE_MESSAGE =
            "Password reset is not available because no email or SMS delivery is configured. Please contact support.";
    public static final String DEV_TOKEN_MESSAGE =
            "Development mode: no email or SMS delivery is configured. If an account matches these details, "
                    + "a password reset token has been created and is included in this response.";
    public static final String DEV_TOKEN_NOTICE =
            "DEVELOPMENT ONLY: this reset token is returned because the server runs with the explicit 'dev' profile "
                    + "and app.password-reset.dev-token-in-response=true. It is never returned in staging or production.";
    public static final String RESET_SUCCESS_MESSAGE = "Password reset successfully. You can now log in with your new password.";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int MAX_IDENTIFIER_LENGTH = 254;

    private final AccountPasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final FarmerRepository farmerRepository;
    private final PartnerRepository partnerRepository;
    private final OperatorRepository operatorRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetRateLimiter rateLimiter;
    private final ObjectProvider<PasswordResetDelivery> deliveryProvider;
    private final TransactionTemplate transactionTemplate;
    private final Environment environment;

    @Value("${app.password-reset.dev-token-in-response:false}")
    private boolean devTokenInResponse;

    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendBaseUrl = "http://localhost:5173";

    public PasswordResetService(AccountPasswordResetTokenRepository tokenRepository,
                                UserRepository userRepository,
                                FarmerRepository farmerRepository,
                                PartnerRepository partnerRepository,
                                OperatorRepository operatorRepository,
                                PasswordEncoder passwordEncoder,
                                PasswordResetRateLimiter rateLimiter,
                                ObjectProvider<PasswordResetDelivery> deliveryProvider,
                                PlatformTransactionManager transactionManager,
                                Environment environment) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.farmerRepository = farmerRepository;
        this.partnerRepository = partnerRepository;
        this.operatorRepository = operatorRepository;
        this.passwordEncoder = passwordEncoder;
        this.rateLimiter = rateLimiter;
        this.deliveryProvider = deliveryProvider;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.environment = environment;
    }

    @PostConstruct
    void logConfiguration() {
        if (devTokenInResponse && !DeploymentProfiles.isExplicitDevelopment(environment)) {
            log.warn("app.password-reset.dev-token-in-response=true is IGNORED: reset tokens are only disclosed with an "
                    + "explicitly active 'dev' profile and never in staging or production.");
        }
        PasswordResetDelivery delivery = deliveryProvider.getIfUnique();
        if (delivery != null && delivery.isConfigured() && !hasFrontendBaseUrl()) {
            log.error("Password reset: a delivery provider is configured but app.frontend.url (APP_FRONTEND_URL) is empty, "
                    + "so reset links cannot be built; password reset stays unavailable until it is set.");
        }
        if (configuredDelivery() == null) {
            log.warn("Password reset: no email/SMS delivery provider is configured; forgot-password requests report that "
                    + "reset is unavailable{}.", isDevTokenDisclosureAllowed() ? " (development token disclosure is ON)" : "");
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Forgot password
    // ------------------------------------------------------------------------------------------------

    /**
     * @param clientAddress the caller's network address, used only as a rate-limit key
     * @throws PasswordResetRateLimitedException when a rate limit is exceeded (HTTP 429, generic body)
     */
    public ForgotPasswordResponse requestReset(ForgotPasswordRequest request, String clientAddress) {
        String requestedRole = request.getRole() == null || request.getRole().isBlank() ? "FARMER" : request.getRole();
        PasswordResetAccountType accountType = PasswordResetAccountType.fromRole(requestedRole);
        if (accountType == null) {
            throw new BadRequestException(INVALID_ROLE_MESSAGE);
        }
        String identifier = request.getAccountIdentifier();
        if (identifier == null || identifier.isBlank() || identifier.length() > MAX_IDENTIFIER_LENGTH) {
            throw new BadRequestException(IDENTIFIER_REQUIRED_MESSAGE);
        }

        if (!rateLimiter.tryAcquire(accountType.name() + ":" + AccountIdentifiers.canonical(identifier),
                clientAddress == null ? "unknown" : clientAddress)) {
            log.warn("Password reset request rate limited (role {})", accountType);
            throw new PasswordResetRateLimitedException(rateLimiter.retryAfterSeconds());
        }

        PasswordResetDelivery delivery = configuredDelivery();
        boolean devDisclosure = isDevTokenDisclosureAllowed();
        if (delivery == null && !devDisclosure) {
            // Nothing could reach the account holder, so no token is created for anyone
            log.info("Password reset requested for role {}; no delivery provider configured, no token issued", accountType);
            return ForgotPasswordResponse.builder()
                    .message(DELIVERY_UNAVAILABLE_MESSAGE)
                    .deliveryConfigured(false)
                    .build();
        }

        IssuedToken issued = transactionTemplate.execute(status ->
                findTarget(accountType, identifier).map(this::issueToken).orElse(null));

        if (issued == null) {
            log.info("Password reset requested for role {}; no matching account (generic response returned)", accountType);
        } else {
            log.info("Password reset token issued for {} account #{}", accountType, issued.target().accountId());
            if (delivery != null) {
                deliver(delivery, issued);
            }
        }

        ForgotPasswordResponse.ForgotPasswordResponseBuilder response = ForgotPasswordResponse.builder()
                .deliveryConfigured(delivery != null)
                .message(delivery != null ? DELIVERY_CONFIGURED_MESSAGE : DEV_TOKEN_MESSAGE);
        if (devDisclosure) {
            response.devNotice(DEV_TOKEN_NOTICE);
            if (issued != null) {
                response.devResetToken(issued.rawToken());
            }
        }
        return response.build();
    }

    private IssuedToken issueToken(ResetTarget target) {
        LocalDateTime now = LocalDateTime.now();
        tokenRepository.invalidateOutstanding(target.accountType(), target.accountId(), now);

        String rawToken = newRawToken();
        LocalDateTime expiresAt = now.plus(TOKEN_TTL);
        tokenRepository.save(AccountPasswordResetToken.builder()
                .accountType(target.accountType())
                .accountId(target.accountId())
                .tokenHash(sha256Hex(rawToken))
                .expiresAt(expiresAt)
                .createdAt(now)
                .build());
        return new IssuedToken(rawToken, target, expiresAt);
    }

    private void deliver(PasswordResetDelivery delivery, IssuedToken issued) {
        ResetTarget target = issued.target();
        PasswordResetInstructions instructions = new PasswordResetInstructions(
                target.accountType(), target.accountId(), target.name(), target.email(), target.mobileNumber(),
                resetUrl(issued.rawToken()), issued.expiresAt());
        try {
            delivery.sendPasswordReset(instructions);
        } catch (RuntimeException e) {
            // The exception text may embed the reset URL, so only its type is logged
            log.error("Password reset delivery failed for {} account #{} ({})",
                    target.accountType(), target.accountId(), e.getClass().getSimpleName());
        }
    }

    private String resetUrl(String rawToken) {
        String base = frontendBaseUrl == null ? "" : frontendBaseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/reset-password?token=" + rawToken;
    }

    private Optional<ResetTarget> findTarget(PasswordResetAccountType accountType, String identifier) {
        try {
            return switch (accountType) {
                case FARMER -> findFarmer(identifier).map(farmer -> new ResetTarget(accountType, farmer.getFarmerId(),
                        farmer.getFullName(), blankToNull(farmer.getEmail()), farmer.getMobileNumber()));
                case PARTNER -> findPartner(identifier).map(partner -> new ResetTarget(accountType, partner.getId(),
                        partner.getFullName(), blankToNull(partner.getEmail()), partner.getMobileNumber()));
                case OPERATOR -> findOperator(identifier).map(operator -> new ResetTarget(accountType, operator.getId(),
                        operator.getFullName(), blankToNull(operator.getEmail()), operator.getMobileNumber()));
            };
        } catch (IncorrectResultSizeDataAccessException e) {
            log.warn("Password reset lookup for role {} matched more than one account; no token issued", accountType);
            return Optional.empty();
        }
    }

    /** farmers table only (an email may also be found through the farmer's linked core user). */
    private Optional<Farmer> findFarmer(String identifier) {
        if (AccountIdentifiers.looksLikeEmail(identifier)) {
            Optional<Farmer> byEmail = farmerRepository.findByEmailIgnoreCase(identifier);
            if (byEmail.isPresent()) {
                return byEmail;
            }
            return userRepository.findByEmailIgnoreCaseAndRole(identifier, Role.FARMER).map(User::getFarmer);
        }
        String mobile = AccountIdentifiers.normalizeMobile(identifier);
        return mobile != null ? farmerRepository.findByMobileNumber(mobile) : Optional.empty();
    }

    /** partners table only */
    private Optional<Partner> findPartner(String identifier) {
        if (AccountIdentifiers.looksLikeEmail(identifier)) {
            return partnerRepository.findByEmailIgnoreCase(identifier);
        }
        String mobile = AccountIdentifiers.normalizeMobile(identifier);
        return mobile != null ? partnerRepository.findByMobileNumber(mobile) : Optional.empty();
    }

    /** operators table only (a users-table account never receives an OPERATOR token) */
    private Optional<Operator> findOperator(String identifier) {
        if (AccountIdentifiers.looksLikeEmail(identifier)) {
            return operatorRepository.findByEmailIgnoreCase(identifier);
        }
        String mobile = AccountIdentifiers.normalizeMobile(identifier);
        return mobile != null ? operatorRepository.findByMobileNumber(mobile) : Optional.empty();
    }

    // ------------------------------------------------------------------------------------------------
    // Reset password
    // ------------------------------------------------------------------------------------------------

    /**
     * Redeems a reset token and sets the new password on the token's account. Unknown, malformed, expired,
     * already used and invalidated tokens all fail with the same {@link #INVALID_TOKEN_MESSAGE}.
     */
    public String resetPassword(ResetPasswordRequest request) {
        String newPassword = request.getNewPassword();
        if (newPassword == null || request.getConfirmPassword() == null) {
            throw new BadRequestException("New password and confirm password are required.");
        }
        if (!newPassword.equals(request.getConfirmPassword())) {
            throw new BadRequestException("New password and confirm password do not match.");
        }
        PasswordPolicy.validate(newPassword);

        String rawToken = request.getToken() == null ? "" : request.getToken().trim();
        if (!TOKEN_FORMAT.matcher(rawToken).matches()) {
            throw new BadRequestException(INVALID_TOKEN_MESSAGE);
        }
        PasswordResetAccountType requestedType = null;
        if (request.getRole() != null && !request.getRole().isBlank()) {
            requestedType = PasswordResetAccountType.fromRole(request.getRole());
            if (requestedType == null) {
                throw new BadRequestException(INVALID_TOKEN_MESSAGE);
            }
        }

        String tokenHash = sha256Hex(rawToken);
        String newPasswordHash = passwordEncoder.encode(newPassword);
        PasswordResetAccountType expectedType = requestedType;
        AtomicReference<AccountPasswordResetToken> redeemed = new AtomicReference<>();

        try {
            transactionTemplate.executeWithoutResult(status -> {
                LocalDateTime now = LocalDateTime.now();
                if (tokenRepository.consumeIfValid(tokenHash, now) != 1) {
                    throw new BadRequestException(INVALID_TOKEN_MESSAGE);
                }
                AccountPasswordResetToken token = tokenRepository.findByTokenHash(tokenHash)
                        .orElseThrow(() -> new BadRequestException(INVALID_TOKEN_MESSAGE));
                if (expectedType != null && expectedType != token.getAccountType()) {
                    // Rolls back the redemption: a cross-role attempt must not burn the token
                    throw new BadRequestException(INVALID_TOKEN_MESSAGE);
                }
                applyNewPassword(token.getAccountType(), token.getAccountId(), newPasswordHash);
                tokenRepository.invalidateOutstanding(token.getAccountType(), token.getAccountId(), now);
                redeemed.set(token);
            });
        } catch (TransientDataAccessException e) {
            // Lock timeouts / concurrent update conflicts: the competing redemption wins, this one is invalid
            log.warn("Password reset rejected after a concurrent redemption conflict ({})", e.getClass().getSimpleName());
            throw new BadRequestException(INVALID_TOKEN_MESSAGE);
        }

        AccountPasswordResetToken token = redeemed.get();
        log.info("Password reset completed for {} account #{}", token.getAccountType(), token.getAccountId());
        return RESET_SUCCESS_MESSAGE;
    }

    /** Writes the new BCrypt hash into the single account table named by the token's account type. */
    private void applyNewPassword(PasswordResetAccountType accountType, Long accountId, String newPasswordHash) {
        switch (accountType) {
            case FARMER -> {
                Farmer farmer = farmerRepository.findById(accountId)
                        .orElseThrow(() -> new BadRequestException(INVALID_TOKEN_MESSAGE));
                farmer.setPassword(newPasswordHash);
                farmerRepository.save(farmer);
            }
            case PARTNER -> {
                Partner partner = partnerRepository.findById(accountId)
                        .orElseThrow(() -> new BadRequestException(INVALID_TOKEN_MESSAGE));
                partner.setPassword(newPasswordHash);
                partnerRepository.save(partner);
            }
            case OPERATOR -> {
                Operator operator = operatorRepository.findById(accountId)
                        .orElseThrow(() -> new BadRequestException(INVALID_TOKEN_MESSAGE));
                operator.setPassword(newPasswordHash);
                operatorRepository.save(operator);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Configuration and helpers
    // ------------------------------------------------------------------------------------------------

    /** True only with an explicitly active 'dev' profile AND the opt-in property; never in staging/prod. */
    public boolean isDevTokenDisclosureAllowed() {
        return devTokenInResponse && DeploymentProfiles.isExplicitDevelopment(environment);
    }

    public boolean isDeliveryConfigured() {
        return configuredDelivery() != null;
    }

    /** The delivery provider, or null when none is configured or reset links cannot be built. */
    private PasswordResetDelivery configuredDelivery() {
        PasswordResetDelivery delivery = deliveryProvider.getIfUnique();
        return delivery != null && delivery.isConfigured() && hasFrontendBaseUrl() ? delivery : null;
    }

    private boolean hasFrontendBaseUrl() {
        return frontendBaseUrl != null && !frontendBaseUrl.isBlank();
    }

    /** Test support. */
    public void setDevTokenInResponse(boolean devTokenInResponse) {
        this.devTokenInResponse = devTokenInResponse;
    }

    /** Test support. */
    public void setFrontendBaseUrl(String frontendBaseUrl) {
        this.frontendBaseUrl = frontendBaseUrl;
    }

    static String newRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Lowercase hex SHA-256, the only form in which a token is stored. */
    public static String sha256Hex(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record ResetTarget(PasswordResetAccountType accountType, Long accountId, String name,
                               String email, String mobileNumber) {
    }

    private record IssuedToken(String rawToken, ResetTarget target, LocalDateTime expiresAt) {
        @Override
        public String toString() {
            return "IssuedToken[" + target.accountType() + " #" + target.accountId() + ", token=<redacted>]";
        }
    }
}
