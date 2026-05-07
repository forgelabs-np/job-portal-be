package com.jobportal.v1.controller;

import com.jobportal.v1.dto.ApiRequest;
import com.jobportal.v1.dto.ApiResponse;
import com.jobportal.v1.dto.PageRes;
import com.jobportal.v1.dto.candidate.request.CandidateRequest;
import com.jobportal.v1.dto.candidate.response.CandidateResponse;
import com.jobportal.v1.security.CurrentUser;
import com.jobportal.v1.security.UserPrincipal;
import com.jobportal.v1.service.CandidateService;
import com.jobportal.v1.util.Pages;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/agency/candidates")
@PreAuthorize("hasRole('AGENCY')")
@Tag(name = "Agency - Candidates", description = "Agency Candidate Management APIs")
public class CandidateController {

    private final CandidateService candidateService;

    @Operation(summary = "Create or Update Candidate", description = "Create new candidate (without id) or update existing (with id)")
    @PostMapping
    public ResponseEntity<ApiResponse<CandidateResponse>> createOrUpdateCandidate(
            @Valid @RequestBody ApiRequest<CandidateRequest> request,
            @CurrentUser UserPrincipal agency) {

        CandidateResponse response = candidateService.createOrUpdateCandidate(request.getData(), agency.getId());
        String message = request.getData().getId() == null ? "Candidate created successfully" : "Candidate updated successfully";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @Operation(summary = "Get Candidate by ID", description = "Get candidate details by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CandidateResponse>> getCandidateById(
            @PathVariable Long id,
            @CurrentUser UserPrincipal agency) {

        CandidateResponse response = candidateService.getCandidateById(id, agency.getId());
        return ResponseEntity.ok(ApiResponse.success("Candidate retrieved", response));
    }

    @Operation(summary = "Get All Candidates", description = "Get paginated list of all candidates (filter by status)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageRes<CandidateResponse>>> getAllCandidates(
            @RequestParam(required = false) Boolean status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @CurrentUser UserPrincipal agency) {

        Pageable pageable = Pages.toPageable(page, size);
        Page<CandidateResponse> candidatePage = candidateService.getAllCandidates(agency.getId(), status, pageable);
        PageRes<CandidateResponse> response = Pages.of(candidatePage);

        String message = status == null ? "All candidates retrieved" :
                (status ? "Enabled candidates retrieved" : "Disabled candidates retrieved");
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @Operation(summary = "Toggle Candidate Status", description = "Enable or disable a candidate (toggles current status)")
    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<ApiResponse<CandidateResponse>> toggleCandidateStatus(
            @PathVariable Long id,
            @CurrentUser UserPrincipal agency) {

        CandidateResponse response = candidateService.toggleCandidateStatus(id, agency.getId());
        String message = response.getIsEnabled() ? "Candidate enabled successfully" : "Candidate disabled successfully";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @Operation(summary = "Delete Candidate", description = "Delete a candidate profile")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCandidate(
            @PathVariable Long id,
            @CurrentUser UserPrincipal agency) {

        candidateService.deleteCandidate(id, agency.getId());
        return ResponseEntity.ok(ApiResponse.success("Candidate deleted successfully", null));
    }
}