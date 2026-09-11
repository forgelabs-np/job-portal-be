package com.jobportal.v1.controller;

import com.jobportal.v1.dto.ApiRequest;
import com.jobportal.v1.dto.ApiResponse;
import com.jobportal.v1.dto.PageRes;
import com.jobportal.v1.dto.lead.request.LeadRequest;
import com.jobportal.v1.dto.lead.response.LeadResponse;
import com.jobportal.v1.enums.LeadSubject;
import com.jobportal.v1.service.LeadService;
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

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/leads")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Leads", description = "Lead Management APIs")
public class LeadController {

    private final LeadService leadService;

    @Operation(summary = "Submit Lead",
            description = "Public endpoint to submit a new lead from the landing page. No authentication required.")
    @PostMapping
    public ResponseEntity<ApiResponse<LeadResponse>> submitLead(
            @Valid @RequestBody ApiRequest<LeadRequest> request) {

        LeadResponse response = leadService.submitLead(request.getData());
        return ResponseUtil.created("Lead submitted successfully", response);
    }


    @Operation(summary = "Get All Leads",
            description = "Get all leads with filters and pagination. Admin/Staff only.")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<PageRes<LeadResponse>>> getAllLeads(
            @RequestParam(required = false) String fullName,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) LeadSubject subject,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) Boolean isProcessed,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<LeadResponse> leads = leadService.getAllLeads(fullName, email, subject, isRead, isProcessed, pageable);
        PageRes<LeadResponse> response = Pages.of(leads);

        return ResponseUtil.ok("Leads retrieved successfully", response);
    }

    @Operation(summary = "Get Lead by ID",
            description = "Get single lead details. Admin/Staff only.")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<LeadResponse>> getLeadById(@PathVariable Long id) {
        LeadResponse response = leadService.getLeadById(id);
        return ResponseUtil.ok("Lead retrieved successfully", response);
    }

    @Operation(summary = "Update Lead",
            description = "Update lead details. Admin/Staff only.")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<LeadResponse>> updateLead(
            @PathVariable Long id,
            @Valid @RequestBody ApiRequest<LeadRequest> request) {

        LeadResponse response = leadService.updateLead(id, request.getData());
        return ResponseUtil.ok("Lead updated successfully", response);
    }

    @Operation(summary = "Mark Lead as Read",
            description = "Mark a lead as read. Admin/Staff only.")
    @PatchMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<LeadResponse>> markAsRead(@PathVariable Long id) {
        LeadResponse response = leadService.markAsRead(id);
        return ResponseUtil.ok("Lead marked as read", response);
    }

    @Operation(summary = "Mark Lead as Processed",
            description = "Mark a lead as processed. Admin/Staff only.")
    @PatchMapping("/{id}/processed")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<LeadResponse>> markAsProcessed(@PathVariable Long id) {
        LeadResponse response = leadService.markAsProcessed(id);
        return ResponseUtil.ok("Lead marked as processed", response);
    }

    @Operation(summary = "Delete Lead",
            description = "Delete a lead. Admin/Staff only.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<Void>> deleteLead(@PathVariable Long id) {
        leadService.deleteLead(id);
        return ResponseUtil.ok("Lead deleted successfully");
    }

    @Operation(summary = "Get Unread Lead Count",
            description = "Get count of unread leads. Admin/Staff only.")
    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount() {
        long count = leadService.getUnreadCount();
        return ResponseUtil.ok("Unread lead count retrieved", count);
    }

    @Operation(summary = "Get Unprocessed Lead Count",
            description = "Get count of unprocessed leads. Admin/Staff only.")
    @GetMapping("/unprocessed-count")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<Long>> getUnprocessedCount() {
        long count = leadService.getUnprocessedCount();
        return ResponseUtil.ok("Unprocessed lead count retrieved", count);
    }
}
