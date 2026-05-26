package com.jobportal.v1.entity;

import com.jobportal.v1.enums.AnnouncementType;
import com.jobportal.v1.enums.TargetAudience;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "announcements")
@Data
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "image_url")
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "announcement_type", nullable = false)
    private AnnouncementType announcementType = AnnouncementType.GENERAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_audience", nullable = false)
    private TargetAudience targetAudience = TargetAudience.ALL;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "is_pinned")
    private Boolean isPinned = false;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_by")
    private Long createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods
    public boolean isPublished() {
        return publishedAt != null && publishedAt.isBefore(LocalDateTime.now());
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isVisible() {
        return Boolean.TRUE.equals(isActive) &&
                !isExpired() &&
                (publishedAt == null || publishedAt.isBefore(LocalDateTime.now()));
    }
}