package com.agrorental.notification.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * Data Transfer Object representing a user notification payload.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private Long id;
    private String recipientRole;
    private Long recipientId;
    private String title;
    private String message;
    private String notificationType;
    private Long bookingId;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
