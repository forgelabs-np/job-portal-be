package com.jobportal.v1.controller;

import com.jobportal.v1.dto.ApiResponse;
import com.jobportal.v1.dto.PageRes;
import com.jobportal.v1.dto.notification.response.NotificationResponse;
import com.jobportal.v1.security.CurrentUser;
import com.jobportal.v1.security.UserPrincipal;
import com.jobportal.v1.service.NotificationService;
import com.jobportal.v1.util.Pages;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Notifications", description = "User Notification APIs")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Get My Notifications", description = "Get all notifications for current user")
    @GetMapping
    public ResponseEntity<ApiResponse<PageRes<NotificationResponse>>> getMyNotifications(
            @RequestParam(required = false) Boolean unreadOnly,
            @PageableDefault(size = 20) Pageable pageable,
            @CurrentUser UserPrincipal user) {

        Page<NotificationResponse> notifications = notificationService.getUserNotifications(
                user.getId(), unreadOnly, pageable);
        PageRes<NotificationResponse> response = Pages.of(notifications);

        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved", response));
    }

    @Operation(summary = "Get Unread Count", description = "Get unread notification count")
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(@CurrentUser UserPrincipal user) {
        long count = notificationService.getUnreadCount(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Unread count retrieved", count));
    }

    @Operation(summary = "Mark as Read", description = "Mark a notification as read")
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long notificationId,
            @CurrentUser UserPrincipal user) {

        notificationService.markAsRead(notificationId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
    }

    @Operation(summary = "Mark All as Read", description = "Mark all notifications as read")
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(@CurrentUser UserPrincipal user) {
        notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", null));
    }

    @Operation(summary = "Delete Notification", description = "Delete a notification")
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @PathVariable Long notificationId,
            @CurrentUser UserPrincipal user) {

        notificationService.deleteNotification(notificationId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Notification deleted", null));
    }
}