package com.jobportal.v1.controller;

import com.jobportal.v1.dto.ApiRequest;
import com.jobportal.v1.dto.ApiResponse;
import com.jobportal.v1.dto.JobDemand.request.JobDemandRequest;
import com.jobportal.v1.dto.JobDemand.response.JobDemandResponse;
import com.jobportal.v1.dto.PageRes;
import com.jobportal.v1.security.CurrentUser;
import com.jobportal.v1.security.UserPrincipal;
import com.jobportal.v1.service.JobDemandService;
import com.jobportal.v1.util.Pages;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/jobs")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Job Demand", description = "Admin Job Demand Management APIs")
public class JobDemandController {

    private final JobDemandService jobDemandService;

    @Operation(summary = "Create or Update Job Demand", description = "Create new job demand (without id) or update existing (with id)")
    @PostMapping
    public ResponseEntity<ApiResponse<JobDemandResponse>> createOrUpdateJobDemand(
            @Valid @RequestBody ApiRequest<JobDemandRequest> request,
            @CurrentUser UserPrincipal admin) {

        JobDemandResponse response = jobDemandService.createOrUpdateJobDemand(request.getData(), admin.getId());
        String message = request.getData().getId() == null ? "Job demand created successfully" : "Job demand updated successfully";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @Operation(summary = "Get Job Demand by ID", description = "Get detailed job demand information")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobDemandResponse>> getJobDemand(@PathVariable Long id) {
        JobDemandResponse response = jobDemandService.getJobDemandById(id);
        return ResponseEntity.ok(ApiResponse.success("Job demand retrieved", response));
    }

    @Operation(summary = "Get All Job Demands", description = "Get paginated list of all job demands")
    @GetMapping
    public ResponseEntity<ApiResponse<PageRes<JobDemandResponse>>> getAllJobDemands(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = Pages.toPageable(page, size);
        Page<JobDemandResponse> jobPage = jobDemandService.getAllJobDemands(pageable);
        PageRes<JobDemandResponse> response = Pages.of(jobPage);

        return ResponseEntity.ok(ApiResponse.success("Job demands retrieved", response));
    }

    @Operation(summary = "Get Job Demands by Status", description = "Get job demands filtered by status (OPEN, CLOSED, COMPLETED, CANCELLED)")
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<PageRes<JobDemandResponse>>> getJobDemandsByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = Pages.toPageable(page, size);
        Page<JobDemandResponse> jobPage = jobDemandService.getJobDemandsByStatus(status.toUpperCase(), pageable);
        PageRes<JobDemandResponse> response = Pages.of(jobPage);

        return ResponseEntity.ok(ApiResponse.success("Job demands retrieved", response));
    }

    @Operation(summary = "Get Open Job Demands", description = "Get all open job demands (accepting candidates)")
    @GetMapping("/open")
    public ResponseEntity<ApiResponse<List<JobDemandResponse>>> getOpenJobDemands() {
        List<JobDemandResponse> response = jobDemandService.getOpenJobDemands();
        return ResponseEntity.ok(ApiResponse.success("Open job demands retrieved", response));
    }

    @Operation(summary = "Close Job Demand", description = "Close a job demand (stop accepting candidates)")
    @PatchMapping("/{id}/close")
    public ResponseEntity<ApiResponse<JobDemandResponse>> closeJobDemand(
            @PathVariable Long id,
            @CurrentUser UserPrincipal admin) {

        JobDemandResponse response = jobDemandService.closeJobDemand(id, admin.getId());
        return ResponseEntity.ok(ApiResponse.success("Job demand closed successfully", response));
    }

    @Operation(summary = "Delete Job Demand", description = "Soft delete a job demand")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteJobDemand(
            @PathVariable Long id,
            @CurrentUser UserPrincipal admin) {

        jobDemandService.deleteJobDemand(id, admin.getId());
        return ResponseEntity.ok(ApiResponse.success("Job demand deleted successfully", null));
    }
}