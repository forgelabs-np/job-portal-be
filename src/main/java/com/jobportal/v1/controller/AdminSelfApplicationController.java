package com.jobportal.v1.controller;

import com.jobportal.v1.dto.ApiRequest;
import com.jobportal.v1.dto.ApiResponse;
import com.jobportal.v1.dto.PageRes;
import com.jobportal.v1.dto.admin.request.DocumentApprovalRequest;
import com.jobportal.v1.dto.admin.response.AdminSelfApplicationResponse;
import com.jobportal.v1.dto.candidate.response.CandidateDocumentResponse;
import com.jobportal.v1.dto.candidate.response.DocumentVerificationStats;
import com.jobportal.v1.dto.jobApplicationReport.request.ApplicationStatusUpdateRequest;
import com.jobportal.v1.dto.jobApplicationReport.response.JobApplicationResponse;
import com.jobportal.v1.security.CurrentUser;
import com.jobportal.v1.security.UserPrincipal;
import com.jobportal.v1.service.AdminCandidateDocumentService;
import com.jobportal.v1.service.JobApplicationService;
import com.jobportal.v1.util.Pages;
import com.jobportal.v1.util.ResponseUtil;
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

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/self-applications")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Self Applications", description = "Admin Self-Candidate Application Management APIs")
public class AdminSelfApplicationController {

    private final JobApplicationService jobApplicationService;
    private final AdminCandidateDocumentService adminCandidateDocumentService;

    @Operation(summary = "Get All Self-Candidate Applications", description = "Get all applications from self-registered candidates (lightweight list)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageRes<JobApplicationResponse>>> getAllSelfApplications(
            @RequestParam(required = false) Long jobDemandId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<JobApplicationResponse> applications = jobApplicationService.getAllSelfApplications(
                jobDemandId, status, pageable);
        PageRes<JobApplicationResponse> response = Pages.of(applications);

        return ResponseUtil.ok("Self-candidate applications retrieved", response);
    }

    @Operation(summary = "Get Self-Application Details", description = "Get detailed application information for self-candidate application")
    @GetMapping("/{applicationId}")
    public ResponseEntity<ApiResponse<AdminSelfApplicationResponse>> getSelfApplicationDetails(
            @PathVariable Long applicationId) {

        AdminSelfApplicationResponse response = jobApplicationService.getSelfApplicationDetails(applicationId);
        return ResponseUtil.ok("Application details retrieved", response);
    }

    @Operation(summary = "Update Self-Application Status", description = "Approve, shortlist, or reject a self-candidate application")
    @PatchMapping("/{applicationId}/status")
    public ResponseEntity<ApiResponse<AdminSelfApplicationResponse>> updateSelfApplicationStatus(
            @PathVariable Long applicationId,
            @Valid @RequestBody ApiRequest<ApplicationStatusUpdateRequest> request,
            @CurrentUser UserPrincipal admin) {

        AdminSelfApplicationResponse response = jobApplicationService.updateSelfApplicationStatus(
                applicationId, request.getData(), admin.getId());

        String message = "Application status updated to " + request.getData().getStatus();
        return ResponseUtil.ok(message, response);
    }

    // ============ Document Verification (Similar to Agency Profile) ============

    @Operation(summary = "Get Pending Candidate Documents", description = "Get all candidate documents pending approval")
    @GetMapping("/documents/pending")
    public ResponseEntity<ApiResponse<PageRes<CandidateDocumentResponse>>> getPendingDocuments(
            @PageableDefault(size = 20) Pageable pageable) {

        Page<CandidateDocumentResponse> response = adminCandidateDocumentService.getPendingDocuments(pageable);
        PageRes<CandidateDocumentResponse> pageRes = Pages.of(response);

        return ResponseUtil.ok("Pending documents retrieved", pageRes);
    }

    @Operation(summary = "Get Documents by Candidate", description = "Get all documents for a specific candidate")
    @GetMapping("/candidates/{candidateId}/documents")
    public ResponseEntity<ApiResponse<List<CandidateDocumentResponse>>> getDocumentsByCandidate(
            @PathVariable Long candidateId) {

        List<CandidateDocumentResponse> response = adminCandidateDocumentService.getDocumentsByCandidate(candidateId);
        return ResponseUtil.ok("Candidate documents retrieved", response);
    }

    @Operation(summary = "Process Document Approval", description = "Approve or reject a candidate document")
    @PostMapping("/documents/process")
    public ResponseEntity<ApiResponse<CandidateDocumentResponse>> processDocumentApproval(
            @Valid @RequestBody ApiRequest<DocumentApprovalRequest> request,
            @CurrentUser UserPrincipal admin) {

        CandidateDocumentResponse response = adminCandidateDocumentService.processDocumentApproval(
                request.getData(), admin.getId());

        String message = request.getData().getStatus().name().equals("APPROVED")
                ? "Document approved successfully"
                : "Document rejected successfully";
        return ResponseUtil.ok(message, response);
    }

    @Operation(summary = "Get Document Statistics", description = "Get document verification statistics")
    @GetMapping("/documents/statistics")
    public ResponseEntity<ApiResponse<DocumentVerificationStats>> getDocumentStatistics() {
        DocumentVerificationStats response = adminCandidateDocumentService.getDocumentStatistics();
        return ResponseUtil.ok("Statistics retrieved", response);
    }
}