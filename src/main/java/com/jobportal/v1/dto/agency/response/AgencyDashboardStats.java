package com.jobportal.v1.dto.agency.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgencyDashboardStats {
    // Candidate Stats
    private Long totalCandidates;
    private Long enabledCandidates;
    private Long disabledCandidates;

    // Application Stats
    private Long totalApplications;
    private Long pendingApplications;
    private Long reviewedApplications;
    private Long shortlistedApplications;
    private Long rejectedApplications;
    private Long withdrawnApplications;

    // Job Stats
    private Long assignedJobs;
    private Long openJobs;
    private Long completedJobs;

    // Success Rate
    private Double approvalRate;
}
