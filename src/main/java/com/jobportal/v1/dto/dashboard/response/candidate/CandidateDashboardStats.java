package com.jobportal.v1.dto.dashboard.response.candidate;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CandidateDashboardStats {
    // Profile Stats
    private Boolean isProfileComplete;
    private String onboardingStage;
    private Integer documentsUploaded;
    private Integer documentsApproved;
    private Integer documentsPending;
    private Integer documentsRejected;
    private Boolean allDocumentsApproved;

    // Application Stats
    private Long totalApplications;
    private Long pendingApplications;
    private Long reviewedApplications;
    private Long shortlistedApplications;
    private Long rejectedApplications;
    private Long withdrawnApplications;

    // Job Stats
    private Long totalPublicJobs;
    private Long appliedJobsCount;
    private Long availableJobsCount;
}