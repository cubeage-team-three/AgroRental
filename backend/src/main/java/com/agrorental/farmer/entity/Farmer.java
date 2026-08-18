package com.agrorental.farmer.entity;

import com.agrorental.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Farmers authenticate via mobile OTP, not a password — see RoleEcosystem's
 * "OTP login — no passwords needed" — so, unlike Partner/Operator, there is
 * deliberately no password field here.
 */
@Entity
@Table(
    name = "farmers",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "mobile_number"),
        @UniqueConstraint(columnNames = "email")
    }
)
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Farmer extends BaseEntity {

    @NotBlank(message = "Full name is mandatory")
    @Column(name = "full_name", nullable = false)
    private String fullName;

    @NotBlank(message = "Mobile number is mandatory")
    @Column(name = "mobile_number", nullable = false, unique = true)
    private String mobileNumber;

    @Column(unique = true)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "aadhaar_number")
    private String aadhaarNumber;

    @Column(name = "profile_photo")
    private String profilePhoto;

    @Column(name = "otp_verified", nullable = false)
    @Builder.Default
    private boolean otpVerified = false;
}
