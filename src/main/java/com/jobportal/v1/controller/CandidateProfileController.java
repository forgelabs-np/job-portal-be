package com.jobportal.v1.controller;

import com.jobportal.v1.dto.ApiRequest;
import com.jobportal.v1.dto.ApiResponse;
import com.jobportal.v1.dto.PageRes;
import com.jobportal.v1.dto.candidate.request.CandidateProfileUpdateRequest;
import com.jobportal.v1.dto.candidate.response.CandidateResponse;
import com.jobportal.v1.dto.jobApplicationReport.response.JobApplicationResponse;
import com.jobportal.v1.security.CurrentUser;
import com.jobportal.v1.security.UserPrincipal;
import com.jobportal.v1.service.CandidateSelfService;
import com.jobportal.v1.util.Pages;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@RequestMapping("/api/candidate")
@PreAuthorize("hasRole('CANDIDATE')")
@Tag(name = "Candidate - Self Service", description = "Candidate Self-Management APIs")
public class CandidateProfileController {

    private final CandidateSelfService candidateSelfService;

    @Operation(summary = "Get My Profile", description = "Get own candidate profile")
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<CandidateResponse>> getMyProfile(
            @CurrentUser UserPrincipal candidate) {

        CandidateResponse response = candidateSelfService.getMyProfile(candidate.getId());
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved", response));
    }

    @Operation(summary = "Update My Profile", description = "Update own candidate profile")
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<CandidateResponse>> updateMyProfile(
            @Valid @RequestBody ApiRequest<CandidateProfileUpdateRequest> request,
            @CurrentUser UserPrincipal candidate) {

        CandidateResponse response = candidateSelfService.updateMyProfile(candidate.getId(), request.getData());
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", response));
    }

    @Operation(summary = "Get My Applications", description = "Get all job applications submitted by candidate")
    @GetMapping("/applications")
    public ResponseEntity<ApiResponse<PageRes<JobApplicationResponse>>> getMyApplications(
            @PageableDefault(size = 20) Pageable pageable,
            @CurrentUser UserPrincipal candidate) {

        Page<JobApplicationResponse> applications = candidateSelfService.getMyApplications(candidate.getId(), pageable);
        PageRes<JobApplicationResponse> response = Pages.of(applications);

        return ResponseEntity.ok(ApiResponse.success("Applications retrieved", response));
    }

    @Operation(summary = "Apply for Job", description = "Submit application for a job")
    @PostMapping("/jobs/{jobDemandId}/apply")
    public ResponseEntity<ApiResponse<JobApplicationResponse>> applyForJob(
            @PathVariable Long jobDemandId,
            @RequestParam(required = false) String notes,
            @CurrentUser UserPrincipal candidate) {

        JobApplicationResponse response = candidateSelfService.applyForJob(candidate.getId(), jobDemandId, notes);
        return ResponseEntity.ok(ApiResponse.success("Application submitted successfully", response));
    }
}