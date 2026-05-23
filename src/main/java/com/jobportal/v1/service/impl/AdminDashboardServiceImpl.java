package com.jobportal.v1.service.impl;

import com.jobportal.v1.dto.admin.response.AdminDashboardResponse;
import com.jobportal.v1.dto.dashboard.response.*;
import com.jobportal.v1.mapper.AdminDashboardMapper;
import com.jobportal.v1.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final AdminDashboardMapper dashboardMapper;

    @Value("${app.dashboard.recent-limit:5}")
    private int recentLimit;

    @Value("${app.dashboard.weekly-days:7}")
    private int weeklyDays;

    @Value("${app.dashboard.day-format:EEE}")
    private String dayFormat;

    private DateTimeFormatter getDayFormatter() {
        return DateTimeFormatter.ofPattern(dayFormat);
    }

    @Override
    public AdminDashboardResponse getAdminDashboard() {
        long startTime = System.currentTimeMillis();

        // 1. Job aggregate stats
        Map<String, Object> jobStats = dashboardMapper.getJobAggregateStats();
        Long totalJobs = jobStats != null && jobStats.get("total_jobs") != null ?
                ((Number) jobStats.get("total_jobs")).longValue() : 0L;
        Long totalSlots = jobStats != null && jobStats.get("total_slots") != null ?
                ((Number) jobStats.get("total_slots")).longValue() : 0L;
        Long filledSlots = jobStats != null && jobStats.get("filled_slots") != null ?
                ((Number) jobStats.get("filled_slots")).longValue() : 0L;

        // 2. Candidate aggregate stats
        Map<String, Object> candidateStats = dashboardMapper.getCandidateAggregateStats("SELF_REGISTERED");
        Long totalSelfCandidates = candidateStats != null && candidateStats.get("total_self_candidates") != null ?
                ((Number) candidateStats.get("total_self_candidates")).longValue() : 0L;
        Long activeSelfCandidates = candidateStats != null && candidateStats.get("active_self_candidates") != null ?
                ((Number) candidateStats.get("active_self_candidates")).longValue() : 0L;
        Long inactiveSelfCandidates = candidateStats != null && candidateStats.get("inactive_self_candidates") != null ?
                ((Number) candidateStats.get("inactive_self_candidates")).longValue() : 0L;
        Long completeProfileSelfCandidates = candidateStats != null && candidateStats.get("complete_profile_self_candidates") != null ?
                ((Number) candidateStats.get("complete_profile_self_candidates")).longValue() : 0L;

        // 3. Agency aggregate stats
        Map<String, Object> agencyStats = dashboardMapper.getAgencyAggregateStats("AGENCY");
        Long totalAgencies = agencyStats != null && agencyStats.get("total_agencies") != null ?
                ((Number) agencyStats.get("total_agencies")).longValue() : 0L;
        Long pendingAgencies = agencyStats != null && agencyStats.get("pending_agencies") != null ?
                ((Number) agencyStats.get("pending_agencies")).longValue() : 0L;
        Long approvedAgencies = agencyStats != null && agencyStats.get("approved_agencies") != null ?
                ((Number) agencyStats.get("approved_agencies")).longValue() : 0L;
        Long rejectedAgencies = agencyStats != null && agencyStats.get("rejected_agencies") != null ?
                ((Number) agencyStats.get("rejected_agencies")).longValue() : 0L;

        // 4. Job status distribution
        List<Map<String, Object>> statusCounts = dashboardMapper.getJobStatusCounts();
        Map<String, Long> jobStatusMap = new HashMap<>();
        for (Map<String, Object> row : statusCounts) {
            jobStatusMap.put(row.get("status").toString(), ((Number) row.get("count")).longValue());
        }

        Long openJobs = jobStatusMap.getOrDefault("OPEN", 0L);
        Long completedJobs = jobStatusMap.getOrDefault("COMPLETED", 0L);
        Long closedJobs = jobStatusMap.getOrDefault("CLOSED", 0L);
        Long cancelledJobs = jobStatusMap.getOrDefault("CANCELLED", 0L);

        // 5. Recent jobs
        List<Map<String, Object>> recentJobsData = dashboardMapper.getRecentJobs(recentLimit);
        List<RecentJobDemand> recentJobs = recentJobsData.stream()
                .map(job -> RecentJobDemand.builder()
                        .id(((Number) job.get("id")).longValue())
                        .title(job.get("title").toString())
                        .country(job.get("country") != null ? job.get("country").toString() : null)
                        .totalSlots(((Number) job.get("total_slots")).intValue())
                        .filledSlots(((Number) job.get("filled_slots")).intValue())
                        .remainingSlots(((Number) job.get("remaining_slots")).intValue())
                        .appliedCount(((Number) job.get("applied_count")).intValue())
                        .status(job.get("status").toString())
                        .createdAt(job.get("created_at").toString())
                        .build())
                .collect(Collectors.toList());

        // 6. Recent agencies
        List<Map<String, Object>> recentAgenciesData = dashboardMapper.getRecentAgencies("AGENCY", recentLimit);
        List<RecentAgency> recentAgencies = recentAgenciesData.stream()
                .map(agency -> RecentAgency.builder()
                        .id(((Number) agency.get("id")).longValue())
                        .fullName(agency.get("full_name").toString())
                        .email(agency.get("email").toString())
                        .approvalStatus(agency.get("approval_status").toString())
                        .createdAt(agency.get("created_at").toString())
                        .build())
                .collect(Collectors.toList());

        // 7. Weekly activity
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(weeklyDays).withHour(0).withMinute(0).withSecond(0);

        Map<String, Long> jobCountsMap = new HashMap<>();
        List<Map<String, Object>> jobCounts = dashboardMapper.getDailyJobCounts(sevenDaysAgo);
        for (Map<String, Object> row : jobCounts) {
            String date = row.get("date").toString();
            Long count = ((Number) row.get("count")).longValue();
            jobCountsMap.put(date, count);
        }

        Map<String, Long> agencyCountsMap = new HashMap<>();
        List<Map<String, Object>> agencyCounts = dashboardMapper.getDailyAgencyCounts("AGENCY", sevenDaysAgo);
        for (Map<String, Object> row : agencyCounts) {
            String date = row.get("date").toString();
            Long count = ((Number) row.get("count")).longValue();
            agencyCountsMap.put(date, count);
        }

        List<String> days = new ArrayList<>();
        List<Long> jobsCreated = new ArrayList<>();
        List<Long> agenciesJoined = new ArrayList<>();

        for (int i = weeklyDays - 1; i >= 0; i--) {
            LocalDateTime dayStart = LocalDateTime.now().minusDays(i);
            String dayKey = dayStart.toLocalDate().toString();
            days.add(dayStart.format(getDayFormatter()));
            jobsCreated.add(jobCountsMap.getOrDefault(dayKey, 0L));
            agenciesJoined.add(agencyCountsMap.getOrDefault(dayKey, 0L));
        }

        WeeklyActivity weeklyActivity = WeeklyActivity.builder()
                .days(days)
                .jobsCreated(jobsCreated)
                .agenciesJoined(agenciesJoined)
                .build();

        // 8. Build Stats
        DashboardStats stats = DashboardStats.builder()
                .totalAgencies(totalAgencies)
                .pendingAgencies(pendingAgencies)
                .approvedAgencies(approvedAgencies)
                .rejectedAgencies(rejectedAgencies)
                .totalSelfCandidates(totalSelfCandidates)
                .activeSelfCandidates(activeSelfCandidates)
                .inactiveSelfCandidates(inactiveSelfCandidates)
                .completeProfileSelfCandidates(completeProfileSelfCandidates)
                .totalJobs(totalJobs)
                .openJobs(openJobs)
                .completedJobs(completedJobs)
                .closedJobs(closedJobs)
                .cancelledJobs(cancelledJobs)
                .totalSlots(totalSlots)
                .filledSlots(filledSlots)
                .remainingSlots(totalSlots - filledSlots)
                .build();

        // 9. Build Job Status Distribution
        JobStatusDistribution jobStatusDistribution = JobStatusDistribution.builder()
                .open(openJobs)
                .completed(completedJobs)
                .closed(closedJobs)
                .cancelled(cancelledJobs)
                .build();

        long endTime = System.currentTimeMillis();
        log.info("Admin Dashboard loaded in {} ms", (endTime - startTime));

        return AdminDashboardResponse.builder()
                .stats(stats)
                .recentJobs(recentJobs)
                .recentAgencies(recentAgencies)
                .jobStatusDistribution(jobStatusDistribution)
                .weeklyActivity(weeklyActivity)
                .build();
    }
}