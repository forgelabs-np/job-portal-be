package com.jobportal.v1.service;

import com.jobportal.v1.dto.agency.response.AgencyDashboardResponse;

public interface AgencyDashboardService {
    AgencyDashboardResponse getAgencyDashboard(Long agencyId);
}