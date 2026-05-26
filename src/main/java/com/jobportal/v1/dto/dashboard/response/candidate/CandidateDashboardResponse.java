package com.jobportal.v1.dto.dashboard.response.candidate;

import com.jobportal.v1.dto.announcement.response.AnnouncementResponse;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class CandidateDashboardResponse {
    private CandidateDashboardStats stats;
    private List<CandidateRecentApplication> recentApplications;
    private List<CandidateRecentJob> recommendedJobs;
    private List<CandidateDocumentSummary> documentSummary;
    private ApplicationStatusDistribution statusDistribution;
    private WeeklyApplicationActivity weeklyActivity;
    private List<AnnouncementResponse> latestAnnouncements;
}