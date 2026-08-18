package com.agrorental.partner.dto;

import com.agrorental.partner.entity.Partner;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartnerProfileResponse {

    private Long id;
    private String fullName;
    private String businessName;
    private String mobileNumber;
    private String email;
    private String address;
    private String gstNumber;
    private String aadhaarNumber;
    private String panNumber;
    private String profilePhoto;
    private boolean otpVerified;
    private Partner.VerificationStatus verificationStatus;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
