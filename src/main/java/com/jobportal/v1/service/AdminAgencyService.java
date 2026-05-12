package com.jobportal.v1.service;

import com.jobportal.v1.dto.admin.request.DocumentApprovalRequest;
import com.jobportal.v1.dto.agency.request.ProfileApprovalRequest;
import com.jobportal.v1.dto.agency.response.AgencyDocumentResponse;
import com.jobportal.v1.dto.agency.response.AgencyProfileResponse;
import com.jobportal.v1.enums.ApprovalStatus;

import java.util.List;

public interface AdminAgencyService {

    // Document Management
    List<AgencyDocumentResponse> getPendingDocuments();

    List<AgencyDocumentResponse> getDocumentsByAgency(Long agencyId);

    AgencyDocumentResponse processDocumentApproval(DocumentApprovalRequest request, Long adminId);

    // Profile Management
    List<AgencyProfileResponse> getProfilesByStatus(ApprovalStatus status);

    AgencyProfileResponse processProfileApproval(ProfileApprovalRequest request, Long adminId);

    AgencyProfileResponse getProfileDetails(Long userId);
}