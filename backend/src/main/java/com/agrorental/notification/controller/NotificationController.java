package com.agrorental.notification.controller;

import com.agrorental.notification.dto.NotificationResponse;
import com.agrorental.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for retrieving and updating user notifications.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getUserNotifications(
            @RequestParam String role,
            @RequestParam Long id) {
        return ResponseEntity.ok(notificationService.getUserNotifications(role, id));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @RequestParam String role,
            @RequestParam Long id) {
        long count = notificationService.getUnreadCount(role, id);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable Long notificationId,
            @RequestParam String role,
            @RequestParam Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(notificationId, role, id));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead(
            @RequestParam String role,
            @RequestParam Long id) {
        notificationService.markAllAsRead(role, id);
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }
}
