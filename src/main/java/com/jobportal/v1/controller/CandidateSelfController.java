package com.jobportal.v1.controller;

import com.jobportal.v1.dto.ApiRequest;
import com.jobportal.v1.dto.ApiResponse;
import com.jobportal.v1.dto.PageRes;
import com.jobportal.v1.dto.candidate.request.CandidateProfileRequest;
import com.jobportal.v1.dto.candidate.response.CandidateDocumentResponse;
import com.jobportal.v1.dto.candidate.response.CandidateJobApplicationResponse;
import com.jobportal.v1.dto.candidate.response.CandidateResponse;
import com.jobportal.v1.dto.dashboard.response.candidate.CandidateDashboardResponse;
import com.jobportal.v1.dto.jobApplicationReport.response.JobApplicationResponse;
import com.jobportal.v1.security.CurrentUser;
import com.jobportal.v1.security.UserPrincipal;
import com.jobportal.v1.service.CandidateDashboardService;
import com.jobportal.v1.service.CandidateSelfService;
import com.jobportal.v1.util.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/candidate")
@PreAuthorize("hasRole('CANDIDATE')")
@Tag(name = "Candidate - Self Service", description = "Candidate Self-Management APIs")
public class CandidateSelfController {

    private final CandidateSelfService candidateSelfService;
    private final CandidateDashboardService candidateDashboardService;

    @Operation(summary = "Candidate Dashboard", description = "Get dashboard statistics and activities for self-candidate")
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<CandidateDashboardResponse>> getDashboard(
            @CurrentUser UserPrincipal candidate) {

        CandidateDashboardResponse response = candidateDashboardService.getCandidateDashboard(candidate.getId());
        return ResponseUtil.ok("Dashboard data retrieved", response);
    }

    @Operation(summary = "Create or Update My Profile", description = "Create new profile (without id) or update existing (with id)")
    @PostMapping("/profile")
    public ResponseEntity<ApiResponse<CandidateResponse>> createOrUpdateProfile(
            @Valid @RequestBody ApiRequest<CandidateProfileRequest> request,
            @CurrentUser UserPrincipal candidate) {

        CandidateResponse response = candidateSelfService.createOrUpdateProfile(candidate.getId(), request.getData());
        return ResponseUtil.ok("Profile saved successfully", response);
    }

    @Operation(summary = "Get My Profile", description = "Get own candidate profile")
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<CandidateResponse>> getMyProfile(
            @CurrentUser UserPrincipal candidate) {

        CandidateResponse response = candidateSelfService.getMyProfile(candidate.getId());
        return ResponseUtil.ok("Profile retrieved", response);
    }

    @Operation(summary = "Upload Document", description = "Upload candidate document (PASSPORT, CV, PCC, etc.)")
    @PostMapping(value = "/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CandidateDocumentResponse>> uploadDocument(
            @RequestParam("documentType") String documentType,
            @RequestParam("file") MultipartFile file,
            @CurrentUser UserPrincipal candidate) {

        CandidateDocumentResponse response = candidateSelfService.uploadDocument(candidate.getId(), documentType, file);
        return ResponseUtil.ok("Document uploaded successfully", response);
    }

    @Operation(summary = "Get My Documents", description = "Get all documents uploaded by candidate")
    @GetMapping("/documents")
    public ResponseEntity<ApiResponse<List<CandidateDocumentResponse>>> getMyDocuments(
            @CurrentUser UserPrincipal candidate) {

        List<CandidateDocumentResponse> response = candidateSelfService.getMyDocuments(candidate.getId());
        return ResponseUtil.ok("Documents retrieved", response);
    }

    @Operation(summary = "Delete Document", description = "Delete an uploaded document")
    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @PathVariable Long documentId,
            @CurrentUser UserPrincipal candidate) {

        candidateSelfService.deleteDocument(candidate.getId(), documentId);
        return ResponseUtil.ok("Document deleted successfully");
    }

    @Operation(summary = "Get My Applications", description = "Get all job applications submitted by candidate")
    @GetMapping("/applications")
    public ResponseEntity<ApiResponse<PageRes<JobApplicationResponse>>> getMyApplications(
            @PageableDefault(size = 20) Pageable pageable,
            @CurrentUser UserPrincipal candidate) {

        Page<JobApplicationResponse> applications = candidateSelfService.getMyApplications(candidate.getId(), pageable);
        return ResponseUtil.page("Applications retrieved", applications);
    }

    @Operation(summary = "Get Application by ID", description = "Get detailed job application by ID with document statuses")
    @GetMapping("/applications/{applicationId}")
    public ResponseEntity<ApiResponse<CandidateJobApplicationResponse>> getMyApplicationById(
            @PathVariable Long applicationId,
            @CurrentUser UserPrincipal candidate) {

        CandidateJobApplicationResponse response = candidateSelfService.getMyApplicationById(candidate.getId(), applicationId);
        return ResponseUtil.ok("Application retrieved", response);
    }

    @Operation(summary = "Apply for Job", description = "Submit application for a job")
    @PostMapping("/jobs/{jobDemandId}/apply")
    public ResponseEntity<ApiResponse<JobApplicationResponse>> applyForJob(
            @PathVariable Long jobDemandId,
            @RequestParam(required = false) String notes,
            @CurrentUser UserPrincipal candidate) {

        JobApplicationResponse response = candidateSelfService.applyForJob(candidate.getId(), jobDemandId, notes);
        return ResponseUtil.ok("Application submitted successfully", response);
    }

    @Operation(summary = "Withdraw Application", description = "Withdraw a submitted job application")
    @PatchMapping("/applications/{applicationId}/withdraw")
    public ResponseEntity<ApiResponse<JobApplicationResponse>> withdrawApplication(
            @PathVariable Long applicationId,
            @CurrentUser UserPrincipal candidate) {

        JobApplicationResponse response = candidateSelfService.withdrawApplication(candidate.getId(), applicationId);
        return ResponseUtil.ok("Application withdrawn successfully", response);
    }

    @Operation(summary = "Get My Shortlisted Applications",
            description = "Get all SHORTLISTED applications for the logged-in candidate")
    @GetMapping("/applications/shortlisted")
    public ResponseEntity<ApiResponse<PageRes<JobApplicationResponse>>> getMyShortlistedApplications(
            @PageableDefault(size = 20) Pageable pageable,
            @CurrentUser UserPrincipal candidate) {

        Page<JobApplicationResponse> applications = candidateSelfService.getMyApplications(
                candidate.getId(), "SHORTLISTED", pageable);
        return ResponseUtil.page("Shortlisted applications retrieved", applications);
    }
}