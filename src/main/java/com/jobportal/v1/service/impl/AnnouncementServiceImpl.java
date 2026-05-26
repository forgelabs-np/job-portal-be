package com.jobportal.v1.service.impl;

import com.jobportal.v1.dto.announcement.request.AnnouncementRequest;
import com.jobportal.v1.dto.announcement.response.AnnouncementResponse;
import com.jobportal.v1.entity.Announcement;
import com.jobportal.v1.entity.User;
import com.jobportal.v1.enums.AnnouncementType;
import com.jobportal.v1.enums.TargetAudience;
import com.jobportal.v1.exception.BadRequestException;
import com.jobportal.v1.exception.ResourceNotFoundException;
import com.jobportal.v1.repository.AnnouncementRepository;
import com.jobportal.v1.repository.UserRepository;
import com.jobportal.v1.service.AnnouncementService;
import com.jobportal.v1.util.FileUploadUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;
    private final FileUploadUtil fileUploadUtil;


    @Override
    @Transactional
    public AnnouncementResponse createOrUpdateAnnouncement(AnnouncementRequest request, Long adminId) {
        Announcement announcement;
        boolean isUpdate = false;

        if (request.getId() != null && request.getId() > 0) {
            announcement = announcementRepository.findById(request.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Announcement not found with id: " + request.getId()));
            isUpdate = true;
            log.info("Updating announcement: {} by admin: {}", request.getId(), adminId);
        } else {
            announcement = new Announcement();
            announcement.setCreatedBy(adminId);
            announcement.setIsActive(true);
            log.info("Creating new announcement by admin: {}", adminId);
        }

        // Validate title and content
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new BadRequestException("Title is required");
        }
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new BadRequestException("Content is required");
        }

        announcement.setTitle(request.getTitle());
        announcement.setContent(request.getContent());
        announcement.setAnnouncementType(request.getAnnouncementType() != null ?
                request.getAnnouncementType() : AnnouncementType.GENERAL);
        announcement.setTargetAudience(request.getTargetAudience() != null ?
                request.getTargetAudience() : TargetAudience.ALL);

        if (request.getIsPinned() != null) {
            announcement.setIsPinned(request.getIsPinned());
        }

        // ✅ Handle publishedAt - default to NOW if not provided
        if (request.getPublishedAt() != null) {
            announcement.setPublishedAt(request.getPublishedAt());
        } else if (!isUpdate) {
            // For new announcements without publishedAt, publish immediately
            announcement.setPublishedAt(LocalDateTime.now());
        }
        // For updates: if publishedAt is null, keep existing value (do nothing)

        announcement.setExpiresAt(request.getExpiresAt());

        // Validate dates
        if (announcement.getExpiresAt() != null && announcement.getPublishedAt() != null &&
                announcement.getExpiresAt().isBefore(announcement.getPublishedAt())) {
            throw new BadRequestException("Expiry date must be after published date");
        }

        // ✅ Handle image upload
        if (request.getImageFile() != null && !request.getImageFile().isEmpty()) {
            try {
                if (isUpdate && announcement.getImageUrl() != null) {
                    fileUploadUtil.deleteFile(announcement.getImageUrl());
                }

                if (!isUpdate) {
                    Announcement tempSaved = announcementRepository.save(announcement);
                    String imagePath = fileUploadUtil.uploadAnnouncementImage(tempSaved.getId(), request.getImageFile());
                    tempSaved.setImageUrl(imagePath);
                    announcement = tempSaved;
                } else {
                    String imagePath = fileUploadUtil.uploadAnnouncementImage(announcement.getId(), request.getImageFile());
                    announcement.setImageUrl(imagePath);
                }
            } catch (IOException e) {
                log.error("Failed to upload announcement image", e);
                throw new RuntimeException("Failed to upload announcement image: " + e.getMessage(), e);
            }
        }

        Announcement saved = announcementRepository.save(announcement);
        log.info("Announcement {}: {} by admin: {}", isUpdate ? "updated" : "created",
                saved.getId(), adminId);

        return mapToResponse(saved);
    }

    @Override
    public AnnouncementResponse getAnnouncementById(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found with id: " + id));
        return mapToResponse(announcement);
    }

    @Override
    @Transactional
    public AnnouncementResponse pinAnnouncement(Long id, Boolean pin, Long adminId) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found with id: " + id));

        announcement.setIsPinned(pin);
        Announcement saved = announcementRepository.save(announcement);
        log.info("Announcement {} {} by admin: {}", id, pin ? "pinned" : "unpinned", adminId);

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteAnnouncement(Long id, Long adminId) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found with id: " + id));

        announcementRepository.delete(announcement);
        log.info("Announcement deleted: {} by admin: {}", id, adminId);
    }

    @Override
    public Page<AnnouncementResponse> getAllAnnouncements(String title, String announcementType,
                                                          String targetAudience, Boolean isActive,
                                                          Pageable pageable) {
        AnnouncementType type = null;
        TargetAudience audience = null;

        if (announcementType != null && !announcementType.isEmpty()) {
            try {
                type = AnnouncementType.valueOf(announcementType.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid announcementType. Allowed: GENERAL, JOB_ALERT, SYSTEM_UPDATE, POLICY_CHANGE, EVENT");
            }
        }

        if (targetAudience != null && !targetAudience.isEmpty()) {
            try {
                audience = TargetAudience.valueOf(targetAudience.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid targetAudience. Allowed: ALL, AGENCY_ONLY, CANDIDATE_ONLY");
            }
        }

        return announcementRepository.findAllWithFilters(title, type, audience, isActive, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public Page<AnnouncementResponse> getVisibleAnnouncements(String userRole, Pageable pageable) {
        TargetAudience audience = determineAudience(userRole);
        return announcementRepository.findVisibleAnnouncements(audience, LocalDateTime.now(), pageable)
                .map(this::mapToResponse);
    }

    @Override
    public AnnouncementResponse getVisibleAnnouncementById(Long id, String userRole) {
        TargetAudience audience = determineAudience(userRole);
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found with id: " + id));

        // Check if user can view this announcement
        if (!announcement.isVisible()) {
            throw new ResourceNotFoundException("Announcement not available");
        }

        if (announcement.getTargetAudience() != TargetAudience.ALL &&
                announcement.getTargetAudience() != audience) {
            throw new ResourceNotFoundException("Announcement not available for your role");
        }

        return mapToResponse(announcement);
    }

    @Override
    public List<AnnouncementResponse> getLatestAnnouncements(String userRole, int limit) {
        TargetAudience audience = determineAudience(userRole);
        Pageable pageable = PageRequest.of(0, limit);

        return announcementRepository.findLatestAnnouncements(audience, LocalDateTime.now(), pageable)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private TargetAudience determineAudience(String userRole) {
        if (userRole == null || userRole.isEmpty()) {
            return TargetAudience.ALL;
        }

        String role = userRole.toUpperCase();
        if (role.equals("AGENCY")) {
            return TargetAudience.AGENCY_ONLY;
        } else if (role.equals("CANDIDATE")) {
            return TargetAudience.CANDIDATE_ONLY;
        }

        return TargetAudience.ALL;
    }

    private AnnouncementResponse mapToResponse(Announcement entity) {
        String createdByName = null;
        if (entity.getCreatedBy() != null) {
            Optional<User> user = userRepository.findById(entity.getCreatedBy());
            if (user.isPresent()) {
                createdByName = user.get().getFullName();
            }
        }

        return AnnouncementResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .imageUrl(entity.getImageUrl())
                .announcementType(entity.getAnnouncementType())
                .targetAudience(entity.getTargetAudience())
                .isActive(entity.getIsActive())
                .isPinned(entity.getIsPinned())
                .isVisible(entity.isVisible())
                .publishedAt(entity.getPublishedAt())
                .expiresAt(entity.getExpiresAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .createdByName(createdByName)
                .build();
    }
}