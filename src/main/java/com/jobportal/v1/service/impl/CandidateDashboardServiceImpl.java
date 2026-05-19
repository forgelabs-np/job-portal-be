package com.jobportal.v1.service.impl;

import com.jobportal.v1.dto.dashboard.response.candidate.*;
import com.jobportal.v1.entity.*;
import com.jobportal.v1.enums.ApplicationStatus;
import com.jobportal.v1.enums.ApprovalStatus;
import com.jobportal.v1.enums.JobStatus;
import com.jobportal.v1.exception.ResourceNotFoundException;
import com.jobportal.v1.repository.*;
import com.jobportal.v1.service.CandidateDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CandidateDashboardServiceImpl implements CandidateDashboardService {

    private final CandidateRepository candidateRepository;
    private final CandidateDocumentRepository documentRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final JobDemandRepository jobDemandRepository;

    @Override
    public CandidateDashboardResponse getCandidateDashboard(Long userId) {
        // Get candidate profile
        Candidate candidate = candidateRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate profile not found"));

        // Document Stats
        List<CandidateDocument> documents = documentRepository.findByCandidateId(candidate.getId());
        long documentsUploaded = documents.size();
        long documentsApproved = documents.stream()
                .filter(doc -> doc.getStatus() == ApprovalStatus.APPROVED).count();
        long documentsPending = documents.stream()
                .filter(doc -> doc.getStatus() == ApprovalStatus.PENDING).count();
        long documentsRejected = documents.stream()
                .filter(doc -> doc.getStatus() == ApprovalStatus.REJECTED).count();
        boolean allDocumentsApproved = documentsUploaded > 0 && documents.stream()
                .allMatch(doc -> doc.getStatus() == ApprovalStatus.APPROVED);

        // ✅ Fix: Get all applications without pageable - use Pageable.unpaged() or large page size
        Pageable unpaged = PageRequest.of(0, Integer.MAX_VALUE);
        List<JobApplication> applications = jobApplicationRepository.findByCandidateId(candidate.getId(), unpaged).getContent();

        long totalApplications = applications.size();
        long pendingApplications = applications.stream()
                .filter(a -> a.getStatus() == ApplicationStatus.PENDING).count();
        long reviewedApplications = applications.stream()
                .filter(a -> a.getStatus() == ApplicationStatus.REVIEWED).count();
        long shortlistedApplications = applications.stream()
                .filter(a -> a.getStatus() == ApplicationStatus.SHORTLISTED).count();
        long rejectedApplications = applications.stream()
                .filter(a -> a.getStatus() == ApplicationStatus.REJECTED).count();
        long withdrawnApplications = applications.stream()
                .filter(a -> a.getStatus() == ApplicationStatus.WITHDRAWN).count();

        // Job Stats
        long totalPublicJobs = jobDemandRepository.countByIsPublicTrueAndStatusAndIsActiveTrue(JobStatus.OPEN);
        long appliedJobsCount = applications.stream()
                .map(JobApplication::getJobDemand)
                .map(JobDemand::getId)
                .distinct()
                .count();
        long availableJobsCount = totalPublicJobs - appliedJobsCount;

        // Build Stats
        CandidateDashboardStats stats = CandidateDashboardStats.builder()
                .isProfileComplete(candidate.isProfileComplete())
                .onboardingStage(candidate.getOnboardingStage() != null ?
                        candidate.getOnboardingStage().name() : "PROFILE")
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

        // Recent Applications (last 5)
        List<CandidateRecentApplication> recentApplications = applications.stream()
                .sorted((a, b) -> b.getAppliedAt().compareTo(a.getAppliedAt()))
                .limit(5)
                .map(app -> CandidateRecentApplication.builder()
                        .id(app.getId())
                        .jobTitle(app.getJobDemand().getTitle())
                        .country(app.getJobDemand().getCountry() != null ?
                                app.getJobDemand().getCountry().getName() : null)
                        .city(app.getJobDemand().getCity())
                        .salaryAmount(app.getJobDemand().getSalaryAmount())
                        .salaryCurrency(app.getJobDemand().getSalaryCurrency())
                        .status(app.getStatus().name())
                        .appliedAt(app.getAppliedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                        .build())
                .collect(Collectors.toList());

        // Recommended Jobs (public jobs candidate hasn't applied to, limit 5)
        Pageable pageable = PageRequest.of(0, 10);
        List<JobDemand> allPublicJobs = jobDemandRepository.findByIsPublicTrueAndStatusAndIsActiveTrue(
                JobStatus.OPEN, pageable).getContent();

        List<Long> appliedJobIds = applications.stream()
                .map(a -> a.getJobDemand().getId())
                .collect(Collectors.toList());

        List<CandidateRecentJob> recommendedJobs = allPublicJobs.stream()
                .filter(job -> !appliedJobIds.contains(job.getId()))
                .limit(5)
                .map(job -> CandidateRecentJob.builder()
                        .id(job.getId())
                        .title(job.getTitle())
                        .country(job.getCountry() != null ? job.getCountry().getName() : null)
                        .city(job.getCity())
                        .salaryAmount(job.getSalaryAmount())
                        .salaryCurrency(job.getSalaryCurrency())
                        .totalSlots(job.getTotalSlots())
                        .remainingSlots(job.getRemainingSlots())
                        .deadline(job.getDeadline() != null ?
                                job.getDeadline().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) : null)
                        .build())
                .collect(Collectors.toList());

        // Document Summary
        List<CandidateDocumentSummary> documentSummary = documents.stream()
                .map(doc -> CandidateDocumentSummary.builder()
                        .documentType(doc.getDocumentType().name())
                        .documentName(doc.getDocumentName())
                        .status(doc.getStatus() != null ? doc.getStatus().name() : "PENDING")
                        .uploadedAt(doc.getUploadedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                        .build())
                .collect(Collectors.toList());

        // Status Distribution
        ApplicationStatusDistribution statusDistribution = ApplicationStatusDistribution.builder()
                .pending(pendingApplications)
                .reviewed(reviewedApplications)
                .shortlisted(shortlistedApplications)
                .rejected(rejectedApplications)
                .withdrawn(withdrawnApplications)
                .build();

        // Weekly Activity (last 7 days)
        List<String> days = new ArrayList<>();
        List<Long> applicationsSubmitted = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            LocalDateTime dayStart = LocalDateTime.now().minusDays(i).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime dayEnd = dayStart.withHour(23).withMinute(59).withSecond(59);
            days.add(dayStart.format(DateTimeFormatter.ofPattern("EEE")));

            Long submittedCount = applications.stream()
                    .filter(a -> a.getAppliedAt().isAfter(dayStart) && a.getAppliedAt().isBefore(dayEnd))
                    .count();
            applicationsSubmitted.add(submittedCount);
        }

        WeeklyApplicationActivity weeklyActivity = WeeklyApplicationActivity.builder()
                .days(days)
                .applicationsSubmitted(applicationsSubmitted)
                .build();

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