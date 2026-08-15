package com.agrorental.partner.service;

import com.agrorental.partner.dto.PartnerRegistrationRequest;
import com.agrorental.partner.entity.Partner;
import com.agrorental.partner.repository.PartnerRepository;

import org.springframework.stereotype.Service;

@Service
public class PartnerService {

    private final PartnerRepository partnerRepository;

    public PartnerService(PartnerRepository partnerRepository) {
        this.partnerRepository = partnerRepository;
    }

    public Partner registerPartner(PartnerRegistrationRequest request) {

        if (partnerRepository.existsByMobileNumber(request.getMobileNumber())) {
            throw new RuntimeException("Mobile number already registered");
        }

        if (request.getEmail() != null &&
                partnerRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
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
                .password(request.getPassword())
                .profilePhoto(request.getProfilePhoto())
                .otpVerified(false)
                .verificationStatus(Partner.VerificationStatus.PENDING)
                .build();

        return partnerRepository.save(partner);
    }
}