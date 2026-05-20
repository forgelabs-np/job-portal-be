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
@RequestMapping("/api/candidate/interviews")
@PreAuthorize("hasRole('CANDIDATE')")
@Tag(name = "Candidate - Interviews", description = "Candidate Interview View APIs")
public class CandidateInterviewController {

    private final InterviewService interviewService;

    @Operation(summary = "Get My Interviews", description = "Get all interviews for the logged-in candidate")
    @GetMapping
    public ResponseEntity<ApiResponse<PageRes<InterviewResponse>>> getMyInterviews(
            @PageableDefault(size = 20) Pageable pageable,
            @CurrentUser UserPrincipal candidate) {

        Page<InterviewResponse> interviews = interviewService.getCandidateInterviews(candidate.getId(), pageable);
        PageRes<InterviewResponse> response = Pages.of(interviews);

        return ResponseEntity.ok(ApiResponse.success("Interviews retrieved", response));
    }

    @Operation(summary = "Get Interview by ID", description = "Get single interview details (read only)")
    @GetMapping("/{interviewId}")
    public ResponseEntity<ApiResponse<InterviewResponse>> getInterviewById(
            @PathVariable Long interviewId,
            @CurrentUser UserPrincipal candidate) {

        InterviewResponse response = interviewService.getCandidateInterviewById(interviewId, candidate.getId());
        return ResponseEntity.ok(ApiResponse.success("Interview retrieved", response));
    }
}