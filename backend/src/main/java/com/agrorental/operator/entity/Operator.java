package com.agrorental.operator.entity;

import com.agrorental.common.entity.BaseEntity;
import com.agrorental.partner.entity.Partner;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "operators",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "mobile_number")
    }
)
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Operator extends BaseEntity {

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
    @ToString.Exclude
    private String password;

    @Column(name = "profile_photo")
    private String profilePhoto;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private OperatorStatus status = OperatorStatus.PENDING;

    @Column(name = "mobile_verified", nullable = false)
    @Builder.Default
    private boolean mobileVerified = false;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id")
    @ToString.Exclude
    private Partner partner;

    @OneToMany(mappedBy = "operator", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    private List<OperatorDocument> documents = new ArrayList<>();

    public void addDocument(OperatorDocument document) {
        if (documents == null) {
            documents = new ArrayList<>();
        }
        documents.add(document);
        document.setOperator(this);
    }

    public void removeDocument(OperatorDocument document) {
        if (documents != null) {
            documents.remove(document);
            document.setOperator(null);
        }
    }
}
