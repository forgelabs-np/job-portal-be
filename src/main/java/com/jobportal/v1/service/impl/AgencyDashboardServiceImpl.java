package com.jobportal.v1.service.impl;

import com.jobportal.v1.dto.agency.response.*;
import com.jobportal.v1.dto.dashboard.response.*;
import com.jobportal.v1.mapper.AgencyDashboardMapper;
import com.jobportal.v1.service.AgencyDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgencyDashboardServiceImpl implements AgencyDashboardService {

    private final AgencyDashboardMapper agencyDashboardMapper;

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
    public AgencyDashboardResponse getAgencyDashboard(Long agencyId) {
        long startTime = System.currentTimeMillis();

        // 1. Candidate stats
        Map<String, Object> candidateStats = agencyDashboardMapper.getCandidateStats(agencyId);
        Long totalCandidates = candidateStats != null && candidateStats.get("total") != null ?
                ((Number) candidateStats.get("total")).longValue() : 0L;
        Long enabledCandidates = candidateStats != null && candidateStats.get("enabled") != null ?
                ((Number) candidateStats.get("enabled")).longValue() : 0L;
        Long disabledCandidates = candidateStats != null && candidateStats.get("disabled") != null ?
                ((Number) candidateStats.get("disabled")).longValue() : 0L;

        // 2. Application status counts
        List<Map<String, Object>> statusCounts = agencyDashboardMapper.getApplicationStatusCounts(agencyId);
        Long totalApplications = 0L, pendingApplications = 0L, reviewedApplications = 0L;
        Long shortlistedApplications = 0L, rejectedApplications = 0L, withdrawnApplications = 0L;

        for (Map<String, Object> row : statusCounts) {
            String status = row.get("status").toString();
            Long count = ((Number) row.get("count")).longValue();
            totalApplications += count;
            switch (status) {
                case "PENDING": pendingApplications = count; break;
                case "REVIEWED": reviewedApplications = count; break;
                case "SHORTLISTED": shortlistedApplications = count; break;
                case "REJECTED": rejectedApplications = count; break;
                case "WITHDRAWN": withdrawnApplications = count; break;
                default: break;
            }
        }

        // 3. Assigned jobs stats
        Map<String, Object> jobStats = agencyDashboardMapper.getAssignedJobsStats(agencyId);
        Long totalAssignedJobs = jobStats != null && jobStats.get("total") != null ?
                ((Number) jobStats.get("total")).longValue() : 0L;
        Long openJobs = jobStats != null && jobStats.get("open_jobs") != null ?
                ((Number) jobStats.get("open_jobs")).longValue() : 0L;
        Long completedJobs = jobStats != null && jobStats.get("completed_jobs") != null ?
                ((Number) jobStats.get("completed_jobs")).longValue() : 0L;

        // 4. Recent applications
        List<Map<String, Object>> recentAppsData = agencyDashboardMapper.getRecentApplications(agencyId, recentLimit);
        List<AgencyRecentApplication> recentApplications = recentAppsData.stream()
                .map(app -> AgencyRecentApplication.builder()
                        .id(((Number) app.get("id")).longValue())
                        .jobTitle(app.get("job_title").toString())
                        .candidateName(app.get("candidate_name").toString())
                        .status(app.get("status").toString())
                        .appliedAt(app.get("applied_at").toString())
                        .build())
                .collect(Collectors.toList());

        // 5. Recent candidates
        List<Map<String, Object>> recentCandidatesData = agencyDashboardMapper.getRecentCandidates(agencyId, recentLimit);
        List<AgencyRecentCandidate> recentCandidates = recentCandidatesData.stream()
                .map(candidate -> AgencyRecentCandidate.builder()
                        .id(((Number) candidate.get("id")).longValue())
                        .fullName(candidate.get("full_name").toString())
                        .trade(candidate.get("trade") != null ? candidate.get("trade").toString() : null)
                        .isEnabled((Boolean) candidate.get("is_enabled"))
                        .createdAt(candidate.get("created_at").toString())
                        .build())
                .collect(Collectors.toList());

        // 6. Recent jobs
        List<Map<String, Object>> recentJobsData = agencyDashboardMapper.getRecentJobs(agencyId, recentLimit);
        List<AgencyRecentJob> recentJobs = recentJobsData.stream()
                .map(job -> AgencyRecentJob.builder()
                        .id(((Number) job.get("id")).longValue())
                        .title(job.get("title").toString())
                        .country(job.get("country") != null ? job.get("country").toString() : null)
                        .totalSlots(((Number) job.get("total_slots")).intValue())
                        .remainingSlots(((Number) job.get("remaining_slots")).intValue())
                        .status(job.get("status").toString())
                        .build())
                .collect(Collectors.toList());

        // 7. Weekly activity
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(weeklyDays).withHour(0).withMinute(0).withSecond(0);

        Map<String, Long> submittedMap = new HashMap<>();
        List<Map<String, Object>> submittedWeekly = agencyDashboardMapper.getWeeklySubmittedCounts(agencyId, sevenDaysAgo);
        for (Map<String, Object> row : submittedWeekly) {
            submittedMap.put(row.get("date").toString(), ((Number) row.get("count")).longValue());
        }

        Map<String, Long> shortlistedMap = new HashMap<>();
        List<Map<String, Object>> shortlistedWeekly = agencyDashboardMapper.getWeeklyShortlistedCounts(agencyId, sevenDaysAgo);
        for (Map<String, Object> row : shortlistedWeekly) {
            shortlistedMap.put(row.get("date").toString(), ((Number) row.get("count")).longValue());
        }

        List<String> days = new ArrayList<>();
        List<Long> applicationsSubmitted = new ArrayList<>();
        List<Long> applicationsShortlisted = new ArrayList<>();

        for (int i = weeklyDays - 1; i >= 0; i--) {
            LocalDateTime dayStart = LocalDateTime.now().minusDays(i);
            String dayKey = dayStart.toLocalDate().toString();
            days.add(dayStart.format(getDayFormatter()));
            applicationsSubmitted.add(submittedMap.getOrDefault(dayKey, 0L));
            applicationsShortlisted.add(shortlistedMap.getOrDefault(dayKey, 0L));
        }

        AgencyWeeklyActivity weeklyActivity = AgencyWeeklyActivity.builder()
                .days(days)
                .applicationsSubmitted(applicationsSubmitted)
                .applicationsShortlisted(applicationsShortlisted)
                .build();

        // 8. Calculate approval rate
        Double approvalRate = totalApplications > 0
                ? (shortlistedApplications.doubleValue() / totalApplications.doubleValue()) * 100
                : 0.0;

        // 9. Build Stats
        AgencyDashboardStats stats = AgencyDashboardStats.builder()
                .totalCandidates(totalCandidates)
                .enabledCandidates(enabledCandidates)
                .disabledCandidates(disabledCandidates)
                .totalApplications(totalApplications)
                .pendingApplications(pendingApplications)
                .reviewedApplications(reviewedApplications)
                .shortlistedApplications(shortlistedApplications)
                .rejectedApplications(rejectedApplications)
                .withdrawnApplications(withdrawnApplications)
                .assignedJobs(totalAssignedJobs)
                .openJobs(openJobs)
                .completedJobs(completedJobs)
                .approvalRate(Math.round(approvalRate * 100.0) / 100.0)
                .build();

        AgencyApplicationStatusDistribution statusDistribution = AgencyApplicationStatusDistribution.builder()
                .pending(pendingApplications)
                .reviewed(reviewedApplications)
                .shortlisted(shortlistedApplications)
                .rejected(rejectedApplications)
                .withdrawn(withdrawnApplications)
                .build();

        long endTime = System.currentTimeMillis();
        log.info("Agency Dashboard loaded in {} ms for agency: {}", (endTime - startTime), agencyId);

        return AgencyDashboardResponse.builder()
                .stats(stats)
                .recentApplications(recentApplications)
                .recentCandidates(recentCandidates)
                .recentJobs(recentJobs)
                .statusDistribution(statusDistribution)
                .weeklyActivity(weeklyActivity)
                .build();
    }
}