package com.jobportal.v1.dto.agency.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AgencyDashboardResponse {
    private AgencyDashboardStats stats;
    private List<AgencyRecentApplication> recentApplications;
    private List<AgencyRecentCandidate> recentCandidates;
    private List<AgencyRecentJob> recentJobs;
    private AgencyApplicationStatusDistribution statusDistribution;
    private AgencyWeeklyActivity weeklyActivity;
}

