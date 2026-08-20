package com.agrorental.complaint.dto;

import com.agrorental.complaint.enums.ComplaintCategory;
import com.agrorental.complaint.enums.ComplaintStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintResponse {

    private Long id;
    private Long farmerId;
    private Long bookingId;
    private ComplaintCategory category;
    private String description;
    private ComplaintStatus status;
    private String resolutionNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
