package com.agrorental.partner.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "partners",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "mobile_number"),
        @UniqueConstraint(columnNames = "email")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Partner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "business_name")
    private String businessName;

    @Column(name = "mobile_number", nullable = false, unique = true)
    private String mobileNumber;

    @Column(unique = true)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "gst_number")
    private String gstNumber;

    @Column(name = "aadhaar_number")
    private String aadhaarNumber;

    @Column(name = "pan_number")
    private String panNumber;

    @Column(nullable = false)
    private String password;

    @Column(name = "profile_photo")
    private String profilePhoto;

    @Column(name = "otp_verified", nullable = false)
    private boolean otpVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = false)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum VerificationStatus {
        PENDING,
        APPROVED,
        REJECTED
    }
}