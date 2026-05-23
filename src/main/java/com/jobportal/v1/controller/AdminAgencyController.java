package com.jobportal.v1.controller;

import com.jobportal.v1.dto.ApiRequest;
import com.jobportal.v1.dto.ApiResponse;
import com.jobportal.v1.dto.PageRes;
import com.jobportal.v1.dto.admin.request.AgencyDocumentApprovalRequest;
import com.jobportal.v1.dto.agency.request.ProfileApprovalRequest;
import com.jobportal.v1.dto.agency.response.AgencyDocumentResponse;
import com.jobportal.v1.dto.agency.response.AgencyProfileResponse;
import com.jobportal.v1.enums.ApprovalStatus;
import com.jobportal.v1.security.CurrentUser;
import com.jobportal.v1.security.UserPrincipal;
import com.jobportal.v1.service.AdminAgencyService;
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
@RequestMapping("/api/admin/agency")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Agency Management", description = "Admin approving / rejecting documents and profile APIs")
public class AdminAgencyController {

    private final AdminAgencyService adminAgencyService;

    // Document Management
    @Operation(summary = "Get Pending Documents", description = "Get all documents pending approval")
    @GetMapping("/documents/pending")
    public ResponseEntity<ApiResponse<List<AgencyDocumentResponse>>> getPendingDocuments() {
        List<AgencyDocumentResponse> response = adminAgencyService.getPendingDocuments();
        return ResponseUtil.ok("Pending documents retrieved", response);
    }

    @Operation(summary = "Get Agency Documents", description = "Get all documents for a specific agency")
    @GetMapping("/agencies/{agencyId}/documents")
    public ResponseEntity<ApiResponse<List<AgencyDocumentResponse>>> getAgencyDocuments(
            @PathVariable Long agencyId) {
        List<AgencyDocumentResponse> response = adminAgencyService.getDocumentsByAgency(agencyId);
        return ResponseUtil.ok("Agency documents retrieved", response);
    }

    @Operation(summary = "Process Document Approval", description = "Approve or reject an agency document")
    @PostMapping("/documents/process")
    public ResponseEntity<ApiResponse<AgencyDocumentResponse>> processDocumentApproval(
            @Valid @RequestBody ApiRequest<AgencyDocumentApprovalRequest> request,
            @CurrentUser UserPrincipal admin) {

        AgencyDocumentResponse response = adminAgencyService.processDocumentApproval(request.getData(), admin.getId());
        String message = request.getData().getStatus().name().equals("APPROVED")
                ? "Document approved successfully"
                : "Document rejected successfully";
        return ResponseUtil.ok(message, response);
    }

    // Profile Management
    @Operation(summary = "Get Profiles by Status", description = "Get agency profiles filtered by approval status with pagination")
    @GetMapping("/profiles")
    public ResponseEntity<ApiResponse<PageRes<AgencyProfileResponse>>> getProfilesByStatus(
            @RequestParam(required = false) ApprovalStatus status,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<AgencyProfileResponse> response = adminAgencyService.getProfilesByStatus(status, pageable);
        String message = status == null ? "All profiles retrieved" : status + " profiles retrieved";
        return ResponseUtil.page(message, response);
    }

    @Operation(summary = "Get Profile Details", description = "Get agency profile details by user ID")
    @GetMapping("/profiles/{userId}")
    public ResponseEntity<ApiResponse<AgencyProfileResponse>> getProfileDetails(
            @PathVariable Long userId) {
        AgencyProfileResponse response = adminAgencyService.getProfileDetails(userId);
        return ResponseUtil.ok("Profile details retrieved", response);
    }

    @Operation(summary = "Process Profile Approval", description = "Approve or reject an agency profile")
    @PostMapping("/profiles/process")
    public ResponseEntity<ApiResponse<AgencyProfileResponse>> processProfileApproval(
            @Valid @RequestBody ApiRequest<ProfileApprovalRequest> request,
            @CurrentUser UserPrincipal admin) {

        AgencyProfileResponse response = adminAgencyService.processProfileApproval(request.getData(), admin.getId());
        String message = request.getData().getStatus().name().equals("APPROVED")
                ? "Profile approved successfully"
                : "Profile rejected successfully";
        return ResponseUtil.ok(message, response);
    }
}