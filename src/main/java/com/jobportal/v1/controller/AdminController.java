package com.jobportal.v1.controller;

import com.jobportal.v1.dto.ApiRequest;
import com.jobportal.v1.dto.ApiResponse;
import com.jobportal.v1.dto.agency.request.ApproveAgencyRequest;
import com.jobportal.v1.dto.agency.request.RejectAgencyRequest;
import com.jobportal.v1.dto.agency.response.AgencyApprovalResponse;
import com.jobportal.v1.security.CurrentUser;
import com.jobportal.v1.security.UserPrincipal;
import com.jobportal.v1.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin Management APIs")
public class AdminController {

    private final AdminService adminService;

    @Operation(summary = "Approve Agency", description = "Admin approves a pending agency")
    @PostMapping("/agencies/approve")
    public ResponseEntity<ApiResponse<AgencyApprovalResponse>> approveAgency(
            @Valid @RequestBody ApiRequest<ApproveAgencyRequest> request,
            @CurrentUser UserPrincipal admin) {

        AgencyApprovalResponse response = adminService.approveAgency(request.getData(), admin.getId());
        return ResponseEntity.ok(ApiResponse.success("Agency approved successfully", response));
    }

    @Operation(summary = "Reject Agency", description = "Admin rejects a pending agency with reason")
    @PostMapping("/agencies/reject")
    public ResponseEntity<ApiResponse<AgencyApprovalResponse>> rejectAgency(
            @Valid @RequestBody ApiRequest<RejectAgencyRequest> request,
            @CurrentUser UserPrincipal admin) {

        AgencyApprovalResponse response = adminService.rejectAgency(request.getData(), admin.getId());
        return ResponseEntity.ok(ApiResponse.success("Agency rejected successfully", response));
    }

    @Operation(summary = "Get Pending Agencies", description = "Get all agencies pending approval")
    @GetMapping("/agencies/pending")
    public ResponseEntity<ApiResponse<List<AgencyApprovalResponse>>> getPendingAgencies() {
        List<AgencyApprovalResponse> response = adminService.getAllPendingAgencies();
        return ResponseEntity.ok(ApiResponse.success("Pending agencies retrieved", response));
    }

    @Operation(summary = "Get Approved Agencies", description = "Get all approved agencies")
    @GetMapping("/agencies/approved")
    public ResponseEntity<ApiResponse<List<AgencyApprovalResponse>>> getApprovedAgencies() {
        List<AgencyApprovalResponse> response = adminService.getAllApprovedAgencies();
        return ResponseEntity.ok(ApiResponse.success("Approved agencies retrieved", response));
    }

    @Operation(summary = "Get Rejected Agencies", description = "Get all rejected agencies")
    @GetMapping("/agencies/rejected")
    public ResponseEntity<ApiResponse<List<AgencyApprovalResponse>>> getRejectedAgencies() {
        List<AgencyApprovalResponse> response = adminService.getAllRejectedAgencies();
        return ResponseEntity.ok(ApiResponse.success("Rejected agencies retrieved", response));
    }
}