package com.jobportal.v1.service;

import com.jobportal.v1.dto.announcement.request.AnnouncementRequest;
import com.jobportal.v1.dto.announcement.response.AnnouncementResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AnnouncementService {

    // Admin methods
    AnnouncementResponse createOrUpdateAnnouncement(AnnouncementRequest request, Long adminId);
    AnnouncementResponse getAnnouncementById(Long id);
    AnnouncementResponse pinAnnouncement(Long id, Boolean pin, Long adminId);
    void deleteAnnouncement(Long id, Long adminId);
    Page<AnnouncementResponse> getAllAnnouncements(String title, String announcementType,
                                                   String targetAudience, Boolean isActive,
                                                   Pageable pageable);

    // Public methods (role-based)
    Page<AnnouncementResponse> getVisibleAnnouncements(String userRole, Pageable pageable);
    AnnouncementResponse getVisibleAnnouncementById(Long id, String userRole);
    List<AnnouncementResponse> getLatestAnnouncements(String userRole, int limit);
}