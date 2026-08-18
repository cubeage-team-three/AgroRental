package com.agrorental.farmer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "farmers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Farmer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "farmer_id")
    private Long farmerId;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "mobile_number", nullable = false, unique = true, length = 15)
    private String mobileNumber;

    @Column(name = "email", length = 120)
    private String email;

    @Column(name = "password")
    private String password;

    @Column(name = "preferred_language", length = 50)
    private String preferredLanguage;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "profile_image", length = 255)
    private String profileImage;

    @Column(name = "account_status", length = 30)
    @Builder.Default
    private String accountStatus = "PENDING_OTP";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.accountStatus == null) {
            this.accountStatus = "PENDING_OTP";
        }
        if (this.preferredLanguage == null) {
            this.preferredLanguage = "English";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
