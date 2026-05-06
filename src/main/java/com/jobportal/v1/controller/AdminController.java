package com.jobportal.v1.controller;

import com.jobportal.v1.dto.ApiRequest;
import com.jobportal.v1.dto.ApiResponse;
import com.jobportal.v1.dto.admin.response.AdminDashboardResponse;
import com.jobportal.v1.dto.agency.request.AgencyActionRequest;
import com.jobportal.v1.dto.agency.response.AgencyApprovalResponse;
import com.jobportal.v1.enums.ApprovalStatus;
import com.jobportal.v1.security.CurrentUser;
import com.jobportal.v1.security.UserPrincipal;
import com.jobportal.v1.service.AdminService;
import com.jobportal.v1.service.DashboardService;
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
    private final DashboardService dashboardService;  // Add this

    @Operation(summary = "Admin Dashboard", description = "Get dashboard statistics and recent activities")
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getDashboard() {
        AdminDashboardResponse response = dashboardService.getAdminDashboard();
        return ResponseEntity.ok(ApiResponse.success("Dashboard data retrieved", response));
    }

    @Operation(summary = "Process Agency Action", description = "Approve or reject a pending agency")
    @PostMapping("/agencies/action")
    public ResponseEntity<ApiResponse<AgencyApprovalResponse>> processAgencyAction(
            @Valid @RequestBody ApiRequest<AgencyActionRequest> request,
            @CurrentUser UserPrincipal admin) {

        AgencyApprovalResponse response = adminService.processAgencyAction(request.getData(), admin.getId());
        return ResponseEntity.ok(ApiResponse.success("Agency " + request.getData().getAction().toLowerCase() + "d successfully", response));
    }

    @Operation(summary = "Get Agencies by Status", description = "Get all agencies filtered by approval status")
    @GetMapping("/agencies")
    public ResponseEntity<ApiResponse<List<AgencyApprovalResponse>>> getAgenciesByStatus(
            @RequestParam(required = false) ApprovalStatus status) {

        List<AgencyApprovalResponse> response = adminService.getAgenciesByStatus(status);
        String message = status == null ? "All agencies retrieved" : status + " agencies retrieved";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }
}