package com.jobportal.v1.repository;

import com.jobportal.v1.entity.Announcement;
import com.jobportal.v1.enums.AnnouncementType;
import com.jobportal.v1.enums.TargetAudience;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    // For public viewing (active, not expired, published)
    @Query("SELECT a FROM Announcement a WHERE " +
            "a.isActive = true AND " +
            "(a.expiresAt IS NULL OR a.expiresAt > :now) AND " +
            "(a.publishedAt IS NULL OR a.publishedAt <= :now) AND " +
            "(a.targetAudience = :audience OR a.targetAudience = 'ALL') " +
            "ORDER BY a.isPinned DESC, a.publishedAt DESC, a.createdAt DESC")
    Page<Announcement> findVisibleAnnouncements(@Param("audience") TargetAudience audience,
                                                @Param("now") LocalDateTime now,
                                                Pageable pageable);

    @Query("SELECT a FROM Announcement a WHERE " +
            "(:title IS NULL OR CAST(a.title AS string) LIKE CONCAT('%', CAST(:title AS string), '%')) AND " +
            "(:announcementType IS NULL OR a.announcementType = :announcementType) AND " +
            "(:targetAudience IS NULL OR a.targetAudience = :targetAudience) AND " +
            "(:isActive IS NULL OR a.isActive = :isActive)")
    Page<Announcement> findAllWithFilters(@Param("title") String title,
                                          @Param("announcementType") AnnouncementType announcementType,
                                          @Param("targetAudience") TargetAudience targetAudience,
                                          @Param("isActive") Boolean isActive,
                                          Pageable pageable);

    // Get latest pinned announcements for dashboard
    @Query("SELECT a FROM Announcement a WHERE " +
            "a.isActive = true AND " +
            "(a.expiresAt IS NULL OR a.expiresAt > :now) AND " +
            "(a.publishedAt IS NULL OR a.publishedAt <= :now) AND " +
            "(a.targetAudience = :audience OR a.targetAudience = 'ALL') " +
            "ORDER BY a.isPinned DESC, a.publishedAt DESC, a.createdAt DESC")
    List<Announcement> findLatestAnnouncements(@Param("audience") TargetAudience audience,
                                               @Param("now") LocalDateTime now,
                                               Pageable pageable);

    long countByIsActiveTrue();
}