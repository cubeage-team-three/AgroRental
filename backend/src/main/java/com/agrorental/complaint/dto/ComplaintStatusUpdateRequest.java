package com.agrorental.complaint.dto;

import com.agrorental.complaint.enums.ComplaintStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintStatusUpdateRequest {

    @NotNull(message = "Status is mandatory")
    private ComplaintStatus status;

    @Size(max = 1000, message = "Resolution note cannot exceed 1000 characters")
    private String resolutionNote;
}
