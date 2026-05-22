package com.jobportal.v1.controller;

import com.jobportal.v1.dto.ApiResponse;
import com.jobportal.v1.dto.PageRes;
import com.jobportal.v1.dto.agency.response.AgencyJobResponse;
import com.jobportal.v1.security.CurrentUser;
import com.jobportal.v1.security.UserPrincipal;
import com.jobportal.v1.service.JobAgencyAssignmentService;
import com.jobportal.v1.util.Pages;
import com.jobportal.v1.util.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/agency/jobs")
@PreAuthorize("hasRole('AGENCY')")
@Tag(name = "Agency - Jobs", description = "Agency Job APIs")
public class AgencyJobController {

    private final JobAgencyAssignmentService jobAgencyAssignmentService;

    @Operation(summary = "Get My Assigned Jobs", description = "Get all jobs assigned to this agency with full details")
    @GetMapping("/assigned")
    public ResponseEntity<ApiResponse<PageRes<AgencyJobResponse>>> getMyAssignedJobs(
            @PageableDefault(size = 20) Pageable pageable,
            @CurrentUser UserPrincipal agency) {

        Page<AgencyJobResponse> response = jobAgencyAssignmentService.getMyAssignedJobs(agency.getId(), pageable);
        PageRes<AgencyJobResponse> pageRes = Pages.of(response);

        return ResponseUtil.ok("Assigned jobs retrieved", pageRes);
    }
}