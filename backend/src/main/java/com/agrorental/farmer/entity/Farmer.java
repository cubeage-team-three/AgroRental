package com.agrorental.farmer.entity;

<<<<<<< HEAD
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
=======
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
import lombok.ToString;

/**
 * Extends BaseEntity for id/createdAt/updatedAt/active, per the project's
 * standing architecture rule. getFarmerId() is kept as an alias for getId()
 * because ~10 files in this module (controllers, services, DTOs) were
 * already written against a standalone farmerId field before this entity
 * was merged onto BaseEntity; migrating every call site to getId() directly
 * is a follow-up cleanup, not a blocking change.
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
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @NotBlank(message = "Mobile number is mandatory")
    @Column(name = "mobile_number", nullable = false, unique = true, length = 15)
    private String mobileNumber;

    @Column(name = "email", unique = true, length = 120)
    private String email;

    /**
     * Optional — farmers primarily authenticate via mobile OTP (see
     * FarmerOtpService), but a password is still captured at registration
     * to support a fallback login path.
     */
    @Column(name = "password")
    @ToString.Exclude
    private String password;

    @Column(name = "preferred_language", length = 50)
    @Builder.Default
    private String preferredLanguage = "English";
>>>>>>> origin/development

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "profile_image", length = 255)
    private String profileImage;

<<<<<<< HEAD
=======
    /**
     * TODO: promote to a proper enum (e.g. FarmerAccountStatus) — currently
     * a free-form string ("PENDING_OTP" / "ACTIVE") set in FarmerService and
     * FarmerOtpService with no compile-time guard against typos or drift.
     * Left as-is here since retyping it also means retyping two response
     * DTOs and every call site; not done as a drive-by inside a conflict fix.
     */
>>>>>>> origin/development
    @Column(name = "account_status", length = 30)
    @Builder.Default
    private String accountStatus = "PENDING_OTP";

<<<<<<< HEAD
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
=======
    public Long getFarmerId() {
        return getId();
>>>>>>> origin/development
    }
}
