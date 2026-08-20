package com.agrorental.operator.dto;

import com.agrorental.operator.entity.OperatorStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Safe client-facing response DTO representing an Operator account.
 * Guarantees zero leakage of sensitive credentials or password hashes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperatorResponse {

    private Long id;
    private String fullName;
    private String mobileNumber;
    private String email;
    private String address;
    private Integer experience;
    private String skills;
    private String profilePhoto;
    private OperatorStatus status;
    private Long partnerId;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
