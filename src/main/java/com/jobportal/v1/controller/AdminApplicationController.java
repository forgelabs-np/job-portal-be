package com.jobportal.v1.controller;

import com.jobportal.v1.dto.ApiRequest;
import com.jobportal.v1.dto.ApiResponse;
import com.jobportal.v1.dto.PageRes;
import com.jobportal.v1.dto.admin.response.AdminApplicationResponse;
import com.jobportal.v1.dto.jobApplicationReport.request.ApplicationStatusUpdateRequest;
import com.jobportal.v1.security.CurrentUser;
import com.jobportal.v1.security.UserPrincipal;
import com.jobportal.v1.service.JobApplicationService;
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
@RequestMapping("/api/admin/applications")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Applications", description = "Admin Job Application Management APIs")
public class AdminApplicationController {

    private final JobApplicationService jobApplicationService;

    @Operation(summary = "Get All Applications", description = "Get all applications with filters")
    @GetMapping
    public ResponseEntity<ApiResponse<PageRes<AdminApplicationResponse>>> getAllApplications(
            @RequestParam(required = false) Long jobDemandId,
            @RequestParam(required = false) Long agencyId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<AdminApplicationResponse> applications = jobApplicationService.getAllApplications(
                jobDemandId, agencyId, status, pageable);
        PageRes<AdminApplicationResponse> response = Pages.of(applications);

        return ResponseEntity.ok(ApiResponse.success("Applications retrieved", response));
    }

    @Operation(summary = "Get Application Details", description = "Get detailed application information")
    @GetMapping("/{applicationId}")
    public ResponseEntity<ApiResponse<AdminApplicationResponse>> getApplicationDetails(
            @PathVariable Long applicationId) {

        AdminApplicationResponse response = jobApplicationService.getApplicationDetails(applicationId);
        return ResponseEntity.ok(ApiResponse.success("Application details retrieved", response));
    }

    @Operation(summary = "Update Application Status", description = "Approve, shortlist, or reject an application")
    @PatchMapping("/{applicationId}/status")
    public ResponseEntity<ApiResponse<AdminApplicationResponse>> updateApplicationStatus(
            @PathVariable Long applicationId,
            @Valid @RequestBody ApiRequest<ApplicationStatusUpdateRequest> request,
            @CurrentUser UserPrincipal admin) {

        AdminApplicationResponse response = jobApplicationService.updateApplicationStatus(
                applicationId, request.getData(), admin.getId());

        String message = "Application status updated to " + request.getData().getStatus();
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }
}