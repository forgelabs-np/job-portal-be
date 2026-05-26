package com.jobportal.v1.controller;

import com.jobportal.v1.dto.ApiResponse;
import com.jobportal.v1.dto.PageRes;
import com.jobportal.v1.dto.announcement.response.AnnouncementResponse;
import com.jobportal.v1.security.CurrentUser;
import com.jobportal.v1.security.UserPrincipal;
import com.jobportal.v1.service.AnnouncementService;
import com.jobportal.v1.util.Pages;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/announcements")
@PreAuthorize("isAuthenticated()")
@Tag(name = "Announcements", description = "Announcement View APIs (All authenticated users)")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @Operation(summary = "Get Visible Announcements",
            description = "Get all active announcements based on user role")
    @GetMapping
    public ResponseEntity<ApiResponse<PageRes<AnnouncementResponse>>> getVisibleAnnouncements(
            @PageableDefault(size = 20) Pageable pageable,
            @CurrentUser UserPrincipal user) {

        String userRole = user.getRoles().stream().findFirst().orElse("");

        Page<AnnouncementResponse> announcements = announcementService.getVisibleAnnouncements(userRole, pageable);
        PageRes<AnnouncementResponse> response = Pages.of(announcements);

        return ResponseEntity.ok(ApiResponse.success("Announcements retrieved", response));
    }

    @Operation(summary = "Get Announcement by ID", description = "Get single announcement (only if visible to user)")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AnnouncementResponse>> getAnnouncementById(
            @PathVariable Long id,
            @CurrentUser UserPrincipal user) {

        String userRole = user.getRoles().stream().findFirst().orElse("");

        AnnouncementResponse response = announcementService.getVisibleAnnouncementById(id, userRole);
        return ResponseEntity.ok(ApiResponse.success("Announcement retrieved", response));
    }

    @Operation(summary = "Get Latest Announcements", description = "Get latest N announcements for dashboard")
    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<List<AnnouncementResponse>>> getLatestAnnouncements(
            @RequestParam(defaultValue = "5") int limit,
            @CurrentUser UserPrincipal user) {

        String userRole = user.getRoles().stream().findFirst().orElse("");

        List<AnnouncementResponse> response = announcementService.getLatestAnnouncements(userRole, limit);
        return ResponseEntity.ok(ApiResponse.success("Latest announcements retrieved", response));
    }
}