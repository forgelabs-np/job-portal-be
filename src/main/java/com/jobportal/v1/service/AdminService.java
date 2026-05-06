package com.jobportal.v1.service;

import com.jobportal.v1.dto.agency.request.AgencyActionRequest;
import com.jobportal.v1.dto.agency.response.AgencyApprovalResponse;
import com.jobportal.v1.enums.ApprovalStatus;

import java.util.List;

public interface AdminService {

    AgencyApprovalResponse processAgencyAction(AgencyActionRequest request, Long adminId);

    List<AgencyApprovalResponse> getAgenciesByStatus(ApprovalStatus status);
}