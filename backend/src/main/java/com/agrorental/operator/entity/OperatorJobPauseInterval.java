package com.agrorental.operator.entity;

import com.agrorental.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Entity tracking individual pause and resume intervals for an active job assignment.
 * Enables accurate multi-pause work duration and earnings calculations.
 */
@Entity
@Table(
        name = "operator_job_pause_intervals",
        indexes = {
                @Index(name = "idx_pause_assignment", columnList = "assignment_id"),
                @Index(name = "idx_pause_operator", columnList = "operator_id"),
                @Index(name = "idx_pause_paused_at", columnList = "paused_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperatorJobPauseInterval extends BaseEntity {

    @NotNull(message = "Assignment reference is mandatory")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private OperatorJobAssignment assignment;

    @NotNull(message = "Operator reference is mandatory")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operator_id", nullable = false)
    private Operator operator;

    @NotNull(message = "Paused timestamp is mandatory")
    @Column(name = "paused_at", nullable = false)
    private LocalDateTime pausedAt;

    @Column(name = "resumed_at")
    private LocalDateTime resumedAt;

    @Column(name = "pause_reason", columnDefinition = "TEXT")
    private String pauseReason;

    @Column(name = "duration_minutes")
    private Long durationMinutes;
}
