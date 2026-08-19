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

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "profile_image", length = 255)
    private String profileImage;

    @Column(name = "account_status", length = 30)
    @Builder.Default
    private String accountStatus = "PENDING_OTP";

    public Long getFarmerId() {
        return getId();
    }
}
