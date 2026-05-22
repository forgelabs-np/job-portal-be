package com.jobportal.v1.controller.interview;

import com.jobportal.v1.dto.ApiResponse;
import com.jobportal.v1.dto.PageRes;
import com.jobportal.v1.dto.candidate.response.ShortlistedCandidateFullResponse;
import com.jobportal.v1.dto.candidate.response.ShortlistedCandidateMinimalResponse;
import com.jobportal.v1.security.CurrentUser;
import com.jobportal.v1.security.UserPrincipal;
import com.jobportal.v1.service.ShortlistedCandidateService;
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

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/shortlisted")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Shortlisted Candidates", description = "Admin Shortlisted Candidates Management APIs")
public class AdminShortlistedController {

    private final ShortlistedCandidateService shortlistedCandidateService;

    @Operation(summary = "Get Shortlisted Candidates (List View)",
            description = "Minimal info for list view. Use applicationType=SELF for self-candidates, AGENCY for agency-candidates, and jobDemandId to filter by job")
    @GetMapping
    public ResponseEntity<ApiResponse<PageRes<ShortlistedCandidateMinimalResponse>>> getShortlistedCandidates(
            @RequestParam(required = false) String applicationType,
            @RequestParam(required = false) Long jobDemandId,
            @PageableDefault(size = 20) Pageable pageable,
            @CurrentUser UserPrincipal admin) {

        Page<ShortlistedCandidateMinimalResponse> candidates = shortlistedCandidateService.getShortlistedCandidates(
                applicationType, jobDemandId, pageable);  // ✅ Pass jobDemandId
        PageRes<ShortlistedCandidateMinimalResponse> response = Pages.of(candidates);

        String message = applicationType == null ? "All shortlisted candidates retrieved" :
                applicationType.equalsIgnoreCase("SELF") ? "Self-candidate shortlisted candidates retrieved" :
                "Agency-candidate shortlisted candidates retrieved";

        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @Operation(summary = "Get Shortlisted Candidate Details (Detail View)",
            description = "Full details including documents and interview info")
    @GetMapping("/{applicationId}")
    public ResponseEntity<ApiResponse<ShortlistedCandidateFullResponse>> getShortlistedCandidateById(
            @PathVariable Long applicationId,
            @CurrentUser UserPrincipal admin) {

        ShortlistedCandidateFullResponse response = shortlistedCandidateService.getShortlistedCandidateById(applicationId);
        return ResponseEntity.ok(ApiResponse.success("Shortlisted candidate details retrieved", response));
    }
}