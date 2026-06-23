package com.jobportal.v1.service.impl;

import com.jobportal.v1.dto.interview.request.InterviewRequest;
import com.jobportal.v1.dto.interview.request.InterviewResultRequest;
import com.jobportal.v1.dto.interview.response.InterviewResponse;
import com.jobportal.v1.entity.*;
import com.jobportal.v1.enums.*;
import com.jobportal.v1.exception.BadRequestException;
import com.jobportal.v1.exception.ResourceNotFoundException;
import com.jobportal.v1.repository.*;
import com.jobportal.v1.service.EmailService;
import com.jobportal.v1.service.InterviewService;
import com.jobportal.v1.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewServiceImpl implements InterviewService {

    private final InterviewRepository interviewRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final CandidateRepository candidateRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public InterviewResponse createOrUpdateInterview(InterviewRequest request, Long adminId) {
        Interview interview;
        boolean isUpdate = false;

        if (request.getId() != null && request.getId() > 0) {
            interview = interviewRepository.findById(request.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Interview not found with id: " + request.getId()));
            isUpdate = true;
            log.info("Updating interview: {} by admin: {}", request.getId(), adminId);
        } else {
            // Validate application exists and is SHORTLISTED
            JobApplication application = jobApplicationRepository.findById(request.getJobApplicationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Job application not found"));

            if (application.getStatus() != ApplicationStatus.SHORTLISTED) {
                throw new BadRequestException("Interview can only be scheduled for SHORTLISTED applications");
            }

            // Check if interview already exists for this application
            if (interviewRepository.existsByJobApplicationId(request.getJobApplicationId())) {
                throw new BadRequestException("An interview already exists for this application. Use update instead.");
            }

            interview = new Interview();
            interview.setJobApplication(application);
            interview.setCandidate(application.getCandidate());
            interview.setScheduledBy(adminId);
            // ✅ Combined: Set result to SCHEDULED initially (status is removed)
            interview.setResult(InterviewResult.SCHEDULED);
            interview.setReminderSent(false);

            log.info("Creating new interview for application: {}", request.getJobApplicationId());
        }

        // Validate interview type requirements
        if (request.getInterviewType() == InterviewType.ONLINE) {
            if (request.getInterviewLink() == null || request.getInterviewLink().isBlank()) {
                throw new BadRequestException("Interview link is required for online interviews");
            }
        } else if (request.getInterviewType() == InterviewType.IN_PERSON) {
            if (request.getVenue() == null || request.getVenue().isBlank()) {
                throw new BadRequestException("Venue is required for in-person interviews");
            }
        }

        // Validate scheduled date is in future
        if (request.getScheduledAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Scheduled date must be in the future");
        }

        interview.setScheduledAt(request.getScheduledAt());
        interview.setTimezone(request.getTimezone() != null ? request.getTimezone() : "Asia/Kathmandu");
        interview.setInterviewLink(request.getInterviewLink());
        interview.setInterviewType(request.getInterviewType());
        interview.setVenue(request.getVenue());
        interview.setAdminNotes(request.getAdminNotes());

        Interview saved = interviewRepository.save(interview);

        // Send email notification
        if (isUpdate) {
            emailService.sendInterviewRescheduledEmail(saved);
        } else {
            emailService.sendInterviewScheduledEmail(saved);
            sendInterviewScheduledNotification(saved);
        }

        log.info("Interview {}: {} for candidate: {} by admin: {}",
                isUpdate ? "updated" : "created", saved.getId(), interview.getCandidate().getFullName(), adminId);

        return mapToResponse(saved);
    }

    @Override
    public InterviewResponse getInterviewById(Long interviewId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found"));
        return mapToResponse(interview);
    }

    @Override
    public InterviewResponse getInterviewByApplicationId(Long jobApplicationId) {
        Interview interview = interviewRepository.findByJobApplicationId(jobApplicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found for application: " + jobApplicationId));
        return mapToResponse(interview);
    }

    @Override
    @Transactional
    public InterviewResponse updateInterviewResult(Long interviewId, InterviewResultRequest request, Long adminId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found with id: " + interviewId));

        InterviewResult currentResult = interview.getResult();
        InterviewResult selectedResult = request.getResult();

        // ✅ Validate current state
        if (currentResult == InterviewResult.PASS ||
                currentResult == InterviewResult.FAIL) {
            throw new BadRequestException("Interview already has a final result: " + currentResult + ". Cannot change.");
        }

        // ✅ Mark as PENDING (interview completed, awaiting result)
        if (selectedResult == InterviewResult.PENDING) {
            if (currentResult != InterviewResult.SCHEDULED) {
                throw new BadRequestException("Interview must be SCHEDULED to mark as PENDING. Current: " + currentResult);
            }
            interview.setResult(InterviewResult.PENDING);
            log.info("Interview marked as PENDING (completed, awaiting result) by admin: {}", adminId);

            Interview saved = interviewRepository.save(interview);
            return mapToResponse(saved);
        }

        // ✅ Set final result: PASS, FAIL, RE_INTERVIEW
        if (selectedResult == InterviewResult.PASS ||
                selectedResult == InterviewResult.FAIL ||
                selectedResult == InterviewResult.RE_INTERVIEW) {

            // Only allow if currently PENDING or RE_INTERVIEW
            if (currentResult != InterviewResult.PENDING && currentResult != InterviewResult.RE_INTERVIEW) {
                throw new BadRequestException("Interview must be PENDING to set final result. Current: " + currentResult);
            }

            interview.setResult(selectedResult);
            interview.setResultNotes(request.getResultNotes());
            interview.setResultUpdatedBy(adminId);
            interview.setResultUpdatedAt(LocalDateTime.now());

            // ✅ If RE_INTERVIEW, reset to SCHEDULED for next round
            if (selectedResult == InterviewResult.RE_INTERVIEW) {
                interview.setScheduledAt(null);  // Admin must set new date
                interview.setReminderSent(false);
                log.info("RE_INTERVIEW selected. Interview reset to SCHEDULED for next round.");
            }

            Interview saved = interviewRepository.save(interview);

            sendInterviewResultNotification(saved, selectedResult);

            log.info("Interview result set: {} for interview: {} by admin: {}", selectedResult, interviewId, adminId);
            return mapToResponse(saved);
        }

        throw new BadRequestException("Invalid result. Allowed: PENDING, PASS, FAIL, RE_INTERVIEW");
    }

    @Override
    @Transactional
    public InterviewResponse cancelInterview(Long interviewId, Long adminId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found"));

        if (interview.getResult() == InterviewResult.PASS ||
                interview.getResult() == InterviewResult.FAIL) {
            throw new BadRequestException("Cannot cancel a completed interview with final result");
        }

        interview.setResult(InterviewResult.RE_INTERVIEW);
        interview.setScheduledAt(null);
        Interview saved = interviewRepository.save(interview);

        emailService.sendInterviewCancelledEmail(saved);
        sendInterviewCancelledNotification(saved);

        log.info("Interview cancelled: {} by admin: {}", interviewId, adminId);

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteInterview(Long interviewId, Long adminId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found"));

        if (interview.getResult() == InterviewResult.SCHEDULED ||
                interview.getResult() == InterviewResult.RE_INTERVIEW) {
            emailService.sendInterviewCancelledEmail(interview);
        }

        interviewRepository.delete(interview);
        log.info("Interview deleted: {} by admin: {}", interviewId, adminId);
    }

    @Override
    public Page<InterviewResponse> getAllInterviews(Long jobDemandId, String result, Long agencyId, Pageable pageable) {
        InterviewResult interviewResult = null;

        if (result != null && !result.isEmpty()) {
            try {
                interviewResult = InterviewResult.valueOf(result.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid result. Allowed: SCHEDULED, PENDING, PASS, FAIL, RE_INTERVIEW");
            }
        }

        return interviewRepository.findAllWithFilters(jobDemandId, interviewResult, agencyId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public Page<InterviewResponse> getAgencyInterviews(Long agencyId, Pageable pageable) {
        return interviewRepository.findByAgencyId(agencyId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public InterviewResponse getAgencyInterviewById(Long interviewId, Long agencyId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found"));

        if (interview.getCandidate().getAgency() == null ||
                !interview.getCandidate().getAgency().getId().equals(agencyId)) {
            throw new BadRequestException("You are not authorized to view this interview");
        }

        return mapToResponse(interview);
    }

    @Override
    public Page<InterviewResponse> getCandidateInterviews(Long userId, Pageable pageable) {
        Candidate candidate = candidateRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate profile not found"));

        return interviewRepository.findByCandidateId(candidate.getId(), pageable)
                .map(this::mapToResponse);
    }

    @Override
    public InterviewResponse getCandidateInterviewById(Long interviewId, Long userId) {
        Candidate candidate = candidateRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate profile not found"));

        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found"));

        if (!interview.getCandidate().getId().equals(candidate.getId())) {
            throw new BadRequestException("You are not authorized to view this interview");
        }

        return mapToResponse(interview);
    }

    // ✅ Notification helper methods
    private void sendInterviewScheduledNotification(Interview interview) {
        Candidate candidate = interview.getCandidate();
        JobApplication application = interview.getJobApplication();
        String jobTitle = application.getJobDemand().getTitle();

        if (candidate.getUser() != null) {
            notificationService.sendNotification(
                    candidate.getUser().getId(),
                    NotificationType.INTERVIEW_SCHEDULED,
                    "Interview Scheduled",
                    "Interview scheduled for " + jobTitle + " on " + interview.getScheduledAt(),
                    "/candidate/interviews/" + interview.getId()
            );
        }

        if (candidate.getAgency() != null) {
            notificationService.sendNotification(
                    candidate.getAgency().getId(),
                    NotificationType.INTERVIEW_SCHEDULED,
                    "Interview Scheduled for Candidate",
                    "Interview scheduled for " + candidate.getFullName() + " for " + jobTitle,
                    "/agency/interviews/" + interview.getId()
            );
        }
    }

    private void sendInterviewResultNotification(Interview interview, InterviewResult result) {
        Candidate candidate = interview.getCandidate();
        JobApplication application = interview.getJobApplication();
        String jobTitle = application.getJobDemand().getTitle();

        if (candidate.getUser() != null) {
            notificationService.sendNotification(
                    candidate.getUser().getId(),
                    NotificationType.INTERVIEW_RESULT,
                    "Interview Result Published",
                    "Your interview result for " + jobTitle + ": " + result,
                    "/candidate/interviews/" + interview.getId()
            );
        }
    }

    private void sendInterviewCancelledNotification(Interview interview) {
        Candidate candidate = interview.getCandidate();
        JobApplication application = interview.getJobApplication();
        String jobTitle = application.getJobDemand().getTitle();

        if (candidate.getUser() != null) {
            notificationService.sendNotification(
                    candidate.getUser().getId(),
                    NotificationType.INTERVIEW_CANCELLED,
                    "Interview Cancelled",
                    "Your interview for " + jobTitle + " has been cancelled.",
                    "/candidate/applications"
            );
        }
    }

    private InterviewResponse mapToResponse(Interview entity) {
        Candidate candidate = entity.getCandidate();
        JobApplication application = entity.getJobApplication();
        JobDemand job = application.getJobDemand();

        return InterviewResponse.builder()
                .id(entity.getId())
                .jobApplicationId(application.getId())
                .jobTitle(job.getTitle())
                .candidateId(candidate.getId())
                .candidateName(candidate.getFullName())
                .candidateType(candidate.getCandidateType().name())
                .agencyId(candidate.getAgency() != null ? candidate.getAgency().getId() : null)
                .agencyName(candidate.getAgency() != null ? candidate.getAgency().getFullName() : null)
                .scheduledAt(entity.getScheduledAt())
                .timezone(entity.getTimezone())
                .interviewLink(entity.getInterviewLink())
                .interviewType(entity.getInterviewType())
                .venue(entity.getVenue())
                .adminNotes(entity.getAdminNotes())
                .result(entity.getResult())
                .resultNotes(entity.getResultNotes())
                .resultUpdatedBy(entity.getResultUpdatedBy())
                .resultUpdatedAt(entity.getResultUpdatedAt())
                .scheduledBy(entity.getScheduledBy())
                .reminderSent(entity.getReminderSent())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}