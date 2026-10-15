package com.agrorental.farmer.service;

import com.agrorental.auth.service.AccountIdentifiers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.agrorental.auth.service.PasswordPolicy;
import com.agrorental.common.enums.Role;
import com.agrorental.common.exception.BadRequestException;
import com.agrorental.farmer.dto.FarmerRegisterRequest;
import com.agrorental.farmer.dto.FarmerResponse;
import com.agrorental.farmer.entity.Farmer;
import com.agrorental.farmer.repository.FarmerRepository;
import com.agrorental.user.entity.User;
import com.agrorental.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FarmerService {

    private final FarmerRepository farmerRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Registers a farmer with a password (no OTP step). The account is ACTIVE immediately and can log in with
     * its mobile number (or email) and password.
     */
    @Transactional
    public FarmerResponse registerFarmer(FarmerRegisterRequest request) {
        log.info("Processing farmer registration for mobile: {}", AccountIdentifiers.mask(request.getMobileNumber()));

        String rawPassword = request.getPassword();
        PasswordPolicy.validate(rawPassword);

        String mobile = request.getMobileNumber().trim();
        if (farmerRepository.existsByMobileNumber(mobile)) {
            log.warn("Registration failed. Mobile number {} already registered", AccountIdentifiers.mask(mobile));
            throw new BadRequestException("Mobile number is already registered. Please login or use a different number.");
        }

        String rawEmail = (request.getEmail() != null && !request.getEmail().trim().isEmpty()) ? request.getEmail().trim() : null;
        if (rawEmail != null && farmerRepository.existsByEmail(rawEmail)) {
            log.warn("Registration failed. Email {} already registered", AccountIdentifiers.mask(rawEmail));
            throw new BadRequestException("Email address is already registered to another account.");
        }

        String userEmail = rawEmail != null ? rawEmail.toLowerCase() : mobile + "@farmer.agrorent.in";
        if (userRepository.existsByEmail(userEmail)) {
            log.warn("Registration failed. User email {} already exists in users repository", AccountIdentifiers.mask(userEmail));
            throw new BadRequestException("An account with this email/mobile already exists.");
        }

        String encodedPassword = passwordEncoder.encode(rawPassword);

        User user = User.builder()
                .name(request.getFullName().trim())
                .email(userEmail)
                .password(encodedPassword)
                .role(Role.FARMER)
                .enabled(true)
                .verified(false)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Created core User entity with ID: {} for farmer", savedUser.getId());

        Farmer farmer = Farmer.builder()
                .user(savedUser)
                .fullName(request.getFullName().trim())
                .mobileNumber(mobile)
                .email(rawEmail)
                .password(encodedPassword)
                .preferredLanguage(request.getPreferredLanguage() != null ? request.getPreferredLanguage() : "English")
                .accountStatus("ACTIVE")
                .build();

        Farmer savedFarmer = farmerRepository.save(farmer);
        log.info("Farmer registered successfully with ID: {}, linked to User ID: {}", savedFarmer.getFarmerId(), savedUser.getId());

        return mapToResponse(savedFarmer);
    }

    @Transactional(readOnly = true)
    public Page<FarmerResponse> getFarmers(String search, String status, Pageable pageable) {
        log.info("Fetching farmers with search: '{}', status: '{}', pagination: page={}, size={}", 
                search, status, pageable.getPageNumber(), pageable.getPageSize());

        org.springframework.data.jpa.domain.Specification<Farmer> spec = (root, query, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();

            if (search != null && !search.trim().isEmpty()) {
                String likePattern = "%" + search.trim().toLowerCase() + "%";
                jakarta.persistence.criteria.Predicate nameMatch = cb.like(cb.lower(root.get("fullName")), likePattern);
                jakarta.persistence.criteria.Predicate phoneMatch = cb.like(root.get("mobileNumber"), likePattern);
                jakarta.persistence.criteria.Predicate emailMatch = cb.like(cb.lower(root.get("email")), likePattern);
                predicates.add(cb.or(nameMatch, phoneMatch, emailMatch));
            }

            if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL")) {
                if (status.equalsIgnoreCase("PENDING")) {
                    predicates.add(cb.not(root.get("accountStatus").in("ACTIVE", "KYC_COMPLETED")));
                } else if (status.equalsIgnoreCase("KYC REVIEW")) {
                    predicates.add(cb.equal(root.get("accountStatus"), "KYC_COMPLETED"));
                } else {
                    predicates.add(cb.equal(root.get("accountStatus"), status.toUpperCase()));
                }
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return farmerRepository.findAll(spec, pageable).map(this::mapToResponse);
    }

    @Transactional
    public FarmerResponse approveFarmerKyc(Long farmerId) {
        log.info("Approving KYC for farmer ID: {}", farmerId);

        Farmer farmer = farmerRepository.findById(farmerId)
                .orElseThrow(() -> new IllegalArgumentException("Farmer not found with ID: " + farmerId));

        if (!"KYC_COMPLETED".equals(farmer.getAccountStatus())) {
            throw new IllegalStateException(
                    "Cannot approve KYC. Farmer status must be 'KYC_COMPLETED' but is '" + 
                    farmer.getAccountStatus() + "'");
        }

        farmer.setAccountStatus("ACTIVE");
        Farmer updatedFarmer = farmerRepository.save(farmer);

        log.info("Successfully approved KYC for farmer ID: {}", farmerId);
        return mapToResponse(updatedFarmer);
    }

    @Transactional
    public FarmerResponse rejectFarmerKyc(Long farmerId) {
        log.info("Rejecting KYC for farmer ID: {}", farmerId);

        Farmer farmer = farmerRepository.findById(farmerId)
                .orElseThrow(() -> new IllegalArgumentException("Farmer not found with ID: " + farmerId));

        if (!"KYC_COMPLETED".equals(farmer.getAccountStatus())) {
            throw new IllegalStateException(
                    "Cannot reject KYC. Farmer status must be 'KYC_COMPLETED' but is '" + 
                    farmer.getAccountStatus() + "'");
        }

        farmer.setAccountStatus("REJECTED");
        Farmer updatedFarmer = farmerRepository.save(farmer);

        log.info("Successfully rejected KYC for farmer ID: {}", farmerId);
        return mapToResponse(updatedFarmer);
    }

    // Helper method to safely map Entity to DTO
    private FarmerResponse mapToResponse(Farmer farmer) {
        return FarmerResponse.builder()
                .farmerId(farmer.getFarmerId())
                .fullName(farmer.getFullName())
                .mobileNumber(farmer.getMobileNumber())
                .email(farmer.getEmail())
                .preferredLanguage(farmer.getPreferredLanguage())
                .accountStatus(farmer.getAccountStatus())
                .address(farmer.getAddress())
                .createdAt(farmer.getCreatedAt())
                .build();
    }
}