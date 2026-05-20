package com.jobportal.v1.controller.interview;

import com.jobportal.v1.dto.ApiResponse;
import com.jobportal.v1.dto.PageRes;
import com.jobportal.v1.dto.interview.response.InterviewResponse;
import com.jobportal.v1.security.CurrentUser;
import com.jobportal.v1.security.UserPrincipal;
import com.jobportal.v1.service.InterviewService;
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
@RequestMapping("/api/agency/interviews")
@PreAuthorize("hasRole('AGENCY')")
@Tag(name = "Agency - Interviews", description = "Agency Interview View APIs")
public class AgencyInterviewController {

    private final InterviewService interviewService;

    @Operation(summary = "Get Agency Interviews", description = "Get all interviews for agency's candidates")
    @GetMapping
    public ResponseEntity<ApiResponse<PageRes<InterviewResponse>>> getAgencyInterviews(
            @PageableDefault(size = 20) Pageable pageable,
            @CurrentUser UserPrincipal agency) {

        Page<InterviewResponse> interviews = interviewService.getAgencyInterviews(agency.getId(), pageable);
        PageRes<InterviewResponse> response = Pages.of(interviews);

        return ResponseEntity.ok(ApiResponse.success("Agency interviews retrieved", response));
    }

    @Operation(summary = "Get Agency Interview by ID", description = "Get single interview details (read only)")
    @GetMapping("/{interviewId}")
    public ResponseEntity<ApiResponse<InterviewResponse>> getAgencyInterviewById(
            @PathVariable Long interviewId,
            @CurrentUser UserPrincipal agency) {

        InterviewResponse response = interviewService.getAgencyInterviewById(interviewId, agency.getId());
        return ResponseEntity.ok(ApiResponse.success("Interview retrieved", response));
    }
}