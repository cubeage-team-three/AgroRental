package com.agrorental.notification.controller;

import com.agrorental.common.dto.ApiResponse;
import com.agrorental.notification.dto.NotificationResponse;
import com.agrorental.notification.service.NotificationService;
import com.agrorental.security.principal.FarmerPrincipal;
import com.agrorental.security.principal.OperatorPrincipal;
import com.agrorental.security.principal.PartnerPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for retrieving, updating, and deleting user notifications.
 *
 * <p>The recipient (role + account ID) is always derived from the authenticated principal. The legacy
 * {@code role} and {@code id} query parameters are still accepted for frontend compatibility but are
 * ignored: they never select whose notifications are read or modified.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /** Recipient identity of the caller, resolved from the JWT principal only. */
    record Recipient(String role, Long id) {
    }

    static Recipient resolveRecipient(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required.");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof FarmerPrincipal farmer) {
            return new Recipient("FARMER", farmer.getId());
        }
        if (principal instanceof PartnerPrincipal partner) {
            return new Recipient("PARTNER", partner.getId());
        }
        if (principal instanceof OperatorPrincipal operator) {
            return new Recipient("OPERATOR", operator.getId());
        }
        // Admin principals are "ADMIN_<admins.id>" or "ADMIN_USER_<users.id>"; keep the two ID spaces apart
        if (principal instanceof String name
                && authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) {
            if (name.startsWith("ADMIN_USER_")) {
                return new Recipient("ADMIN_USER", Long.valueOf(name.substring("ADMIN_USER_".length())));
            }
            if (name.startsWith("ADMIN_")) {
                return new Recipient("ADMIN", Long.valueOf(name.substring("ADMIN_".length())));
            }
        }
        throw new AccessDeniedException("Unsupported principal for notifications.");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUserNotifications(
            @RequestParam(name = "role", required = false) String ignoredRole,
            @RequestParam(name = "id", required = false) Long ignoredId,
            Authentication authentication) {
        Recipient recipient = resolveRecipient(authentication);
        List<NotificationResponse> list = notificationService.getUserNotifications(recipient.role(), recipient.id());
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved successfully", list));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            @RequestParam(name = "role", required = false) String ignoredRole,
            @RequestParam(name = "id", required = false) Long ignoredId,
            Authentication authentication) {
        Recipient recipient = resolveRecipient(authentication);
        long count = notificationService.getUnreadCount(recipient.role(), recipient.id());
        return ResponseEntity.ok(ApiResponse.success("Unread count fetched successfully", Map.of("unreadCount", count)));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @PathVariable Long notificationId,
            @RequestParam(name = "role", required = false) String ignoredRole,
            @RequestParam(name = "id", required = false) Long ignoredId,
            Authentication authentication) {
        Recipient recipient = resolveRecipient(authentication);
        NotificationResponse res = notificationService.markAsRead(notificationId, recipient.role(), recipient.id());
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", res));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Map<String, String>>> markAllAsRead(
            @RequestParam(name = "role", required = false) String ignoredRole,
            @RequestParam(name = "id", required = false) Long ignoredId,
            Authentication authentication) {
        Recipient recipient = resolveRecipient(authentication);
        notificationService.markAllAsRead(recipient.role(), recipient.id());
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", Map.of("message", "All notifications marked as read")));
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<String>> deleteNotification(
            @PathVariable Long notificationId,
            @RequestParam(name = "role", required = false) String ignoredRole,
            @RequestParam(name = "id", required = false) Long ignoredId,
            Authentication authentication) {
        Recipient recipient = resolveRecipient(authentication);
        notificationService.deleteNotification(notificationId, recipient.role(), recipient.id());
        return ResponseEntity.ok(ApiResponse.success("Notification deleted successfully", "Deleted notification #" + notificationId));
    }

    @DeleteMapping("/clear-all")
    public ResponseEntity<ApiResponse<String>> clearAllNotifications(
            @RequestParam(name = "role", required = false) String ignoredRole,
            @RequestParam(name = "id", required = false) Long ignoredId,
            Authentication authentication) {
        Recipient recipient = resolveRecipient(authentication);
        notificationService.clearAllNotifications(recipient.role(), recipient.id());
        return ResponseEntity.ok(ApiResponse.success("All notifications cleared successfully", "All notifications cleared"));
    }
}
