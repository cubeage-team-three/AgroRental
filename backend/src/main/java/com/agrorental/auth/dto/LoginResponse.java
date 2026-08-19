package com.agrorental.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    private String token;
    private Long farmerId;
    private Long partnerId;
    private String businessName;
    private String fullName;
    private String mobileNumber;
    private String email;
    private String preferredLanguage;
    private String role;
    private String accountStatus;
    private String message;
}
