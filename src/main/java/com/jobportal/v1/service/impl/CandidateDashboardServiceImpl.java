package com.jobportal.v1.service.impl;

import com.jobportal.v1.dto.dashboard.response.candidate.*;
import com.jobportal.v1.exception.ResourceNotFoundException;
import com.jobportal.v1.mapper.CandidateDashboardMapper;
import com.jobportal.v1.service.CandidateDashboardService;
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
public class CandidateDashboardServiceImpl implements CandidateDashboardService {

    private final CandidateDashboardMapper dashboardMapper;

    @Value("${app.dashboard.recent-limit:5}")
    private int recentLimit;

    @Value("${app.dashboard.recommended-limit:5}")
    private int recommendedLimit;

    @Value("${app.dashboard.weekly-days:7}")
    private int weeklyDays;

    @Value("${app.dashboard.day-format:EEE}")
    private String dayFormat;

    private DateTimeFormatter getDayFormatter() {
        return DateTimeFormatter.ofPattern(dayFormat);
    }

    @Override
    public CandidateDashboardResponse getCandidateDashboard(Long userId) {
        long startTime = System.currentTimeMillis();

        // 1. Get candidate info
        Map<String, Object> candidateInfo = dashboardMapper.getCandidateInfo(userId);
        if (candidateInfo == null || candidateInfo.isEmpty()) {
            throw new ResourceNotFoundException("Candidate profile not found");
        }

        Long candidateId = ((Number) candidateInfo.get("candidate_id")).longValue();
        boolean isProfileComplete = candidateInfo.get("is_profile_complete") != null &&
                (Boolean) candidateInfo.get("is_profile_complete");
        String onboardingStage = candidateInfo.get("onboarding_stage") != null ?
                candidateInfo.get("onboarding_stage").toString() : "PROFILE";

        // 2. Document stats
        Map<String, Object> docStats = dashboardMapper.getDocumentStats(candidateId);
        long documentsUploaded = docStats != null && docStats.get("total") != null ?
                ((Number) docStats.get("total")).longValue() : 0L;
        long documentsApproved = docStats != null && docStats.get("approved") != null ?
                ((Number) docStats.get("approved")).longValue() : 0L;
        long documentsPending = docStats != null && docStats.get("pending") != null ?
                ((Number) docStats.get("pending")).longValue() : 0L;
        long documentsRejected = docStats != null && docStats.get("rejected") != null ?
                ((Number) docStats.get("rejected")).longValue() : 0L;

        boolean allDocumentsApproved = documentsUploaded > 0 && documentsApproved == documentsUploaded;

        // 3. Application status counts
        List<Map<String, Object>> statusCounts = dashboardMapper.getApplicationStatusCounts(candidateId);
        long totalApplications = 0L, pendingApplications = 0L, reviewedApplications = 0L;
        long shortlistedApplications = 0L, rejectedApplications = 0L, withdrawnApplications = 0L;

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

        // 4. Job stats
        long totalPublicJobs = dashboardMapper.getTotalPublicJobsCount("OPEN");
        long availableJobsCount = dashboardMapper.getAvailableJobsCount(candidateId, "OPEN");
        long appliedJobsCount = totalPublicJobs - availableJobsCount;

        // 5. Build Stats DTO
        CandidateDashboardStats stats = CandidateDashboardStats.builder()
                .isProfileComplete(isProfileComplete)
                .onboardingStage(onboardingStage)
                .documentsUploaded((int) documentsUploaded)
                .documentsApproved((int) documentsApproved)
                .documentsPending((int) documentsPending)
                .documentsRejected((int) documentsRejected)
                .allDocumentsApproved(allDocumentsApproved)
                .totalApplications(totalApplications)
                .pendingApplications(pendingApplications)
                .reviewedApplications(reviewedApplications)
                .shortlistedApplications(shortlistedApplications)
                .rejectedApplications(rejectedApplications)
                .withdrawnApplications(withdrawnApplications)
                .totalPublicJobs(totalPublicJobs)
                .appliedJobsCount(appliedJobsCount)
                .availableJobsCount(availableJobsCount)
                .build();

        // 6. Recent applications
        List<Map<String, Object>> recentAppsData = dashboardMapper.getRecentApplications(candidateId, recentLimit);
        List<CandidateRecentApplication> recentApplications = recentAppsData.stream()
                .map(app -> CandidateRecentApplication.builder()
                        .id(((Number) app.get("id")).longValue())
                        .jobTitle(app.get("job_title") != null ? app.get("job_title").toString() : "")
                        .country(app.get("country") != null ? app.get("country").toString() : null)
                        .city(app.get("city") != null ? app.get("city").toString() : null)
                        .salaryAmount(app.get("salary_amount") != null ? ((Number) app.get("salary_amount")).doubleValue() : null)
                        .salaryCurrency(app.get("salary_currency") != null ? app.get("salary_currency").toString() : null)
                        .status(app.get("status").toString())
                        .appliedAt(app.get("applied_at") != null ? app.get("applied_at").toString() : "")
                        .build())
                .collect(Collectors.toList());

        // 7. Recommended jobs
        List<Map<String, Object>> recommendedJobsData = dashboardMapper.getRecommendedJobs(
                candidateId, "OPEN", recommendedLimit);
        List<CandidateRecentJob> recommendedJobs = recommendedJobsData.stream()
                .map(job -> CandidateRecentJob.builder()
                        .id(((Number) job.get("id")).longValue())
                        .title(job.get("title").toString())
                        .country(job.get("country") != null ? job.get("country").toString() : null)
                        .city(job.get("city") != null ? job.get("city").toString() : null)
                        .salaryAmount(job.get("salary_amount") != null ? ((Number) job.get("salary_amount")).doubleValue() : null)
                        .salaryCurrency(job.get("salary_currency") != null ? job.get("salary_currency").toString() : null)
                        .totalSlots(job.get("total_slots") != null ? ((Number) job.get("total_slots")).intValue() : null)
                        .remainingSlots(job.get("remaining_slots") != null ? ((Number) job.get("remaining_slots")).intValue() : null)
                        .deadline(job.get("deadline") != null ? job.get("deadline").toString() : null)
                        .build())
                .collect(Collectors.toList());

        // 8. Document summary
        List<Map<String, Object>> documentsData = dashboardMapper.getDocumentSummary(candidateId);
        List<CandidateDocumentSummary> documentSummary = documentsData.stream()
                .map(doc -> CandidateDocumentSummary.builder()
                        .documentType(doc.get("document_type").toString())
                        .documentName(doc.get("document_name") != null ? doc.get("document_name").toString() : "")
                        .status(doc.get("status").toString())
                        .uploadedAt(doc.get("uploaded_at") != null ? doc.get("uploaded_at").toString() : "")
                        .build())
                .collect(Collectors.toList());

        // 9. Status Distribution
        ApplicationStatusDistribution statusDistribution = ApplicationStatusDistribution.builder()
                .pending(pendingApplications)
                .reviewed(reviewedApplications)
                .shortlisted(shortlistedApplications)
                .rejected(rejectedApplications)
                .withdrawn(withdrawnApplications)
                .build();

        // 10. Weekly activity
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(weeklyDays).withHour(0).withMinute(0).withSecond(0);
        List<Map<String, Object>> weeklyData = dashboardMapper.getWeeklyActivity(candidateId, sevenDaysAgo);

        Map<String, Long> weeklyMap = new HashMap<>();
        for (Map<String, Object> row : weeklyData) {
            String date = row.get("date").toString();
            Long count = ((Number) row.get("count")).longValue();
            weeklyMap.put(date, count);
        }

        List<String> days = new ArrayList<>();
        List<Long> applicationsSubmitted = new ArrayList<>();
        for (int i = weeklyDays - 1; i >= 0; i--) {
            LocalDateTime dayStart = LocalDateTime.now().minusDays(i);
            String dayKey = dayStart.toLocalDate().toString();
            days.add(dayStart.format(getDayFormatter()));
            applicationsSubmitted.add(weeklyMap.getOrDefault(dayKey, 0L));
        }

        WeeklyApplicationActivity weeklyActivity = WeeklyApplicationActivity.builder()
                .days(days)
                .applicationsSubmitted(applicationsSubmitted)
                .build();

        long endTime = System.currentTimeMillis();
        log.info("Candidate Dashboard loaded in {} ms for user: {}", (endTime - startTime), userId);

        return CandidateDashboardResponse.builder()
                .stats(stats)
                .recentApplications(recentApplications)
                .recommendedJobs(recommendedJobs)
                .documentSummary(documentSummary)
                .statusDistribution(statusDistribution)
                .weeklyActivity(weeklyActivity)
                .build();
    }
}