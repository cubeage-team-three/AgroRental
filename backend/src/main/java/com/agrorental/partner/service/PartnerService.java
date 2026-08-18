package com.agrorental.partner.service;

import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.partner.dto.PartnerChangePasswordRequest;
import com.agrorental.partner.dto.PartnerDashboardResponse;
import com.agrorental.partner.dto.PartnerProfileResponse;
import com.agrorental.partner.dto.PartnerProfileUpdateRequest;
import com.agrorental.partner.dto.PartnerRegistrationRequest;
import com.agrorental.partner.entity.Partner;
import com.agrorental.partner.repository.PartnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartnerService {

    private final PartnerRepository partnerRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Partner registerPartner(PartnerRegistrationRequest request) {
        if (partnerRepository.existsByMobileNumber(request.getMobileNumber())) {
            throw new BadRequestException("Mobile number already registered");
        }

        if (request.getEmail() != null && !request.getEmail().trim().isEmpty() &&
                partnerRepository.existsByEmail(request.getEmail().trim())) {
            throw new BadRequestException("Email already registered");
        }

        Partner partner = Partner.builder()
                .fullName(request.getFullName().trim())
                .businessName(request.getBusinessName() != null ? request.getBusinessName().trim() : null)
                .mobileNumber(request.getMobileNumber().trim())
                .email(request.getEmail() != null && !request.getEmail().trim().isEmpty() ? request.getEmail().trim() : null)
                .address(request.getAddress() != null ? request.getAddress().trim() : null)
                .gstNumber(request.getGstNumber() != null ? request.getGstNumber().trim() : null)
                .aadhaarNumber(request.getAadhaarNumber() != null ? request.getAadhaarNumber().trim() : null)
                .panNumber(request.getPanNumber() != null ? request.getPanNumber().trim() : null)
                .password(passwordEncoder.encode(request.getPassword()))
                .profilePhoto(request.getProfilePhoto() != null ? request.getProfilePhoto().trim() : null)
                .otpVerified(false)
                .verificationStatus(Partner.VerificationStatus.PENDING)
                .build();

        return partnerRepository.save(partner);
    }

    @Transactional(readOnly = true)
    public PartnerProfileResponse getPartnerProfile(Long id) {
        Partner partner = partnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Partner profile not found with ID: " + id));

        return toProfileResponse(partner);
    }

    @Transactional
    public PartnerProfileResponse updatePartnerProfile(Long id, PartnerProfileUpdateRequest request) {
        Partner partner = partnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Partner profile not found with ID: " + id));

        // Check if email changed and is already taken by another partner
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            String newEmail = request.getEmail().trim();
            if (!newEmail.equalsIgnoreCase(partner.getEmail())) {
                partnerRepository.findByEmail(newEmail).ifPresent(existing -> {
                    if (!existing.getId().equals(id)) {
                        throw new BadRequestException("Email " + newEmail + " is already in use by another account.");
                    }
                });
            }
            partner.setEmail(newEmail);
        } else {
            partner.setEmail(null);
        }

        partner.setFullName(request.getFullName().trim());
        partner.setBusinessName(request.getBusinessName() != null ? request.getBusinessName().trim() : null);
        partner.setAddress(request.getAddress() != null ? request.getAddress().trim() : null);
        partner.setGstNumber(request.getGstNumber() != null ? request.getGstNumber().trim() : null);
        partner.setAadhaarNumber(request.getAadhaarNumber() != null ? request.getAadhaarNumber().trim() : null);
        partner.setPanNumber(request.getPanNumber() != null ? request.getPanNumber().trim() : null);

        if (request.getProfilePhoto() != null) {
            partner.setProfilePhoto(request.getProfilePhoto().trim());
        }

        Partner updated = partnerRepository.save(partner);
        log.info("Successfully updated partner profile for ID: {}", id);
        return toProfileResponse(updated);
    }

    @Transactional
    public void changePartnerPassword(Long id, PartnerChangePasswordRequest request) {
        Partner partner = partnerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Partner profile not found with ID: " + id));

        // Validate current password
        if (partner.getPassword() != null && !passwordEncoder.matches(request.getCurrentPassword(), partner.getPassword())) {
            throw new BadRequestException("Current password provided is incorrect.");
        }

        // Check if new password is identical to old password
        if (passwordEncoder.matches(request.getNewPassword(), partner.getPassword())) {
            throw new BadRequestException("New password cannot be the same as the current password.");
        }

        partner.setPassword(passwordEncoder.encode(request.getNewPassword()));
        partnerRepository.save(partner);
        log.info("Successfully changed password for partner ID: {}", id);
    }

    @Transactional(readOnly = true)
    public Optional<PartnerDashboardResponse> getPartnerDashboard(Long id) {
        return partnerRepository.findById(id)
                .map(partner -> new PartnerDashboardResponse(
                        partner.getId(),
                        partner.getFullName(),
                        partner.getBusinessName(),
                        partner.getMobileNumber(),
                        partner.getEmail(),
                        partner.getAddress(),
                        partner.isOtpVerified(),
                        partner.getVerificationStatus()
                ));
    }

    public PartnerProfileResponse toProfileResponse(Partner partner) {
        return PartnerProfileResponse.builder()
                .id(partner.getId())
                .fullName(partner.getFullName())
                .businessName(partner.getBusinessName())
                .mobileNumber(partner.getMobileNumber())
                .email(partner.getEmail())
                .address(partner.getAddress())
                .gstNumber(partner.getGstNumber())
                .aadhaarNumber(partner.getAadhaarNumber())
                .panNumber(partner.getPanNumber())
                .profilePhoto(partner.getProfilePhoto())
                .otpVerified(partner.isOtpVerified())
                .verificationStatus(partner.getVerificationStatus())
                .active(partner.isActive())
                .createdAt(partner.getCreatedAt())
                .updatedAt(partner.getUpdatedAt())
                .build();
    }
}