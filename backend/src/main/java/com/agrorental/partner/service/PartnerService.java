package com.agrorental.partner.service;

import com.agrorental.partner.dto.PartnerDashboardResponse;
import com.agrorental.partner.dto.PartnerRegistrationRequest;
import com.agrorental.partner.entity.Partner;
import com.agrorental.partner.repository.PartnerRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PartnerService {

    private final PartnerRepository partnerRepository;
    private final PasswordEncoder passwordEncoder;

    public PartnerService(
            PartnerRepository partnerRepository,
            PasswordEncoder passwordEncoder) {

        this.partnerRepository = partnerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Partner registerPartner(
            PartnerRegistrationRequest request) {

        if (partnerRepository.existsByMobileNumber(
                request.getMobileNumber())) {

            throw new RuntimeException(
                    "Mobile number already registered"
            );
        }

        if (request.getEmail() != null &&
                partnerRepository.existsByEmail(request.getEmail())) {

            throw new RuntimeException(
                    "Email already registered"
            );
        }

        Partner partner = Partner.builder()
                .fullName(request.getFullName())
                .businessName(request.getBusinessName())
                .mobileNumber(request.getMobileNumber())
                .email(request.getEmail())
                .address(request.getAddress())
                .gstNumber(request.getGstNumber())
                .aadhaarNumber(request.getAadhaarNumber())
                .panNumber(request.getPanNumber())
                .password(
                        passwordEncoder.encode(
                                request.getPassword()
                        )
                )
                .profilePhoto(request.getProfilePhoto())
                .otpVerified(false)
                .verificationStatus(
                        Partner.VerificationStatus.PENDING
                )
                .build();

        return partnerRepository.save(partner);
    }

    public Optional<PartnerDashboardResponse>
    getPartnerDashboard(Long id) {

        return partnerRepository.findById(id)
                .map(partner ->
                        new PartnerDashboardResponse(
                                partner.getId(),
                                partner.getFullName(),
                                partner.getBusinessName(),
                                partner.getMobileNumber(),
                                partner.getEmail(),
                                partner.getAddress(),
                                partner.isOtpVerified(),
                                partner.getVerificationStatus()
                        )
                );
    }
}