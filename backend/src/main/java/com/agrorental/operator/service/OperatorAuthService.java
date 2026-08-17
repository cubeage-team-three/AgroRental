package com.agrorental.operator.service;

import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.security.JwtTokenProvider;
import com.agrorental.operator.dto.OperatorLoginRequest;
import com.agrorental.operator.dto.OperatorLoginResponse;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.repository.OperatorRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class OperatorAuthService {

    private final OperatorRepository operatorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public OperatorAuthService(
            OperatorRepository operatorRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider) {
        this.operatorRepository = operatorRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Transactional(readOnly = true)
    public OperatorLoginResponse login(OperatorLoginRequest request) {
        String identifier = request.getIdentifier().trim();
        String password = request.getPassword();

        // Find operator by mobile or email
        Operator operator = operatorRepository.findByMobileNumberOrEmail(identifier, identifier)
                .orElseThrow(() -> new BadRequestException("Invalid mobile number/email or password"));

        // Verify password
        if (!passwordEncoder.matches(password, operator.getPassword())) {
            log.warn("Failed login attempt for identifier: {}", identifier);
            throw new BadRequestException("Invalid mobile number/email or password");
        }

        // Check mobile verification (Module 2 gate)
        if (!operator.isMobileVerified()) {
            throw new BadRequestException("Mobile number is not verified. Please complete OTP verification first.");
        }

        // Check document verification / approval status (Module 3 gate)
        if (operator.getStatus() == OperatorStatus.PENDING) {
            if (!operator.isDocumentsSubmitted()) {
                throw new BadRequestException("Your account is pending document submission. Please upload your documents.");
            } else {
                throw new BadRequestException("Your account is under review by compliance admin. Please wait for approval.");
            }
        }

        if (operator.getStatus() == OperatorStatus.REJECTED) {
            throw new BadRequestException("Your operator application has been rejected. Please review document feedback.");
        }

        if (operator.getStatus() != OperatorStatus.APPROVED) {
            throw new BadRequestException("Your operator account is not active. Status: " + operator.getStatus());
        }

        // Generate JWT token
        String token = jwtTokenProvider.generateToken(
                operator.getId(),
                operator.getMobileNumber(),
                operator.getFullName(),
                "ROLE_OPERATOR",
                operator.getStatus().name()
        );

        log.info("Operator {} logged in successfully", operator.getMobileNumber());

        OperatorLoginResponse.OperatorProfileSummary summary = OperatorLoginResponse.OperatorProfileSummary.builder()
                .id(operator.getId())
                .fullName(operator.getFullName())
                .mobileNumber(operator.getMobileNumber())
                .email(operator.getEmail())
                .address(operator.getAddress())
                .experience(operator.getExperience())
                .skills(operator.getSkills())
                .profilePhoto(operator.getProfilePhoto())
                .status(operator.getStatus())
                .mobileVerified(operator.isMobileVerified())
                .documentsSubmitted(operator.isDocumentsSubmitted())
                .build();

        return OperatorLoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(86400L) // 24 hours
                .operator(summary)
                .build();
    }
}
