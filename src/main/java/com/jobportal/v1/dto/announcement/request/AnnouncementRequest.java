package com.jobportal.v1.dto.announcement.request;

import com.jobportal.v1.enums.AnnouncementType;
import com.jobportal.v1.enums.TargetAudience;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Data
public class AnnouncementRequest {
    private Long id;
    private String title;
    private String content;
    private MultipartFile imageFile;
    private String existingImageUrl;
    private AnnouncementType announcementType;
    private TargetAudience targetAudience;
    private Boolean isPinned;
    private LocalDateTime publishedAt;
    private LocalDateTime expiresAt;
}