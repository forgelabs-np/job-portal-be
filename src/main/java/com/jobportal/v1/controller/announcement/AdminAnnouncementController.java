package com.jobportal.v1.controller.announcement;

import com.jobportal.v1.dto.ApiResponse;
import com.jobportal.v1.dto.PageRes;
import com.jobportal.v1.dto.announcement.request.AnnouncementRequest;
import com.jobportal.v1.dto.announcement.response.AnnouncementResponse;
import com.jobportal.v1.security.CurrentUser;
import com.jobportal.v1.security.UserPrincipal;
import com.jobportal.v1.service.AnnouncementService;
import com.jobportal.v1.util.Pages;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/announcements")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
@Tag(name = "Admin - Announcements", description = "Admin Announcement Management APIs")
public class AdminAnnouncementController {

    private final AnnouncementService announcementService;

    @Operation(summary = "Create or Update Announcement",
            description = "Create new announcement (without id) or update existing (with id). Can upload image file directly.")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AnnouncementResponse>> createOrUpdateAnnouncement(
            @Valid @ModelAttribute AnnouncementRequest request,
            @CurrentUser UserPrincipal admin) {

        AnnouncementResponse response = announcementService.createOrUpdateAnnouncement(request, admin.getId());
        String message = request.getId() == null ? "Announcement created successfully" : "Announcement updated successfully";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @Operation(summary = "Get All Announcements", description = "Get all announcements with filters")
    @GetMapping
    public ResponseEntity<ApiResponse<PageRes<AnnouncementResponse>>> getAllAnnouncements(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String announcementType,
            @RequestParam(required = false) String targetAudience,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<AnnouncementResponse> announcements = announcementService.getAllAnnouncements(
                title, announcementType, targetAudience, isActive, pageable);
        PageRes<AnnouncementResponse> response = Pages.of(announcements);

        return ResponseEntity.ok(ApiResponse.success("Announcements retrieved", response));
    }

    @Operation(summary = "Get Announcement by ID", description = "Get single announcement details")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> getAnnouncementById(@PathVariable Long id) {
        AnnouncementResponse response = announcementService.getAnnouncementById(id);
        return ResponseEntity.ok(ApiResponse.success("Announcement retrieved", response));
    }

    @Operation(summary = "Pin/Unpin Announcement", description = "Pin or unpin an announcement")
    @PatchMapping("/{id}/pin")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> pinAnnouncement(
            @PathVariable Long id,
            @RequestParam Boolean pin,
            @CurrentUser UserPrincipal admin) {

        AnnouncementResponse response = announcementService.pinAnnouncement(id, pin, admin.getId());
        String message = pin ? "Announcement pinned successfully" : "Announcement unpinned successfully";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @Operation(summary = "Delete Announcement", description = "Delete an announcement")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAnnouncement(
            @PathVariable Long id,
            @CurrentUser UserPrincipal admin) {

        announcementService.deleteAnnouncement(id, admin.getId());
        return ResponseEntity.ok(ApiResponse.success("Announcement deleted successfully", null));
    }
}