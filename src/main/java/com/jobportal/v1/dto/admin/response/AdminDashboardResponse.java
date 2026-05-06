package com.jobportal.v1.dto.admin.response;

import com.jobportal.v1.dto.dashboard.response.*;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class AdminDashboardResponse {
    private DashboardStats stats;
    private List<RecentJobDemand> recentJobs;
    private List<RecentAgency> recentAgencies;
    private JobStatusDistribution jobStatusDistribution;
    private WeeklyActivity weeklyActivity;
}