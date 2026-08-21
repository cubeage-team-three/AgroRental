package com.agrorental.operator.entity;

import com.agrorental.booking.entity.Booking;
import com.agrorental.common.entity.BaseEntity;
import com.agrorental.operator.enums.OperatorAssignmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Domain entity representing the assignment and active work lifecycle of a qualified machinery Operator to a Booking.
 */
@Entity
@Table(
        name = "operator_job_assignments",
        indexes = {
                @Index(name = "idx_assignment_operator", columnList = "operator_id"),
                @Index(name = "idx_assignment_booking", columnList = "booking_id"),
                @Index(name = "idx_assignment_status", columnList = "assignment_status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperatorJobAssignment extends BaseEntity {

    @NotNull(message = "Operator reference is mandatory")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operator_id", nullable = false)
    private Operator operator;

    @NotNull(message = "Booking reference is mandatory")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @NotNull(message = "Assignment status is mandatory")
    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_status", nullable = false, length = 30)
    private OperatorAssignmentStatus assignmentStatus;

    @NotNull(message = "Assigned timestamp is mandatory")
    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @Column(name = "assigned_by", length = 100)
    private String assignedBy;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // ==========================================
    // PHASE 5: WORK LIFECYCLE AUDITING FIELDS
    // ==========================================

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "traveling_at")
    private LocalDateTime travelingAt;

    @Column(name = "reached_at")
    private LocalDateTime reachedAt;

    @Column(name = "work_started_at")
    private LocalDateTime workStartedAt;

    @Column(name = "paused_at")
    private LocalDateTime pausedAt;

    @Column(name = "pause_reason", columnDefinition = "TEXT")
    private String pauseReason;

    @Column(name = "resumed_at")
    private LocalDateTime resumedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "completion_notes", columnDefinition = "TEXT")
    private String completionNotes;
}
