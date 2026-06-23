package com.jobportal.v1.dto.notification.request;

import com.jobportal.v1.enums.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class NotificationRequest {
    private Long userId;
    private List<Long> userIds;
    private NotificationType type;
    private String title;
    private String message;
    private String link;
    private String metadata;
}