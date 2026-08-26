package com.agrorental.farmer.entity;

import com.agrorental.common.entity.BaseEntity;
import com.agrorental.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
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
 * Entity representing a Farmer profile, linked to the core User entity for authentication.
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

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id", referencedColumnName = "id", unique = true)
    @ToString.Exclude
    private User user;

    @NotBlank(message = "Full name is mandatory")
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @NotBlank(message = "Mobile number is mandatory")
    @Column(name = "mobile_number", nullable = false, unique = true, length = 15)
    private String mobileNumber;

    @Column(name = "email", unique = true, length = 120)
    private String email;

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
