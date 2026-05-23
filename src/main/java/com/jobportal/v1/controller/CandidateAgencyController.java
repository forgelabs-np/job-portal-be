package com.jobportal.v1.controller;

import com.jobportal.v1.dto.ApiRequest;
import com.jobportal.v1.dto.ApiResponse;
import com.jobportal.v1.dto.PageRes;
import com.jobportal.v1.dto.candidate.request.CandidateRequest;
import com.jobportal.v1.dto.candidate.response.CandidateDocumentResponse;
import com.jobportal.v1.dto.candidate.response.CandidateResponse;
import com.jobportal.v1.security.CurrentUser;
import com.jobportal.v1.security.UserPrincipal;
import com.jobportal.v1.service.CandidateService;
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
@RequestMapping("/api/agency/candidates")
@PreAuthorize("hasRole('AGENCY')")
@Tag(name = "Agency - Candidates", description = "Agency Candidate Management APIs")
public class CandidateAgencyController {

    private final CandidateService candidateService;

    @Operation(summary = "Create or Update Candidate", description = "Create new candidate (without id) or update existing (with id)")
    @PostMapping
    public ResponseEntity<ApiResponse<CandidateResponse>> createOrUpdateCandidate(
            @Valid @RequestBody ApiRequest<CandidateRequest> request,
            @CurrentUser UserPrincipal agency) {

        CandidateResponse response = candidateService.createOrUpdateCandidate(request.getData(), agency.getId());
        String message = request.getData().getId() == null ? "Candidate created successfully" : "Candidate updated successfully";
        return ResponseUtil.ok(message, response);
    }

    @Operation(summary = "Upload Candidate Document", description = "Upload a document for a specific candidate (only PDF, DOC, DOCX, JPG, PNG)")
    @PostMapping(value = "/{candidateId}/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CandidateDocumentResponse>> uploadCandidateDocument(
            @PathVariable Long candidateId,
            @RequestParam("documentType") String documentType,
            @RequestParam("file") MultipartFile file,
            @CurrentUser UserPrincipal agency) {

        CandidateDocumentResponse response = candidateService.uploadCandidateDocument(candidateId, agency.getId(), documentType, file);
        return ResponseUtil.ok("Document uploaded successfully", response);
    }

    @Operation(summary = "Get Candidate Documents", description = "Get all documents for a specific candidate")
    @GetMapping("/{candidateId}/documents")
    public ResponseEntity<ApiResponse<List<CandidateDocumentResponse>>> getCandidateDocuments(
            @PathVariable Long candidateId,
            @CurrentUser UserPrincipal agency) {

        List<CandidateDocumentResponse> response = candidateService.getCandidateDocuments(candidateId, agency.getId());
        return ResponseUtil.ok("Documents retrieved successfully", response);
    }

    @Operation(summary = "Delete Candidate Document", description = "Delete a specific document from a candidate")
    @DeleteMapping("/{candidateId}/documents/{documentId}")
    public ResponseEntity<ApiResponse<Void>> deleteCandidateDocument(
            @PathVariable Long candidateId,
            @PathVariable Long documentId,
            @CurrentUser UserPrincipal agency) {

        candidateService.deleteCandidateDocument(candidateId, agency.getId(), documentId);
        return ResponseUtil.ok("Document deleted successfully");
    }

    @Operation(summary = "Get Candidate by ID", description = "Get candidate details by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CandidateResponse>> getCandidateById(
            @PathVariable Long id,
            @CurrentUser UserPrincipal agency) {

        CandidateResponse response = candidateService.getCandidateById(id, agency.getId());
        return ResponseUtil.ok("Candidate retrieved", response);
    }

    @Operation(summary = "Get All Candidates", description = "Get paginated list of all candidates (filter by status)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageRes<CandidateResponse>>> getAllCandidates(
            @RequestParam(required = false) Boolean status,
            @PageableDefault(size = 20) Pageable pageable,
            @CurrentUser UserPrincipal agency) {

        Page<CandidateResponse> candidatePage = candidateService.getAllCandidates(agency.getId(), status, pageable);

        String message = status == null ? "All candidates retrieved" :
                (status ? "Enabled candidates retrieved" : "Disabled candidates retrieved");
        return ResponseUtil.page(message, candidatePage);
    }

    @Operation(summary = "Toggle Candidate Status", description = "Enable or disable a candidate (toggles current status)")
    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<ApiResponse<CandidateResponse>> toggleCandidateStatus(
            @PathVariable Long id,
            @CurrentUser UserPrincipal agency) {

        CandidateResponse response = candidateService.toggleCandidateStatus(id, agency.getId());
        String message = response.getIsEnabled() ? "Candidate enabled successfully" : "Candidate disabled successfully";
        return ResponseUtil.ok(message, response);
    }

    @Operation(summary = "Delete Candidate", description = "Delete a candidate profile")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCandidate(
            @PathVariable Long id,
            @CurrentUser UserPrincipal agency) {

        candidateService.deleteCandidate(id, agency.getId());
        return ResponseUtil.ok("Candidate deleted successfully");
    }
}