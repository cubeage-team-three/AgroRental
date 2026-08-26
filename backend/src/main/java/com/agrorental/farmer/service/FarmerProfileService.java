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
    public FarmerProfileResponse uploadAvatar(Long farmerId, org.springframework.web.multipart.MultipartFile file) {
        log.info("Processing avatar upload for farmer ID: {}", farmerId);

        Farmer farmer = farmerRepository.findById(farmerId)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer profile not found for ID: " + farmerId));

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Avatar file must not be empty.");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BadRequestException("Avatar file size must not exceed 5 MB.");
        }

        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();

        String extension = getFileExtension(originalFilename);
        if (!isAllowedImageExtension(extension) || !isAllowedMimeType(contentType)) {
            throw new BadRequestException("Invalid file type. Only JPG, PNG, WEBP, and GIF images are allowed.");
        }

        String safeFileName = java.util.UUID.randomUUID().toString() + extension;
        java.nio.file.Path uploadPath = java.nio.file.Paths.get("uploads", "avatars").toAbsolutePath().normalize();

        try {
            java.nio.file.Files.createDirectories(uploadPath);
            java.nio.file.Path targetLocation = uploadPath.resolve(safeFileName);
            java.nio.file.Files.copy(file.getInputStream(), targetLocation, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            String avatarUrl = "/uploads/avatars/" + safeFileName;
            farmer.setProfileImage(avatarUrl);
            Farmer updated = farmerRepository.save(farmer);

            log.info("Successfully uploaded avatar for farmer ID {}: {}", farmerId, avatarUrl);
            return mapToResponse(updated);
        } catch (java.io.IOException e) {
            log.error("Failed to store avatar file for farmer ID {}: {}", farmerId, e.getMessage(), e);
            throw new BadRequestException("Failed to save uploaded avatar file.");
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }

    private boolean isAllowedImageExtension(String ext) {
        return java.util.List.of(".jpg", ".jpeg", ".png", ".webp", ".gif").contains(ext);
    }

    private boolean isAllowedMimeType(String contentType) {
        if (contentType == null) return false;
        String mime = contentType.toLowerCase();
        return mime.equals("image/jpeg") || mime.equals("image/png") || mime.equals("image/webp") || mime.equals("image/gif");
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
