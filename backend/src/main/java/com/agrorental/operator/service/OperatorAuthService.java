package com.agrorental.operator.service;

import com.agrorental.common.exception.ForbiddenException;
import com.agrorental.common.exception.UnauthorizedException;
import com.agrorental.operator.dto.AuthenticatedOperatorResponse;
import com.agrorental.operator.dto.OperatorLoginRequest;
import com.agrorental.operator.dto.OperatorLoginResponse;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.mapper.OperatorMapper;
import com.agrorental.operator.repository.OperatorRepository;
import com.agrorental.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing Operator authentication, credential validation,
 * account status lifecycle checks, and JWT token issuance (FR-39).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperatorAuthService {

    private final OperatorRepository operatorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OperatorMapper operatorMapper;

    /**
     * Authenticates an operator using mobile number and password, enforces account
     * lifecycle rules, and issues a signed JWT access token.
     *
     * @param request OperatorLoginRequest containing mobile number and raw password.
     * @return OperatorLoginResponse containing JWT token and safe operator identity.
     */
    @Transactional
    public OperatorLoginResponse login(OperatorLoginRequest request) {
        String rawMobile = request.getMobileNumber() != null ? request.getMobileNumber().trim() : "";
        String cleanMobile = cleanMobileNumber(rawMobile);
        String searchMobile = (cleanMobile != null && cleanMobile.length() == 10) ? cleanMobile : rawMobile;
        log.info("Processing operator login attempt for mobile: [PROTECTED]");

        // 1. Find operator by mobile number
        Operator operator = operatorRepository.findByMobileNumber(searchMobile)
                .or(() -> operatorRepository.findByMobileNumber(rawMobile))
                .orElseThrow(() -> {
                    log.warn("Operator login failed: Mobile number not registered");
                    return new UnauthorizedException("Invalid mobile number or password");
                });

        // 2. Validate password via BCrypt PasswordEncoder with safe legacy compatibility
        if (!verifyOperatorPassword(request.getPassword(), operator)) {
            log.warn("Operator login failed for operator ID {}: Password mismatch", operator.getId());
            throw new UnauthorizedException("Invalid mobile number or password");
        }

        // 3. Enforce Mobile Verification
        if (!operator.isMobileVerified()) {
            log.warn("Operator login blocked for operator ID {}: Mobile is unverified", operator.getId());
            throw new ForbiddenException("Mobile number is not verified");
        }

        // 4. Enforce Operator Status
        if (operator.getStatus() == OperatorStatus.PENDING) {
            log.warn("Operator login blocked for operator ID {}: Account is pending admin approval", operator.getId());
            throw new ForbiddenException("Operator account is pending admin approval");
        }

        if (operator.getStatus() == OperatorStatus.REJECTED) {
            log.warn("Operator login blocked for operator ID {}: Account is rejected", operator.getId());
            String reasonSuffix = (operator.getRejectionReason() != null && !operator.getRejectionReason().isBlank())
                    ? ": " + operator.getRejectionReason()
                    : "";
            throw new ForbiddenException("Operator account has been rejected" + reasonSuffix);
        }

        if (operator.getStatus() != OperatorStatus.APPROVED) {
            log.warn("Operator login blocked for operator ID {}: Status is {}", operator.getId(), operator.getStatus());
            throw new ForbiddenException("Operator account status is not approved: " + operator.getStatus());
        }

        // 5. Enforce Active Account Status
        if (!operator.isActive()) {
            log.warn("Operator login blocked for operator ID {}: Account is inactive", operator.getId());
            throw new ForbiddenException("Operator account is inactive");
        }

        // 6. Generate signed JWT token
        String token = jwtService.generateOperatorToken(operator.getId(), operator.getMobileNumber());
        long expiresIn = jwtService.getExpiresInSeconds();

        // 7. Map safe response
        AuthenticatedOperatorResponse authenticatedOperator = operatorMapper.toAuthenticatedResponse(operator);

        log.info("Operator login successful for operator ID: {}", operator.getId());

        return OperatorLoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .operator(authenticatedOperator)
                .build();
    }

    /**
     * Retrieves the safe authenticated operator identity for the currently logged-in principal.
     *
     * @param operatorId Primary key ID of the authenticated operator.
     * @return AuthenticatedOperatorResponse with safe identity fields.
     */
    @Transactional(readOnly = true)
    public AuthenticatedOperatorResponse getCurrentOperator(Long operatorId) {
        if (operatorId == null) {
            throw new UnauthorizedException("Full authentication is required to access this resource");
        }

        Operator operator = operatorRepository.findById(operatorId)
                .orElseThrow(() -> new UnauthorizedException("Operator account not found"));

        if (!operator.isActive() || operator.getStatus() != OperatorStatus.APPROVED) {
            throw new ForbiddenException("Operator account is no longer active or approved");
        }

        return operatorMapper.toAuthenticatedResponse(operator);
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
}
