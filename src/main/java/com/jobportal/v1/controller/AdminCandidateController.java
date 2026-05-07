package com.jobportal.v1.controller;

import com.jobportal.v1.dto.ApiRequest;
import com.jobportal.v1.dto.ApiResponse;
import com.jobportal.v1.dto.admin.request.CandidateStatusUpdateRequest;
import com.jobportal.v1.dto.candidate.response.CandidateResponse;
import com.jobportal.v1.security.CurrentUser;
import com.jobportal.v1.security.UserPrincipal;
import com.jobportal.v1.service.CandidateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/candidates")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Candidates", description = "Admin Candidate Management APIs")
public class AdminCandidateController {

    private final CandidateService candidateService;

    @Operation(summary = "Update Candidate Status", description = "Admin updates candidate document statuses (PCC, SLC, Work Permit, Visa)")
    @PatchMapping("/{candidateId}/status")
    public ResponseEntity<ApiResponse<CandidateResponse>> updateCandidateStatus(
            @PathVariable Long candidateId,
            @Valid @RequestBody ApiRequest<CandidateStatusUpdateRequest> request,
            @CurrentUser UserPrincipal admin) {

        CandidateResponse response = candidateService.updateCandidateStatus(candidateId, request.getData(), admin.getId());
        return ResponseEntity.ok(ApiResponse.success("Candidate status updated successfully", response));
    }
}