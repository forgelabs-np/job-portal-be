package com.jobportal.v1.controller;

import com.jobportal.v1.dto.ApiRequest;
import com.jobportal.v1.dto.ApiResponse;
import com.jobportal.v1.dto.PageRes;
import com.jobportal.v1.dto.agency.response.AgencyJobApplicationResponse;
import com.jobportal.v1.dto.candidate.response.ShortlistedCandidateMinimalResponse;
import com.jobportal.v1.dto.jobApplicationReport.request.JobApplicationRequest;
import com.jobportal.v1.dto.jobApplicationReport.response.JobApplicationResponse;
import com.jobportal.v1.security.CurrentUser;
import com.jobportal.v1.security.UserPrincipal;
import com.jobportal.v1.service.JobApplicationService;
import com.jobportal.v1.service.ShortlistedCandidateService;
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
@RequestMapping("/api/agency/applications")
@PreAuthorize("hasRole('AGENCY')")
@Tag(name = "Agency - Applications", description = "Agency Job Application APIs")
public class AgencyApplicationController {

    private final JobApplicationService jobApplicationService;
    private final ShortlistedCandidateService shortlistedCandidateService;  // Add MyBatis service

    @Operation(summary = "Apply for Job", description = "Agency applies for a job using a candidate")
    @PostMapping
    public ResponseEntity<ApiResponse<JobApplicationResponse>> applyForJob(
            @Valid @RequestBody ApiRequest<JobApplicationRequest> request,
            @CurrentUser UserPrincipal agency) {

        JobApplicationResponse response = jobApplicationService.applyForJob(request.getData(), agency.getId());
        return ResponseEntity.ok(ApiResponse.success("Application submitted successfully", response));
    }

    @Operation(summary = "Get My Applications", description = "Get all applications submitted by this agency (filter by status)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageRes<JobApplicationResponse>>> getMyApplications(
            @RequestParam(required = false) Long jobDemandId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable,
            @CurrentUser UserPrincipal agency) {

        Page<JobApplicationResponse> applications = jobApplicationService.getMyApplications(
                agency.getId(), jobDemandId, status, pageable);
        PageRes<JobApplicationResponse> response = Pages.of(applications);

        return ResponseEntity.ok(ApiResponse.success("Applications retrieved", response));
    }

    @Operation(summary = "Get Agency's Shortlisted Candidates",
            description = "Get shortlisted candidates for this agency only")
    @GetMapping("/shortlisted")
    public ResponseEntity<ApiResponse<PageRes<ShortlistedCandidateMinimalResponse>>> getAgencyShortlistedCandidates(
            @PageableDefault(size = 20) Pageable pageable,
            @CurrentUser UserPrincipal agency) {

        Page<ShortlistedCandidateMinimalResponse> candidates = shortlistedCandidateService.getShortlistedCandidatesForAgency(
                agency.getId(), pageable);

        PageRes<ShortlistedCandidateMinimalResponse> response = Pages.of(candidates);
        return ResponseEntity.ok(ApiResponse.success("Agency shortlisted candidates retrieved", response));
    }

    @Operation(summary = "Get Application by ID", description = "Get specific application details")
    @GetMapping("/{applicationId}")
    public ResponseEntity<ApiResponse<AgencyJobApplicationResponse>> getApplicationById(
            @PathVariable Long applicationId,
            @CurrentUser UserPrincipal agency) {

        AgencyJobApplicationResponse response = jobApplicationService.getMyApplicationById(applicationId, agency.getId());
        return ResponseEntity.ok(ApiResponse.success("Application retrieved", response));
    }

    @Operation(summary = "Withdraw Application", description = "Withdraw a pending application")
    @PatchMapping("/{applicationId}/withdraw")
    public ResponseEntity<ApiResponse<JobApplicationResponse>> withdrawApplication(
            @PathVariable Long applicationId,
            @CurrentUser UserPrincipal agency) {

        JobApplicationResponse response = jobApplicationService.withdrawApplication(applicationId, agency.getId());
        return ResponseEntity.ok(ApiResponse.success("Application withdrawn successfully", response));
    }
}