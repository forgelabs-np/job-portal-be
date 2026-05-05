package com.jobportal.v1.service;

import com.jobportal.v1.dto.agency.request.ApproveAgencyRequest;
import com.jobportal.v1.dto.agency.request.RejectAgencyRequest;
import com.jobportal.v1.dto.agency.response.AgencyApprovalResponse;

import java.util.List;

public interface AdminService {
    AgencyApprovalResponse approveAgency(ApproveAgencyRequest request, Long adminId);

    AgencyApprovalResponse rejectAgency(RejectAgencyRequest request, Long adminId);

    List<AgencyApprovalResponse> getAllPendingAgencies();

    List<AgencyApprovalResponse> getAllApprovedAgencies();

    List<AgencyApprovalResponse> getAllRejectedAgencies();
}