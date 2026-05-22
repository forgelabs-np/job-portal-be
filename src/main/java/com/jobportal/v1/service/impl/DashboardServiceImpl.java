package com.jobportal.v1.service.impl;

import com.jobportal.v1.dto.admin.response.AdminDashboardResponse;
import com.jobportal.v1.dto.dashboard.response.*;
import com.jobportal.v1.entity.JobDemand;
import com.jobportal.v1.entity.User;
import com.jobportal.v1.enums.CandidateType;
import com.jobportal.v1.enums.JobStatus;
import com.jobportal.v1.enums.RoleEnum;
import com.jobportal.v1.repository.CandidateRepository;
import com.jobportal.v1.repository.JobDemandRepository;
import com.jobportal.v1.repository.UserRepository;
import com.jobportal.v1.repository.projection.*;
import com.jobportal.v1.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
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
public class DashboardServiceImpl implements DashboardService {

    private static final int RECENT_LIMIT = 5;
    private static final int WEEKLY_DAYS = 7;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("EEE");

    private final UserRepository userRepository;
    private final JobDemandRepository jobDemandRepository;
    private final CandidateRepository candidateRepository;

    @Override
    public AdminDashboardResponse getAdminDashboard() {
        JobAggregateStatsProjection jobStats = jobDemandRepository.getJobAggregateStats();
        CandidateAggregateStatsProjection candidateStats = candidateRepository.getCandidateAggregateStats(CandidateType.SELF_REGISTERED);
        AgencyAggregateStatsProjection agencyStats = userRepository.getAgencyAggregateStats(RoleEnum.AGENCY);

        Map<String, Long> jobStatusMap = getJobStatusMap();
        Long openJobs = jobStatusMap.getOrDefault("OPEN", 0L);
        Long completedJobs = jobStatusMap.getOrDefault("COMPLETED", 0L);
        Long closedJobs = jobStatusMap.getOrDefault("CLOSED", 0L);
        Long cancelledJobs = jobStatusMap.getOrDefault("CANCELLED", 0L);

        return AdminDashboardResponse.builder()
                .stats(buildDashboardStats(jobStats, candidateStats, agencyStats, openJobs, completedJobs, closedJobs, cancelledJobs))
                .recentJobs(getRecentJobs())
                .recentAgencies(getRecentAgencies())
                .jobStatusDistribution(buildJobStatusDistribution(openJobs, completedJobs, closedJobs, cancelledJobs))
                .weeklyActivity(buildWeeklyActivity())
                .build();
    }

    private Map<String, Long> getJobStatusMap() {
        List<JobStatusCountProjection> jobStatusCounts = jobDemandRepository.getJobStatusCounts();
        Map<String, Long> statusMap = new HashMap<>();
        for (JobStatusCountProjection projection : jobStatusCounts) {
            statusMap.put(projection.getStatus(), projection.getCount());
        }
        return statusMap;
    }

    private DashboardStats buildDashboardStats(
            JobAggregateStatsProjection jobStats,
            CandidateAggregateStatsProjection candidateStats,
            AgencyAggregateStatsProjection agencyStats,
            Long openJobs, Long completedJobs, Long closedJobs, Long cancelledJobs) {

        Long totalSlots = jobStats.getTotalSlots();
        Long filledSlots = jobStats.getFilledSlots();

        return DashboardStats.builder()
                .totalAgencies(agencyStats.getTotalAgencies())
                .pendingAgencies(agencyStats.getPendingAgencies())
                .approvedAgencies(agencyStats.getApprovedAgencies())
                .rejectedAgencies(agencyStats.getRejectedAgencies())
                .totalSelfCandidates(candidateStats.getTotalSelfCandidates())
                .activeSelfCandidates(candidateStats.getActiveSelfCandidates())
                .inactiveSelfCandidates(candidateStats.getInactiveSelfCandidates())
                .completeProfileSelfCandidates(candidateStats.getCompleteProfileSelfCandidates())
                .totalJobs(jobStats.getTotalJobs())
                .openJobs(openJobs)
                .completedJobs(completedJobs)
                .closedJobs(closedJobs)
                .cancelledJobs(cancelledJobs)
                .totalSlots(totalSlots)
                .filledSlots(filledSlots)
                .remainingSlots(totalSlots - filledSlots)
                .build();
    }

    private List<RecentJobDemand> getRecentJobs() {
        return jobDemandRepository.findRecentJobs(PageRequest.of(0, RECENT_LIMIT))
                .stream()
                .map(this::toRecentJobDemand)
                .toList();
    }

    private RecentJobDemand toRecentJobDemand(JobDemand job) {
        String countryName = null;
        if (job.getCountry() != null) {
            countryName = job.getCountry().getName();
        }

        return RecentJobDemand.builder()
                .id(job.getId())
                .title(job.getTitle())
                .country(countryName)
                .totalSlots(job.getTotalSlots())
                .filledSlots(job.getFilledSlots())
                .remainingSlots(job.getRemainingSlots())
                .appliedCount(job.getAppliedCount())
                .status(job.getStatus().name())
                .createdAt(job.getCreatedAt().format(DATE_TIME_FORMATTER))
                .build();
    }

    private List<RecentAgency> getRecentAgencies() {
        return userRepository.findRecentByRole(RoleEnum.AGENCY, PageRequest.of(0, RECENT_LIMIT))
                .stream()
                .map(this::toRecentAgency)
                .toList();
    }

    private RecentAgency toRecentAgency(User agency) {
        return RecentAgency.builder()
                .id(agency.getId())
                .fullName(agency.getFullName())
                .email(agency.getEmail())
                .approvalStatus(agency.getApprovalStatus().name())
                .createdAt(agency.getCreatedAt().format(DATE_TIME_FORMATTER))
                .build();
    }

    private JobStatusDistribution buildJobStatusDistribution(Long openJobs, Long completedJobs, Long closedJobs, Long cancelledJobs) {
        return JobStatusDistribution.builder()
                .open(openJobs)
                .completed(completedJobs)
                .closed(closedJobs)
                .cancelled(cancelledJobs)
                .build();
    }

    private WeeklyActivity buildWeeklyActivity() {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(WEEKLY_DAYS).withHour(0).withMinute(0).withSecond(0);

        Map<String, Long> jobCounts = jobDemandRepository.getDailyJobCounts(sevenDaysAgo)
                .stream()
                .collect(Collectors.toMap(
                        p -> p.getDate().substring(0, 10),
                        DailyJobCountProjection::getCount,
                        (a, b) -> a
                ));

        Map<String, Long> agencyCounts = userRepository.getDailyAgencyCounts(RoleEnum.AGENCY, sevenDaysAgo)
                .stream()
                .collect(Collectors.toMap(
                        p -> p.getDate().substring(0, 10),
                        DailyAgencyCountProjection::getCount,
                        (a, b) -> a
                ));

        List<String> days = new ArrayList<>();
        List<Long> jobsCreated = new ArrayList<>();
        List<Long> agenciesJoined = new ArrayList<>();

        for (int i = WEEKLY_DAYS - 1; i >= 0; i--) {
            String dayKey = LocalDateTime.now().minusDays(i).toLocalDate().toString();
            days.add(LocalDateTime.now().minusDays(i).format(DAY_FORMATTER));
            jobsCreated.add(jobCounts.getOrDefault(dayKey, 0L));
            agenciesJoined.add(agencyCounts.getOrDefault(dayKey, 0L));
        }

        return WeeklyActivity.builder()
                .days(days)
                .jobsCreated(jobsCreated)
                .agenciesJoined(agenciesJoined)
                .build();
    }
}