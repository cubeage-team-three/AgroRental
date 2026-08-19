package com.agrorental.notification.service;

import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.notification.dto.NotificationResponse;
import com.agrorental.notification.entity.Notification;
import com.agrorental.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service managing user notifications and state updates.
 */
@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Helper method to send a new notification.
     */
    public NotificationResponse sendNotification(String recipientRole, Long recipientId, String title, String message, String type, Long bookingId) {
        Notification notification = Notification.builder()
                .recipientRole(recipientRole.toUpperCase())
                .recipientId(recipientId)
                .title(title)
                .message(message)
                .notificationType(type)
                .bookingId(bookingId)
                .isRead(false)
                .build();

        Notification saved = notificationRepository.save(notification);
        return mapToResponse(saved);
    }

    /**
     * Retrieves notifications for a specific recipient role & ID.
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUserNotifications(String recipientRole, Long recipientId) {
        return notificationRepository.findByRecipientRoleAndRecipientIdOrderByCreatedAtDesc(recipientRole.toUpperCase(), recipientId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Returns the count of unread notifications for a user.
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(String recipientRole, Long recipientId) {
        return notificationRepository.countByRecipientRoleAndRecipientIdAndIsReadFalse(recipientRole.toUpperCase(), recipientId);
    }

    /**
     * Marks a specific notification as read after verifying recipient security ownership.
     */
    public NotificationResponse markAsRead(Long id, String recipientRole, Long recipientId) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with ID: " + id));

        if (!notification.getRecipientRole().equalsIgnoreCase(recipientRole) ||
                !notification.getRecipientId().equals(recipientId)) {
            throw new BadRequestException("Unauthorized access to notification #" + id);
        }

        notification.setIsRead(true);
        Notification saved = notificationRepository.save(notification);
        return mapToResponse(saved);
    }

    /**
     * Marks all notifications as read for a recipient.
     */
    public void markAllAsRead(String recipientRole, Long recipientId) {
        notificationRepository.markAllAsRead(recipientRole.toUpperCase(), recipientId);
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .recipientRole(notification.getRecipientRole())
                .recipientId(notification.getRecipientId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .notificationType(notification.getNotificationType())
                .bookingId(notification.getBookingId())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
