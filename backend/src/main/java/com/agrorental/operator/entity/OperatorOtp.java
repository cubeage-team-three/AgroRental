package com.agrorental.operator.entity;

import com.agrorental.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "operator_otps",
    indexes = {
        @Index(name = "idx_operator_otps_mobile", columnList = "mobile_number, is_used, expires_at")
    }
)
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperatorOtp extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operator_id", nullable = false)
    @ToString.Exclude
    private Operator operator;

    @Column(name = "mobile_number", nullable = false, length = 15)
    private String mobileNumber;

    @Column(name = "otp_code", nullable = false, length = 6)
    private String otpCode;

    @Column(name = "purpose", nullable = false, length = 30)
    @Builder.Default
    private String purpose = "REGISTRATION";

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_used", nullable = false)
    @Builder.Default
    private boolean used = false;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private int attemptCount = 0;
}
