package com.agrorental.operator.service;

import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.operator.dto.OperatorProfileResponse;
import com.agrorental.operator.dto.OperatorProfileUpdateRequest;
import com.agrorental.operator.dto.OperatorRegistrationRequest;
import com.agrorental.operator.entity.Operator;
import com.agrorental.operator.entity.OperatorDocument;
import com.agrorental.operator.entity.OperatorStatus;
import com.agrorental.operator.repository.OperatorDocumentRepository;
import com.agrorental.operator.repository.OperatorRepository;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OperatorService {

    private static final List<String> ALLOWED_IMAGE_TYPES = List.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final OperatorRepository operatorRepository;
    private final OperatorDocumentRepository operatorDocumentRepository;
    private final PasswordEncoder passwordEncoder;
    private final Path photoUploadPath;

    public OperatorService(
            OperatorRepository operatorRepository,
            OperatorDocumentRepository operatorDocumentRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.upload.operator-photos-dir:uploads/operator_photos}") String photoUploadDir) {

        this.operatorRepository = operatorRepository;
        this.operatorDocumentRepository = operatorDocumentRepository;
        this.passwordEncoder = passwordEncoder;
        this.photoUploadPath = Paths.get(photoUploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(this.photoUploadPath);
            log.info("Initialized operator photos upload directory at: {}", this.photoUploadPath);
        } catch (IOException e) {
            log.error("Could not initialize operator photo upload directory: {}", e.getMessage());
            throw new RuntimeException("Could not initialize photo storage directory", e);
        }
    }

    @Transactional
    public Operator registerOperator(
            @Valid OperatorRegistrationRequest request) {

        if (operatorRepository.existsByMobileNumber(
                request.getMobileNumber())) {

            throw new IllegalArgumentException(
                    "Mobile number is already registered"
            );
        }

        Operator operator = new Operator();

        operator.setFullName(request.getFullName());
        operator.setMobileNumber(request.getMobileNumber());
        operator.setEmail(request.getEmail());
        operator.setAddress(request.getAddress());
        operator.setAadhaarNumber(request.getAadhaarNumber());
        operator.setDrivingLicenseNumber(
                request.getDrivingLicenseNumber()
        );
        operator.setExperience(request.getExperience());
        operator.setSkills(request.getSkills());

        // Store encrypted password
        operator.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        operator.setProfilePhoto(request.getProfilePhoto());

        // New operators require approval
        operator.setStatus(OperatorStatus.PENDING);

        return operatorRepository.save(operator);
    }

    @Transactional(readOnly = true)
    public OperatorProfileResponse getOperatorProfile(Long operatorId) {
        Operator operator = operatorRepository.findById(operatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found with id: " + operatorId));

        return buildProfileResponse(operator);
    }

    @Transactional
    public OperatorProfileResponse updateOperatorProfile(Long operatorId, OperatorProfileUpdateRequest request) {
        Operator operator = operatorRepository.findById(operatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found with id: " + operatorId));

        // Update allowed personal & professional fields
        operator.setFullName(request.getFullName().trim());
        operator.setEmail(request.getEmail().trim());
        operator.setAddress(request.getAddress().trim());
        operator.setExperience(request.getExperience());
        operator.setSkills(request.getSkills().trim());

        if (request.getProfilePhoto() != null && !request.getProfilePhoto().isBlank()) {
            operator.setProfilePhoto(request.getProfilePhoto().trim());
        }

        Operator updatedOperator = operatorRepository.save(operator);
        log.info("Operator profile updated successfully for ID: {}", operatorId);

        return buildProfileResponse(updatedOperator);
    }

    @Transactional
    public OperatorProfileResponse uploadProfilePhoto(Long operatorId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please select a valid image file to upload");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Unsupported image format. Allowed formats: JPG, PNG, WEBP");
        }

        // Max 5MB file size for avatar
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new BadRequestException("Profile photo must not exceed 5MB");
        }

        Operator operator = operatorRepository.findById(operatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Operator not found with id: " + operatorId));

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        if (originalFilename.contains("..")) {
            throw new BadRequestException("Invalid filename path sequence: " + originalFilename);
        }

        String fileExtension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            fileExtension = originalFilename.substring(dotIndex);
        }

        String uniqueFileName = String.format("photo_%d_%s%s",
                operator.getId(),
                UUID.randomUUID().toString().substring(0, 8),
                fileExtension
        );

        Path targetLocation = this.photoUploadPath.resolve(uniqueFileName);

        try {
            Files.createDirectories(this.photoUploadPath);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            log.error("Failed to store profile photo: {}", ex.getMessage());
            throw new RuntimeException("Failed to save profile photo on disk. Please try again.", ex);
        }

        operator.setProfilePhoto(uniqueFileName);
        Operator updatedOperator = operatorRepository.save(operator);
        log.info("Profile photo updated for operator ID: {}", operatorId);

        return buildProfileResponse(updatedOperator);
    }

    @Transactional(readOnly = true)
    public Resource loadProfilePhotoAsResource(String fileName) {
        try {
            String sanitized = StringUtils.cleanPath(fileName);
            if (sanitized.contains("..")) {
                throw new BadRequestException("Invalid photo filename sequence");
            }

            Path filePath = this.photoUploadPath.resolve(sanitized).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("Profile photo not found: " + fileName);
            }
        } catch (MalformedURLException ex) {
            throw new ResourceNotFoundException("Invalid file path for photo: " + fileName);
        }
    }

    private OperatorProfileResponse buildProfileResponse(Operator operator) {
        List<OperatorDocument> documents = operatorDocumentRepository.findAllByOperatorId(operator.getId());

        List<OperatorProfileResponse.DocumentStatusSummary> docSummaries = documents.stream()
                .map(doc -> OperatorProfileResponse.DocumentStatusSummary.builder()
                        .id(doc.getId())
                        .documentType(doc.getDocumentType())
                        .documentNumber(doc.getDocumentNumber())
                        .fileName(doc.getFileName())
                        .fileDownloadUrl("/api/operators/documents/" + doc.getId() + "/file")
                        .verificationStatus(doc.getVerificationStatus())
                        .rejectionReason(doc.getRejectionReason())
                        .verifiedAt(doc.getVerifiedAt())
                        .build())
                .collect(Collectors.toList());

        String photoUrl = null;
        if (operator.getProfilePhoto() != null && !operator.getProfilePhoto().isBlank()) {
            if (operator.getProfilePhoto().startsWith("http") || operator.getProfilePhoto().startsWith("/")) {
                photoUrl = operator.getProfilePhoto();
            } else {
                photoUrl = "/api/operators/profile/photo/" + operator.getProfilePhoto();
            }
        }

        String partnerName = null;
        if (operator.getPartner() != null) {
            partnerName = operator.getPartner().getFullName();
        }

        return OperatorProfileResponse.builder()
                .id(operator.getId())
                .fullName(operator.getFullName())
                .mobileNumber(operator.getMobileNumber())
                .email(operator.getEmail())
                .address(operator.getAddress())
                .aadhaarNumber(operator.getAadhaarNumber())
                .drivingLicenseNumber(operator.getDrivingLicenseNumber())
                .experience(operator.getExperience())
                .skills(operator.getSkills())
                .profilePhoto(operator.getProfilePhoto())
                .profilePhotoUrl(photoUrl)
                .status(operator.getStatus())
                .mobileVerified(operator.isMobileVerified())
                .documentsSubmitted(operator.isDocumentsSubmitted())
                .partnerName(partnerName)
                .createdAt(operator.getCreatedAt())
                .updatedAt(operator.getUpdatedAt())
                .documents(docSummaries)
                .build();
    }
}

