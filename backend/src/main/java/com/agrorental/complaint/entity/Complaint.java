package com.agrorental.complaint.entity;

import com.agrorental.common.entity.BaseEntity;
import com.agrorental.complaint.enums.ComplaintCategory;
import com.agrorental.complaint.enums.ComplaintStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * JPA Entity representing customer service issues and complaint resolution requests.
 */
@Entity
@Table(name = "complaints")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Complaint extends BaseEntity {

    @NotNull(message = "Farmer ID is mandatory")
    @Column(name = "farmer_id", nullable = false)
    private Long farmerId;

    @Column(name = "booking_id")
    private Long bookingId;

    @NotNull(message = "Complaint category is mandatory")
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private ComplaintCategory category;

    @NotNull(message = "Description is mandatory")
    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @NotNull(message = "Complaint status is mandatory")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ComplaintStatus status;

    @Column(name = "resolution_note", length = 1000)
    private String resolutionNote;
}
