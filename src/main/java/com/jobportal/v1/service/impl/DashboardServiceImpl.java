package com.jobportal.v1.service.impl;

import com.jobportal.v1.dto.admin.response.AdminDashboardResponse;
import com.jobportal.v1.dto.dashboard.response.*;
import com.jobportal.v1.entity.JobDemand;
import com.jobportal.v1.entity.User;
import com.jobportal.v1.enums.ApprovalStatus;
import com.jobportal.v1.enums.CandidateType;
import com.jobportal.v1.enums.JobStatus;
import com.jobportal.v1.enums.RoleEnum;
import com.jobportal.v1.repository.CandidateRepository;
import com.jobportal.v1.repository.JobDemandRepository;
import com.jobportal.v1.repository.UserRepository;
import com.jobportal.v1.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private final UserRepository userRepository;
    private final JobDemandRepository jobDemandRepository;
    private final CandidateRepository candidateRepository;

    @Override
    public AdminDashboardResponse getAdminDashboard() {
        // Agency Stats
        Long totalAgencies = userRepository.countByRole(RoleEnum.AGENCY);
        Long pendingAgencies = userRepository.countByRoleAndApprovalStatus(RoleEnum.AGENCY, ApprovalStatus.PENDING);
        Long approvedAgencies = userRepository.countByRoleAndApprovalStatus(RoleEnum.AGENCY, ApprovalStatus.APPROVED);
        Long rejectedAgencies = userRepository.countByRoleAndApprovalStatus(RoleEnum.AGENCY, ApprovalStatus.REJECTED);

        Long totalSelfCandidates = candidateRepository.countByCandidateType(CandidateType.SELF_REGISTERED);
        Long activeSelfCandidates = candidateRepository.countByCandidateTypeAndIsEnabled(CandidateType.SELF_REGISTERED, true);
        Long inactiveSelfCandidates = candidateRepository.countByCandidateTypeAndIsEnabled(CandidateType.SELF_REGISTERED, false);
        Long completeProfileSelfCandidates = candidateRepository.countByCandidateTypeAndProfileCompleteTrue(CandidateType.SELF_REGISTERED);

        // Job Stats
        Long totalJobs = jobDemandRepository.countAllActiveJobs();
        Long openJobs = jobDemandRepository.countByStatus(JobStatus.OPEN);
        Long completedJobs = jobDemandRepository.countByStatus(JobStatus.COMPLETED);
        Long closedJobs = jobDemandRepository.countByStatus(JobStatus.CLOSED);
        Long cancelledJobs = jobDemandRepository.countByStatus(JobStatus.CANCELLED);

        // Slot Stats
        Long totalSlots = jobDemandRepository.sumTotalSlots();
        Long filledSlots = jobDemandRepository.sumFilledSlots();
        Long remainingSlots = totalSlots - filledSlots;

        DashboardStats stats = DashboardStats.builder()
                .totalAgencies(totalAgencies)
                .pendingAgencies(pendingAgencies)
                .approvedAgencies(approvedAgencies)
                .activeSelfCandidates(activeSelfCandidates)
                .inactiveSelfCandidates(inactiveSelfCandidates)
                .completeProfileSelfCandidates(completeProfileSelfCandidates)
                .totalSelfCandidates(totalSelfCandidates)
                .rejectedAgencies(rejectedAgencies)
                .totalJobs(totalJobs)
                .openJobs(openJobs)
                .completedJobs(completedJobs)
                .closedJobs(closedJobs)
                .cancelledJobs(cancelledJobs)
                .totalSlots(totalSlots)
                .filledSlots(filledSlots)
                .remainingSlots(remainingSlots)
                .build();

        // Recent Jobs (last 5)
        List<JobDemand> recentJobsList = jobDemandRepository.findRecentJobs(PageRequest.of(0, 5));
        List<RecentJobDemand> recentJobs = recentJobsList.stream()
                .map(job -> RecentJobDemand.builder()
                        .id(job.getId())
                        .title(job.getTitle())
                        .country(job.getCountry() != null ? job.getCountry().getName() : null)
                        .totalSlots(job.getTotalSlots())
                        .filledSlots(job.getFilledSlots())
                        .remainingSlots(job.getRemainingSlots())
                        .appliedCount(job.getAppliedCount())
                        .status(job.getStatus().name())
                        .createdAt(job.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                        .build())
                .collect(Collectors.toList());

        // Recent Agencies (last 5)
        List<User> recentAgenciesList = userRepository.findRecentByRole(RoleEnum.AGENCY, PageRequest.of(0, 5));
        List<RecentAgency> recentAgencies = recentAgenciesList.stream()
                .map(agency -> RecentAgency.builder()
                        .id(agency.getId())
                        .fullName(agency.getFullName())
                        .email(agency.getEmail())
                        .approvalStatus(agency.getApprovalStatus().name())
                        .createdAt(agency.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                        .build())
                .collect(Collectors.toList());

        // Job Status Distribution
        JobStatusDistribution jobStatusDistribution = JobStatusDistribution.builder()
                .open(openJobs)
                .completed(completedJobs)
                .closed(closedJobs)
                .cancelled(cancelledJobs)
                .build();

        // Weekly Activity (last 7 days)
        List<String> days = new java.util.ArrayList<>();
        List<Long> jobsCreated = new java.util.ArrayList<>();
        List<Long> agenciesJoined = new java.util.ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            LocalDateTime dayStart = LocalDateTime.now().minusDays(i).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime dayEnd = dayStart.withHour(23).withMinute(59).withSecond(59);

            days.add(dayStart.format(DateTimeFormatter.ofPattern("EEE")));

            Long jobsCount = jobDemandRepository.countByDateRange(dayStart, dayEnd);
            jobsCreated.add(jobsCount);

            Long agenciesCount = userRepository.countByRoleAndDateRange(RoleEnum.AGENCY, dayStart, dayEnd);
            agenciesJoined.add(agenciesCount);
        }

        WeeklyActivity weeklyActivity = WeeklyActivity.builder()
                .days(days)
                .jobsCreated(jobsCreated)
                .agenciesJoined(agenciesJoined)
                .build();

        return AdminDashboardResponse.builder()
                .stats(stats)
                .recentJobs(recentJobs)
                .recentAgencies(recentAgencies)
                .jobStatusDistribution(jobStatusDistribution)
                .weeklyActivity(weeklyActivity)
                .build();
    }
}