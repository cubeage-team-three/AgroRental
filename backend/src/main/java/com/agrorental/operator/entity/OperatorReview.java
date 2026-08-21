package com.agrorental.operator.entity;

import com.agrorental.booking.entity.Booking;
import com.agrorental.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Domain entity representing a verified farmer review and 1-5 star rating for a completed operator job assignment.
 */
@Entity
@Table(
        name = "operator_reviews",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_operator_review_assignment", columnNames = "assignment_id")
        },
        indexes = {
                @Index(name = "idx_op_review_assignment", columnList = "assignment_id"),
                @Index(name = "idx_op_review_operator", columnList = "operator_id"),
                @Index(name = "idx_op_review_farmer", columnList = "farmer_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperatorReview extends BaseEntity {

    @NotNull(message = "Job assignment reference is mandatory")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignment_id", nullable = false)
    private OperatorJobAssignment assignment;

    @NotNull(message = "Operator reference is mandatory")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "operator_id", nullable = false)
    private Operator operator;

    @NotNull(message = "Booking reference is mandatory")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @NotNull(message = "Farmer ID is mandatory")
    @Column(name = "farmer_id", nullable = false)
    private Long farmerId;

    @NotNull(message = "Rating is mandatory")
    @Min(value = 1, message = "Rating must be at least 1 star")
    @Max(value = 5, message = "Rating cannot exceed 5 stars")
    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "comment", length = 1000)
    private String comment;
}
