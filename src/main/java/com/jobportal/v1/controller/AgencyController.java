package com.jobportal.v1.controller;

import com.jobportal.v1.dto.ApiRequest;
import com.jobportal.v1.dto.ApiResponse;
import com.jobportal.v1.dto.agency.request.AgencyProfileRequest;
import com.jobportal.v1.dto.agency.response.AgencyDashboardResponse;
import com.jobportal.v1.dto.agency.response.AgencyDocumentResponse;
import com.jobportal.v1.dto.agency.response.AgencyProfileResponse;
import com.jobportal.v1.security.CurrentUser;
import com.jobportal.v1.security.UserPrincipal;
import com.jobportal.v1.service.AgencyProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/agency")
@PreAuthorize("hasRole('AGENCY')")
@Tag(name = "Agency", description = "Agency Management APIs")
public class AgencyController {

    private final AgencyProfileService agencyProfileService;

    @Operation(summary = "Agency Dashboard", description = "Get dashboard statistics and activities for agency")
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<AgencyDashboardResponse>> getDashboard(
            @CurrentUser UserPrincipal agency) {

        AgencyDashboardResponse response = agencyProfileService.getAgencyDashboard(agency.getId());
        return ResponseEntity.ok(ApiResponse.success("Dashboard data retrieved", response));
    }

    @Operation(summary = "Create or Update Agency Profile", description = "Agency creates or updates their profile")
    @PostMapping("/profile")
    public ResponseEntity<ApiResponse<AgencyProfileResponse>> createOrUpdateProfile(
            @Valid @RequestBody ApiRequest<AgencyProfileRequest> request,
            @CurrentUser UserPrincipal agency) {

        AgencyProfileResponse response = agencyProfileService.createOrUpdateProfile(agency.getId(), request.getData());
        return ResponseEntity.ok(ApiResponse.success("Profile saved successfully", response));
    }

    @Operation(summary = "Get My Profile", description = "Agency gets their own profile")
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<AgencyProfileResponse>> getMyProfile(
            @CurrentUser UserPrincipal agency) {

        AgencyProfileResponse response = agencyProfileService.getMyProfile(agency.getId());
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved", response));
    }

    @Operation(summary = "Upload Document", description = "Upload agency document (Trade Licence, Company Registration, MOU, Owner Citizenship)")
    @PostMapping(value = "/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AgencyDocumentResponse>> uploadDocument(
            @RequestParam("documentType") String documentType,
            @RequestParam("file") MultipartFile file,
            @CurrentUser UserPrincipal agency) {

        AgencyDocumentResponse response = agencyProfileService.uploadDocument(agency.getId(), documentType, file);
        return ResponseEntity.ok(ApiResponse.success("Document uploaded successfully", response));
    }

    @Operation(summary = "Get My Documents", description = "Get all documents uploaded by agency")
    @GetMapping("/documents")
    public ResponseEntity<ApiResponse<List<AgencyDocumentResponse>>> getMyDocuments(
            @CurrentUser UserPrincipal agency) {

        List<AgencyDocumentResponse> response = agencyProfileService.getMyDocuments(agency.getId());
        return ResponseEntity.ok(ApiResponse.success("Documents retrieved", response));
    }

    @Operation(summary = "Delete Document", description = "Delete an uploaded document")
    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @PathVariable Long documentId,
            @CurrentUser UserPrincipal agency) {

        agencyProfileService.deleteDocument(agency.getId(), documentId);
        return ResponseEntity.ok(ApiResponse.success("Document deleted successfully", null));
    }
}