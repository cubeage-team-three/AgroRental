package com.agrorental.farmer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FarmerProfileResponse {

    private Long farmerId;
    private String fullName;
    private String mobileNumber;
    private String email;
    private String address;
    private String preferredLanguage;
    private String profileImage;
    private String accountStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
