package com.jobportal.v1.service;

import com.jobportal.v1.dto.notification.request.NotificationRequest;
import com.jobportal.v1.dto.notification.response.NotificationResponse;
import com.jobportal.v1.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {

    // Send notification to single user
    void sendNotification(Long userId, NotificationType type, String title, String message, String link);

    // Send notification to multiple users
    void sendNotificationToUsers(NotificationRequest request);

    // Send notification to all admin/staff users
    void sendNotificationToAdmins(NotificationType type, String title, String message, String link);

    // Get user's notifications
    Page<NotificationResponse> getUserNotifications(Long userId, Boolean unreadOnly, Pageable pageable);

    // Get unread count
    long getUnreadCount(Long userId);

    // Mark as read
    void markAsRead(Long notificationId, Long userId);

    // Mark all as read
    void markAllAsRead(Long userId);

    // Delete notification
    void deleteNotification(Long notificationId, Long userId);
}