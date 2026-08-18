package com.agrorental.farmer.service;

import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.farmer.dto.ChangePasswordRequest;
import com.agrorental.farmer.dto.FarmerProfileResponse;
import com.agrorental.farmer.dto.UpdateFarmerProfileRequest;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.repository.FarmerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FarmerProfileService {

    private final FarmerRepository farmerRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public FarmerProfileResponse getProfile(Long farmerId) {
        log.info("Fetching profile for farmer ID: {}", farmerId);
        Farmer farmer = farmerRepository.findById(farmerId)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer profile not found for ID: " + farmerId));
        return mapToResponse(farmer);
    }

    @Transactional(readOnly = true)
    public FarmerProfileResponse getProfileByMobile(String mobileNumber) {
        Farmer farmer = farmerRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer profile not found for mobile: " + mobileNumber));
        return mapToResponse(farmer);
    }

    @Transactional
    public FarmerProfileResponse updateProfile(Long farmerId, UpdateFarmerProfileRequest request) {
        log.info("Updating profile for farmer ID: {}", farmerId);

        Farmer farmer = farmerRepository.findById(farmerId)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer profile not found for ID: " + farmerId));

        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            String newEmail = request.getEmail().trim();
            if (!newEmail.equalsIgnoreCase(farmer.getEmail())) {
                if (farmerRepository.existsByEmail(newEmail)) {
                    throw new BadRequestException("Email address is already registered to another account.");
                }
                farmer.setEmail(newEmail);
            }
        }

        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            farmer.setFullName(request.getFullName().trim());
        }

        if (request.getAddress() != null) {
            farmer.setAddress(request.getAddress().trim());
        }

        if (request.getPreferredLanguage() != null && !request.getPreferredLanguage().trim().isEmpty()) {
            farmer.setPreferredLanguage(request.getPreferredLanguage().trim());
        }

        if (request.getProfileImage() != null) {
            farmer.setProfileImage(request.getProfileImage().trim());
        }

        Farmer updated = farmerRepository.save(farmer);
        log.info("Farmer profile updated successfully for ID: {}", farmerId);
        return mapToResponse(updated);
    }

    @Transactional
    public void changePassword(Long farmerId, ChangePasswordRequest request) {
        log.info("Processing password change for farmer ID: {}", farmerId);

        Farmer farmer = farmerRepository.findById(farmerId)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer profile not found for ID: " + farmerId));

        if (farmer.getPassword() != null && !farmer.getPassword().isEmpty()) {
            if (request.getCurrentPassword() == null || request.getCurrentPassword().isEmpty()) {
                throw new BadRequestException("Current password is required to set a new password.");
            }
            if (!passwordEncoder.matches(request.getCurrentPassword(), farmer.getPassword())) {
                throw new BadRequestException("Incorrect current password. Please try again.");
            }
        }

        farmer.setPassword(passwordEncoder.encode(request.getNewPassword().trim()));
        farmerRepository.save(farmer);
        log.info("Password changed successfully for farmer ID: {}", farmerId);
    }

    private FarmerProfileResponse mapToResponse(Farmer farmer) {
        return FarmerProfileResponse.builder()
                .farmerId(farmer.getFarmerId())
                .fullName(farmer.getFullName())
                .mobileNumber(farmer.getMobileNumber())
                .email(farmer.getEmail())
                .address(farmer.getAddress())
                .preferredLanguage(farmer.getPreferredLanguage())
                .profileImage(farmer.getProfileImage())
                .accountStatus(farmer.getAccountStatus())
                .createdAt(farmer.getCreatedAt())
                .updatedAt(farmer.getUpdatedAt())
                .build();
    }
}
