package com.jobportal.v1.controller;

import com.jobportal.v1.dto.ApiResponse;
import com.jobportal.v1.dto.jobDemand.response.JobDemandResponse;
import com.jobportal.v1.dto.PageRes;
import com.jobportal.v1.service.PublicJobService;
import com.jobportal.v1.util.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/jobs")
@Tag(name = "Public Jobs", description = "Public Job Listing APIs (No authentication required)")
public class PublicJobController {

    private final PublicJobService publicJobService;

    @Operation(summary = "Get All Public Jobs", description = "Get paginated list of all public jobs (isPublic=true and status=OPEN)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageRes<JobDemandResponse>>> getPublicJobs(
            @PageableDefault(size = 20) Pageable pageable) {

        Page<JobDemandResponse> response = publicJobService.getPublicJobs(pageable);
        return ResponseUtil.page("Public jobs retrieved", response);
    }

    @Operation(summary = "Get Public Job by ID", description = "Get a single public job by ID (only if isPublic=true)")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobDemandResponse>> getPublicJobById(@PathVariable Long id) {
        JobDemandResponse response = publicJobService.getPublicJobById(id);
        return ResponseUtil.ok("Job retrieved", response);
    }
}