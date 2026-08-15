package com.agrorental.operator.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "operators",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "mobile_number")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Operator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "mobile_number", nullable = false, unique = true)
    private String mobileNumber;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "aadhaar_number", nullable = false)
    private String aadhaarNumber;

    @Column(name = "driving_license_number", nullable = false)
    private String drivingLicenseNumber;

    @Column(name = "experience", nullable = false)
    private Integer experience;

    @Column(name = "skills", nullable = false)
    private String skills;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "profile_photo")
    private String profilePhoto;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private OperatorStatus status = OperatorStatus.PENDING;
}