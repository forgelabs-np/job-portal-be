package com.jobportal.v1.controller;

import com.jobportal.v1.dto.ApiRequest;
import com.jobportal.v1.dto.ApiResponse;
import com.jobportal.v1.dto.jobDemand.request.JobAgencyAssignmentRequest;
import com.jobportal.v1.dto.jobDemand.response.JobAgencyAssignmentResponse;
import com.jobportal.v1.dto.admin.response.AdminDashboardResponse;
import com.jobportal.v1.dto.agency.request.AgencyActionRequest;
import com.jobportal.v1.dto.agency.response.AgencyApprovalResponse;
import com.jobportal.v1.dto.agency.response.AgencyJobDetailResponse;
import com.jobportal.v1.security.CurrentUser;
import com.jobportal.v1.security.UserPrincipal;
import com.jobportal.v1.service.AdminService;
import com.jobportal.v1.service.DashboardService;
import com.jobportal.v1.service.JobAgencyAssignmentService;
import com.jobportal.v1.util.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin Management APIs")
public class AdminController {

    private final AdminService adminService;
    private final DashboardService dashboardService;
    private final JobAgencyAssignmentService jobAgencyAssignmentService;

    @Operation(summary = "Admin Dashboard", description = "Get dashboard statistics and recent activities")
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getDashboard() {
        AdminDashboardResponse response = dashboardService.getAdminDashboard();
        return ResponseUtil.ok("Dashboard data retrieved", response);
    }

    @Operation(summary = "Process Agency Action", description = "Approve or reject a pending agency")
    @PostMapping("/agencies/action")
    public ResponseEntity<ApiResponse<AgencyApprovalResponse>> processAgencyAction(
            @Valid @RequestBody ApiRequest<AgencyActionRequest> request,
            @CurrentUser UserPrincipal admin) {

        AgencyApprovalResponse response = adminService.processAgencyAction(request.getData(), admin.getId());
        return ResponseUtil.ok("Agency " + request.getData().getAction().toLowerCase() + "d successfully", response);
    }

    @Operation(summary = "Assign Agencies to Job", description = "Assign multiple agencies to a job demand")
    @PostMapping("/jobs/assign")
    public ResponseEntity<ApiResponse<List<JobAgencyAssignmentResponse>>> assignAgenciesToJob(
            @Valid @RequestBody ApiRequest<JobAgencyAssignmentRequest> request,
            @CurrentUser UserPrincipal admin) {

        List<JobAgencyAssignmentResponse> response = jobAgencyAssignmentService.assignAgenciesToJob(request.getData(), admin.getId());
        return ResponseUtil.ok("Agencies assigned successfully", response);
    }

    @Operation(summary = "Remove Agency from Job", description = "Remove an agency from a job demand")
    @DeleteMapping("/jobs/{jobDemandId}/agencies/{agencyId}")
    public ResponseEntity<ApiResponse<Void>> removeAgencyFromJob(
            @PathVariable Long jobDemandId,
            @PathVariable Long agencyId) {

        jobAgencyAssignmentService.removeAgencyFromJob(jobDemandId, agencyId);
        return ResponseUtil.ok("Agency removed from job successfully");
    }

    @Operation(summary = "Toggle Agency Job Access", description = "Enable or disable agency access to a job")
    @PatchMapping("/jobs/{jobDemandId}/agencies/{agencyId}")
    public ResponseEntity<ApiResponse<JobAgencyAssignmentResponse>> toggleAgencyJobAccess(
            @PathVariable Long jobDemandId,
            @PathVariable Long agencyId,
            @RequestParam Boolean enabled) {

        JobAgencyAssignmentResponse response = jobAgencyAssignmentService.toggleAgencyJobAccess(jobDemandId, agencyId, enabled);
        return ResponseUtil.ok("Access toggled successfully", response);
    }

    @Operation(summary = "Get Agencies by Job", description = "Get all agencies assigned to a job")
    @GetMapping("/jobs/{jobDemandId}/agencies")
    public ResponseEntity<ApiResponse<List<JobAgencyAssignmentResponse>>> getAgenciesByJob(
            @PathVariable Long jobDemandId) {

        List<JobAgencyAssignmentResponse> response = jobAgencyAssignmentService.getAgenciesByJob(jobDemandId);
        return ResponseUtil.ok("Agencies retrieved", response);
    }

    @Operation(summary = "Get Jobs by Agency", description = "Get all jobs assigned to an agency with job details")
    @GetMapping("/agencies/{agencyId}/jobs")
    public ResponseEntity<ApiResponse<List<AgencyJobDetailResponse>>> getJobsByAgency(
            @PathVariable Long agencyId) {

        List<AgencyJobDetailResponse> response = jobAgencyAssignmentService.getJobsByAgency(agencyId);
        return ResponseUtil.ok("Jobs retrieved", response);
    }
}