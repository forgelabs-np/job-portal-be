package com.jobportal.v1.service.impl;

import com.jobportal.v1.dto.notification.request.NotificationRequest;
import com.jobportal.v1.dto.notification.response.NotificationResponse;
import com.jobportal.v1.entity.Notification;
import com.jobportal.v1.entity.User;
import com.jobportal.v1.enums.NotificationType;
import com.jobportal.v1.enums.RoleEnum;
import com.jobportal.v1.exception.ResourceNotFoundException;
import com.jobportal.v1.repository.NotificationRepository;
import com.jobportal.v1.repository.UserRepository;
import com.jobportal.v1.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void sendNotification(Long userId, NotificationType type, String title, String message, String link) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setLink(link);
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        notificationRepository.save(notification);
        log.info("Notification sent to user {}: {}", userId, title);
    }

    @Override
    @Transactional
    public void sendNotificationToUsers(NotificationRequest request) {
        if (request.getUserIds() != null && !request.getUserIds().isEmpty()) {
            for (Long userId : request.getUserIds()) {
                sendNotification(userId, request.getType(), request.getTitle(),
                        request.getMessage(), request.getLink());
            }
        }
        log.info("Notification sent to {} users", request.getUserIds() != null ? request.getUserIds().size() : 0);
    }

    @Override
    @Transactional
    public void sendNotificationToAdmins(NotificationType type, String title, String message, String link) {
        List<User> adminList = userRepository.findByRolesContaining(RoleEnum.ADMIN);
        List<User> staffList = userRepository.findByRolesContaining(RoleEnum.STAFF);

        for (User admin : adminList) {
            sendNotification(admin.getId(), type, title, message, link);
        }
        for (User staff : staffList) {
            sendNotification(staff.getId(), type, title, message, link);
        }

        log.info("Notification sent to {} admin/staff users", adminList.size() + staffList.size());
    }

    @Override
    public Page<NotificationResponse> getUserNotifications(Long userId, Boolean unreadOnly, Pageable pageable) {
        Page<Notification> notifications;

        if (Boolean.TRUE.equals(unreadOnly)) {
            notifications = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable);
        } else {
            notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }

        return notifications.map(this::mapToResponse);
    }

    @Override
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        int updated = notificationRepository.markAsRead(notificationId, userId);
        if (updated == 0) {
            throw new ResourceNotFoundException("Notification not found for user: " + notificationId);
        }
        log.info("Notification {} marked as read for user {}", notificationId, userId);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        int updated = notificationRepository.markAllAsRead(userId);
        log.info("All notifications marked as read for user {}", userId);
    }

    @Override
    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        notificationRepository.deleteByIdAndUserId(notificationId, userId);
        log.info("Notification {} deleted for user {}", notificationId, userId);
    }

    private NotificationResponse mapToResponse(Notification entity) {
        return NotificationResponse.builder()
                .id(entity.getId())
                .type(entity.getType())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .link(entity.getLink())
                .isRead(entity.getIsRead())
                .readAt(entity.getReadAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}