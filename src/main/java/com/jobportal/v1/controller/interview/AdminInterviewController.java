package com.jobportal.v1.controller.interview;

import com.jobportal.v1.dto.ApiRequest;
import com.jobportal.v1.dto.ApiResponse;
import com.jobportal.v1.dto.PageRes;
import com.jobportal.v1.dto.interview.request.InterviewRequest;
import com.jobportal.v1.dto.interview.request.InterviewResultRequest;
import com.jobportal.v1.dto.interview.response.InterviewResponse;
import com.jobportal.v1.enums.InterviewStatus;
import com.jobportal.v1.security.CurrentUser;
import com.jobportal.v1.security.UserPrincipal;
import com.jobportal.v1.service.InterviewService;
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
@RequestMapping("/api/admin/interviews")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Interviews", description = "Admin Interview Management APIs")
public class AdminInterviewController {

    private final InterviewService interviewService;

    @Operation(summary = "Create or Update Interview", description = "Create new interview or update existing")
    @PostMapping
    public ResponseEntity<ApiResponse<InterviewResponse>> createOrUpdateInterview(
            @Valid @RequestBody ApiRequest<InterviewRequest> request,
            @CurrentUser UserPrincipal admin) {

        InterviewResponse response = interviewService.createOrUpdateInterview(request.getData(), admin.getId());
        String message = request.getData().getId() == null ? "Interview scheduled successfully" : "Interview updated successfully";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @Operation(summary = "Get All Interviews", description = "Get all interviews with filters")
    @GetMapping
    public ResponseEntity<ApiResponse<PageRes<InterviewResponse>>> getAllInterviews(
            @RequestParam(required = false) Long jobDemandId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) Long agencyId,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<InterviewResponse> interviews = interviewService.getAllInterviews(jobDemandId, status, result, agencyId, pageable);
        PageRes<InterviewResponse> response = Pages.of(interviews);

        return ResponseEntity.ok(ApiResponse.success("Interviews retrieved", response));
    }

    @Operation(summary = "Get Interview by ID", description = "Get single interview details")
    @GetMapping("/{interviewId}")
    public ResponseEntity<ApiResponse<InterviewResponse>> getInterviewById(@PathVariable Long interviewId) {
        InterviewResponse response = interviewService.getInterviewById(interviewId);
        return ResponseEntity.ok(ApiResponse.success("Interview retrieved", response));
    }

    @Operation(summary = "Get Interview by Application", description = "Get interview for a specific application")
    @GetMapping("/application/{jobApplicationId}")
    public ResponseEntity<ApiResponse<InterviewResponse>> getInterviewByApplicationId(@PathVariable Long jobApplicationId) {
        InterviewResponse response = interviewService.getInterviewByApplicationId(jobApplicationId);
        return ResponseEntity.ok(ApiResponse.success("Interview retrieved", response));
    }

    @Operation(summary = "Set Interview Result", description = "Set result for completed interview (PASS/FAIL/RE_INTERVIEW)")
    @PatchMapping("/{interviewId}/result")
    public ResponseEntity<ApiResponse<InterviewResponse>> setInterviewResult(
            @PathVariable Long interviewId,
            @Valid @RequestBody ApiRequest<InterviewResultRequest> request,
            @CurrentUser UserPrincipal admin) {

        InterviewResponse response = interviewService.setInterviewResult(interviewId, request.getData(), admin.getId());
        String message = "Interview result set to " + request.getData().getResult();
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @Operation(summary = "Update Interview Status",
            description = "Update interview status (SCHEDULED, RESCHEDULED, COMPLETED, CANCELLED, NO_SHOW)")
    @PatchMapping("/{interviewId}/status")
    public ResponseEntity<ApiResponse<InterviewResponse>> updateInterviewStatus(
            @PathVariable Long interviewId,
            @RequestParam InterviewStatus status,
            @CurrentUser UserPrincipal admin) {

        InterviewResponse response = interviewService.updateInterviewStatus(interviewId, status, admin.getId());
        String message = "Interview status updated to " + status;
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @Operation(summary = "Cancel Interview", description = "Cancel a scheduled interview")
    @PatchMapping("/{interviewId}/cancel")
    public ResponseEntity<ApiResponse<InterviewResponse>> cancelInterview(
            @PathVariable Long interviewId,
            @CurrentUser UserPrincipal admin) {

        InterviewResponse response = interviewService.cancelInterview(interviewId, admin.getId());
        return ResponseEntity.ok(ApiResponse.success("Interview cancelled successfully", response));
    }

    @Operation(summary = "Delete Interview", description = "Permanently delete an interview")
    @DeleteMapping("/{interviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteInterview(
            @PathVariable Long interviewId,
            @CurrentUser UserPrincipal admin) {

        interviewService.deleteInterview(interviewId, admin.getId());
        return ResponseEntity.ok(ApiResponse.success("Interview deleted successfully", null));
    }
}