package com.agrorental.partner.dto;

import com.agrorental.partner.entity.Partner;

public record PartnerDashboardResponse(
        Long id,
        String fullName,
        String businessName,
        String mobileNumber,
        String email,
        String address,
        boolean otpVerified,
        Partner.VerificationStatus verificationStatus
) {
}