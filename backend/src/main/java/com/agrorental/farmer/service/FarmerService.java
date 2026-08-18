package com.agrorental.farmer.service;

import com.agrorental.common.exception.BadRequestException;
import com.agrorental.farmer.dto.FarmerRegisterRequest;
import com.agrorental.farmer.dto.FarmerResponse;
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
public class FarmerService {

    private final FarmerRepository farmerRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public FarmerResponse registerFarmer(FarmerRegisterRequest request) {
        log.info("Processing farmer registration for mobile: {}", request.getMobileNumber());

        if (farmerRepository.existsByMobileNumber(request.getMobileNumber().trim())) {
            log.warn("Registration failed. Mobile number {} already registered", request.getMobileNumber());
            throw new BadRequestException("Mobile number is already registered. Please login or use a different number.");
        }

        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            String email = request.getEmail().trim();
            if (farmerRepository.existsByEmail(email)) {
                log.warn("Registration failed. Email {} already registered", email);
                throw new BadRequestException("Email address is already registered to another account.");
            }
        }

        String encodedPassword = null;
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            encodedPassword = passwordEncoder.encode(request.getPassword().trim());
        }

        Farmer farmer = Farmer.builder()
                .fullName(request.getFullName().trim())
                .mobileNumber(request.getMobileNumber().trim())
                .email(request.getEmail() != null && !request.getEmail().trim().isEmpty() ? request.getEmail().trim() : null)
                .password(encodedPassword)
                .preferredLanguage(request.getPreferredLanguage() != null ? request.getPreferredLanguage() : "English")
                .accountStatus("PENDING_OTP")
                .build();

        Farmer savedFarmer = farmerRepository.save(farmer);
        log.info("Farmer registered successfully with ID: {}", savedFarmer.getFarmerId());

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

