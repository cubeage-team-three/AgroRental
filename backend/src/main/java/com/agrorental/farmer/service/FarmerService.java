package com.agrorental.farmer.service;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FarmerService {

    private final FarmerRepository farmerRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public FarmerResponse registerFarmer(FarmerRegisterRequest request) {
        log.info("Processing farmer registration for mobile: {}", request.getMobileNumber());

        String mobile = request.getMobileNumber().trim();
        if (farmerRepository.existsByMobileNumber(mobile)) {
            log.warn("Registration failed. Mobile number {} already registered", mobile);
            throw new BadRequestException("Mobile number is already registered. Please login or use a different number.");
        }

        String rawEmail = (request.getEmail() != null && !request.getEmail().trim().isEmpty()) ? request.getEmail().trim() : null;
        if (rawEmail != null && farmerRepository.existsByEmail(rawEmail)) {
            log.warn("Registration failed. Email {} already registered", rawEmail);
            throw new BadRequestException("Email address is already registered to another account.");
        }

        String userEmail = rawEmail != null ? rawEmail.toLowerCase() : mobile + "@farmer.agrorent.in";
        if (userRepository.existsByEmail(userEmail)) {
            log.warn("Registration failed. User email {} already exists in users repository", userEmail);
            throw new BadRequestException("An account with this email/mobile already exists.");
        }

        String rawPassword = (request.getPassword() != null && !request.getPassword().trim().isEmpty()) ? request.getPassword().trim() : null;
        String encodedPassword = passwordEncoder.encode(rawPassword != null ? rawPassword : UUID.randomUUID().toString());

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
                .password(rawPassword != null ? encodedPassword : null)
                .preferredLanguage(request.getPreferredLanguage() != null ? request.getPreferredLanguage() : "English")
                .accountStatus("PENDING_OTP")
                .build();

        Farmer savedFarmer = farmerRepository.save(farmer);
        log.info("Farmer registered successfully with ID: {}, linked to User ID: {}", savedFarmer.getFarmerId(), savedUser.getId());

        return FarmerResponse.builder()
                .farmerId(savedFarmer.getFarmerId())
                .fullName(savedFarmer.getFullName())
                .mobileNumber(savedFarmer.getMobileNumber())
                .email(savedFarmer.getEmail())
                .preferredLanguage(savedFarmer.getPreferredLanguage())
                .accountStatus(savedFarmer.getAccountStatus())
                .createdAt(savedFarmer.getCreatedAt())
                .build();
    }
}


