package com.agrorental.notification;

import com.agrorental.common.exception.BadRequestException;
import com.agrorental.common.exception.ResourceNotFoundException;
import com.agrorental.notification.dto.NotificationResponse;
import com.agrorental.notification.entity.Notification;
import com.agrorental.notification.repository.NotificationRepository;
import com.agrorental.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private Notification testNotification;

    @BeforeEach
    void setUp() {
        testNotification = Notification.builder()
                .id(1L)
                .recipientRole("FARMER")
                .recipientId(100L)
                .title("Booking Confirmed")
                .message("Your booking #500 for Mahindra 575 DI Tractor has been confirmed.")
                .notificationType("BOOKING_CONFIRMED")
                .bookingId(500L)
                .isRead(false)
                .build();
    }

    @Test
    @DisplayName("sendNotification: Saves and returns notification response DTO")
    void sendNotification_Success() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        NotificationResponse response = notificationService.sendNotification(
                "FARMER", 100L, "Booking Confirmed", "Your booking #500 has been confirmed.", "BOOKING_CONFIRMED", 500L
        );

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("FARMER", response.getRecipientRole());
        assertEquals(100L, response.getRecipientId());
        assertEquals("BOOKING_CONFIRMED", response.getNotificationType());
        assertFalse(response.getIsRead());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("getUserNotifications: Retrieves recipient-filtered notifications")
    void getUserNotifications_Success() {
        when(notificationRepository.findByRecipientRoleAndRecipientIdOrderByCreatedAtDesc("FARMER", 100L))
                .thenReturn(List.of(testNotification));

        List<NotificationResponse> result = notificationService.getUserNotifications("farmer", 100L);

        assertEquals(1, result.size());
        assertEquals("Booking Confirmed", result.get(0).getTitle());
    }

    @Test
    @DisplayName("getUnreadCount: Returns exact count of unread notifications")
    void getUnreadCount_Success() {
        when(notificationRepository.countByRecipientRoleAndRecipientIdAndIsReadFalse("FARMER", 100L))
                .thenReturn(3L);

        long count = notificationService.getUnreadCount("FARMER", 100L);

        assertEquals(3L, count);
    }

    @Test
    @DisplayName("markAsRead: Successfully marks notification read for authorized recipient")
    void markAsRead_Success() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        NotificationResponse response = notificationService.markAsRead(1L, "FARMER", 100L);

        assertNotNull(response);
        assertTrue(testNotification.getIsRead());
        verify(notificationRepository).save(testNotification);
    }

    @Test
    @DisplayName("markAsRead: Throws BadRequestException when unauthorized recipient attempts access")
    void markAsRead_RecipientMismatch_ThrowsException() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> notificationService.markAsRead(1L, "FARMER", 999L));

        assertTrue(exception.getMessage().contains("Unauthorized access"));
    }

    @Test
    @DisplayName("markAllAsRead: Calls repository bulk update for recipient")
    void markAllAsRead_Success() {
        notificationService.markAllAsRead("PARTNER", 10L);

        verify(notificationRepository).markAllAsRead("PARTNER", 10L);
    }
}
