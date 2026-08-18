package com.agrorental.operator.entity;

import com.agrorental.common.entity.BaseEntity;
import com.agrorental.equipment.entity.Equipment;
import com.agrorental.partner.entity.Partner;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(
    name = "operator_jobs",
    indexes = {
        @Index(name = "idx_op_jobs_operator_status", columnList = "operator_id, status"),
        @Index(name = "idx_op_jobs_scheduled_date", columnList = "scheduled_date")
    }
)
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperatorJob extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operator_id", nullable = false)
    @ToString.Exclude
    private Operator operator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id")
    @ToString.Exclude
    private Equipment equipment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id")
    @ToString.Exclude
    private Partner partner;

    @Column(name = "job_title", nullable = false, length = 150)
    private String jobTitle;

    @Column(name = "job_type", length = 50)
    private String jobType;

    @Column(name = "job_description", columnDefinition = "TEXT")
    private String jobDescription;

    @Column(name = "work_instructions", columnDefinition = "TEXT")
    private String workInstructions;

    @Column(name = "customer_name", nullable = false, length = 100)
    private String customerName;

    @Column(name = "customer_mobile", nullable = false, length = 20)
    private String customerMobile;

    @Column(name = "work_location", nullable = false, length = 255)
    private String workLocation;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "scheduled_start_time")
    private LocalTime scheduledStartTime;

    @Column(name = "scheduled_end_time")
    private LocalTime scheduledEndTime;

    @Column(name = "estimated_duration_hours")
    private Double estimatedDurationHours;

    @Column(name = "operator_payout", nullable = false, precision = 12, scale = 2)
    private BigDecimal operatorPayout;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private JobStatus status = JobStatus.PENDING_RESPONSE;

    @Column(name = "traveling_at")
    private LocalDateTime travelingAt;

    @Column(name = "reached_location_at")
    private LocalDateTime reachedLocationAt;

    @Column(name = "work_started_at")
    private LocalDateTime workStartedAt;

    @Column(name = "work_paused_at")
    private LocalDateTime workPausedAt;

    @Column(name = "work_resumed_at")
    private LocalDateTime workResumedAt;

    @Column(name = "work_completed_at")
    private LocalDateTime workCompletedAt;

    @Column(name = "assigned_by", length = 100)
    private String assignedBy;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
