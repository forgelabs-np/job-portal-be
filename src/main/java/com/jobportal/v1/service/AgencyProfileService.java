package com.jobportal.v1.service;

import com.jobportal.v1.dto.agency.request.AgencyProfileRequest;
import com.jobportal.v1.dto.agency.response.AgencyProfileResponse;
import com.jobportal.v1.dto.agency.response.AgencyDashboardResponse;

public interface AgencyProfileService {
    AgencyProfileResponse createOrUpdateProfile(Long userId, AgencyProfileRequest request);

    AgencyProfileResponse getProfileByUserId(Long userId);

    AgencyProfileResponse getMyProfile(Long userId);

    boolean isProfileComplete(Long userId);

    AgencyDashboardResponse getAgencyDashboard(Long agencyId);
}