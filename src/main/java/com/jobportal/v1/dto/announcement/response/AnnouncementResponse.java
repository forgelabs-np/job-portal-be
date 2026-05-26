package com.jobportal.v1.dto.announcement.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jobportal.v1.enums.AnnouncementType;
import com.jobportal.v1.enums.TargetAudience;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AnnouncementResponse {
    private Long id;
    private String title;
    private String content;
    private String imageUrl;
    private AnnouncementType announcementType;
    private TargetAudience targetAudience;
    private Boolean isActive;
    private Boolean isPinned;
    private Boolean isVisible;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiresAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    private Long createdBy;
    private String createdByName;
}