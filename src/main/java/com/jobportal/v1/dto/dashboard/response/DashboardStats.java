package com.jobportal.v1.dto.dashboard.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardStats {
    // Agency Stats
    private Long totalAgencies;
    private Long pendingAgencies;
    private Long approvedAgencies;
    private Long rejectedAgencies;

    // Job Stats
    private Long totalJobs;
    private Long openJobs;
    private Long completedJobs;
    private Long closedJobs;
    private Long cancelledJobs;

    // Slot Stats
    private Long totalSlots;
    private Long filledSlots;
    private Long remainingSlots;
}