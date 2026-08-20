package com.agrorental.operator.service;

import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.ForbiddenException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.operator.dto.OperatorChangePasswordRequest;
import com.agrorental.operator.dto.OperatorProfileResponse;
import com.agrorental.operator.dto.OperatorProfileUpdateRequest;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.mapper.OperatorMapper;
import com.agrorental.operator.repository.OperatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing authenticated Operator profile retrieval, editable field updates,
 * and secure credential changes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OperatorProfileService {

    private final OperatorRepository operatorRepository;
    private final OperatorMapper operatorMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * Retrieves the complete profile of the authenticated operator.
     *
     * @param operatorId ID extracted from authenticated JWT OperatorPrincipal
     * @return Safe OperatorProfileResponse DTO
     */
    @Transactional(readOnly = true)
    public OperatorProfileResponse getCurrentProfile(Long operatorId) {
        log.info("Fetching profile for operator ID: {}", operatorId);

        Operator operator = getActiveApprovedOperator(operatorId);
        return operatorMapper.toProfileResponse(operator);
    }

    /**
     * Updates permitted editable profile fields (fullName, email, address, experience, skills, profilePhoto).
     * Strictly protects immutable fields (mobileNumber, aadhaar, DL, status, active, mobileVerified, password).
     *
     * @param operatorId ID extracted from authenticated JWT OperatorPrincipal
     * @param request Validated profile update payload
     * @return Updated OperatorProfileResponse DTO
     */
    @Transactional
    public OperatorProfileResponse updateCurrentProfile(Long operatorId, OperatorProfileUpdateRequest request) {
        log.info("Updating profile for operator ID: {}", operatorId);

        Operator operator = getActiveApprovedOperator(operatorId);

        // Handle Email update with uniqueness validation across operators
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            String trimmedEmail = request.getEmail().trim();
            if (operatorRepository.existsByEmailAndIdNot(trimmedEmail, operatorId)) {
                log.warn("Profile update rejected for operator ID {}: Email {} already registered", operatorId, trimmedEmail);
                throw new BadRequestException("Email is already registered by another account");
            }
            operator.setEmail(trimmedEmail);
        }

        // Apply permitted editable fields only
        operator.setFullName(request.getFullName().trim());
        operator.setAddress(request.getAddress().trim());
        operator.setExperience(request.getExperience());
        operator.setSkills(request.getSkills().trim());

        if (request.getProfilePhoto() != null) {
            operator.setProfilePhoto(request.getProfilePhoto().trim());
        }

        // Save updated operator (updatedAt automatically modified via BaseEntity)
        Operator savedOperator = operatorRepository.save(operator);
        log.info("Profile successfully updated for operator ID: {}", operatorId);

        return operatorMapper.toProfileResponse(savedOperator);
    }

    /**
     * Securely updates an authenticated operator's password after verifying the current password.
     *
     * @param operatorId ID extracted from authenticated JWT OperatorPrincipal
     * @param request Validated password change payload
     */
    @Transactional
    public void changePassword(Long operatorId, OperatorChangePasswordRequest request) {
        log.info("Processing password change request for operator ID: {}", operatorId);

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            log.warn("Password change failed for operator ID {}: Passwords do not match", operatorId);
            throw new BadRequestException("New passwords do not match");
        }

        if (request.getNewPassword().length() < 8) {
            log.warn("Password change failed for operator ID {}: Password too short", operatorId);
            throw new BadRequestException("New password must be at least 8 characters long");
        }

        Operator operator = getActiveApprovedOperator(operatorId);

        // Verify current password matches BCrypt hash
        if (!passwordEncoder.matches(request.getCurrentPassword(), operator.getPassword())) {
            log.warn("Password change failed for operator ID {}: Current password mismatch", operatorId);
            throw new BadRequestException("Current password is incorrect");
        }

        // Verify new password is not identical to current password
        if (passwordEncoder.matches(request.getNewPassword(), operator.getPassword())) {
            log.warn("Password change failed for operator ID {}: New password equals old password", operatorId);
            throw new BadRequestException("New password must be different from current password");
        }

        // Encode and persist new password
        operator.setPassword(passwordEncoder.encode(request.getNewPassword()));
        operatorRepository.save(operator);

        log.info("Password successfully updated for operator ID: {}", operatorId);
    }

    /**
     * Helper method to verify operator existence and authorized status.
     */
    private Operator getActiveApprovedOperator(Long operatorId) {
        Operator operator = operatorRepository.findById(operatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found with ID: " + operatorId));

        if (!operator.isActive()) {
            log.warn("Access denied for operator ID {}: Account is inactive", operatorId);
            throw new ForbiddenException("Operator account is inactive");
        }

        if (operator.getStatus() != OperatorStatus.APPROVED) {
            log.warn("Access denied for operator ID {}: Account status is {}", operatorId, operator.getStatus());
            throw new ForbiddenException("Operator account is not approved");
        }

        return operator;
    }
}
