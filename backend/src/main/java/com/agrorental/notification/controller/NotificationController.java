package com.agrorental.notification.controller;

import com.agrorental.common.dto.ApiResponse;
import com.agrorental.notification.dto.NotificationResponse;
import com.agrorental.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for retrieving, updating, and deleting user notifications.
 */
@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = {
        "http://localhost:5173",
        "http://localhost:5174",
        "http://localhost:5175",
        "http://localhost:5176"
})
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUserNotifications(
            @RequestParam String role,
            @RequestParam Long id) {
        List<NotificationResponse> list = notificationService.getUserNotifications(role, id);
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved successfully", list));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            @RequestParam String role,
            @RequestParam Long id) {
        long count = notificationService.getUnreadCount(role, id);
        return ResponseEntity.ok(ApiResponse.success("Unread count fetched successfully", Map.of("unreadCount", count)));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @PathVariable Long notificationId,
            @RequestParam String role,
            @RequestParam Long id) {
        NotificationResponse res = notificationService.markAsRead(notificationId, role, id);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", res));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Map<String, String>>> markAllAsRead(
            @RequestParam String role,
            @RequestParam Long id) {
        notificationService.markAllAsRead(role, id);
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", Map.of("message", "All notifications marked as read")));
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<String>> deleteNotification(
            @PathVariable Long notificationId,
            @RequestParam String role,
            @RequestParam Long id) {
        notificationService.deleteNotification(notificationId, role, id);
        return ResponseEntity.ok(ApiResponse.success("Notification deleted successfully", "Deleted notification #" + notificationId));
    }

    @DeleteMapping("/clear-all")
    public ResponseEntity<ApiResponse<String>> clearAllNotifications(
            @RequestParam String role,
            @RequestParam Long id) {
        notificationService.clearAllNotifications(role, id);
        return ResponseEntity.ok(ApiResponse.success("All notifications cleared successfully", "All notifications cleared"));
    }
}
