package com.agrorental.complaint.dto;

import com.agrorental.complaint.enums.ComplaintCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintCreateRequest {

    // Not client-validated: the controller always overwrites this with the
    // authenticated farmer's own ID from the JWT before it reaches the service,
    // so a client-supplied value here is never trusted anyway.
    private Long farmerId;

    private Long bookingId;

    @NotNull(message = "Category is mandatory")
    private ComplaintCategory category;

    @NotBlank(message = "Description is mandatory")
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;
}
