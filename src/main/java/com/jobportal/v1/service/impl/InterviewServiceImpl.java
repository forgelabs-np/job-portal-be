package com.jobportal.v1.service.impl;

import com.jobportal.v1.dto.interview.request.InterviewRequest;
import com.jobportal.v1.dto.interview.request.InterviewResultRequest;
import com.jobportal.v1.dto.interview.request.InterviewUpdateRequest;
import com.jobportal.v1.dto.interview.response.InterviewResponse;
import com.jobportal.v1.entity.*;
import com.jobportal.v1.enums.*;
import com.jobportal.v1.exception.BadRequestException;
import com.jobportal.v1.exception.ResourceNotFoundException;
import com.jobportal.v1.repository.*;
import com.jobportal.v1.service.EmailService;
import com.jobportal.v1.service.InterviewService;
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
            interview.setStatus(InterviewStatus.SCHEDULED);
            interview.setResult(InterviewResult.PENDING);
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
    public InterviewResponse updateInterviewStatus(Long interviewId, InterviewStatus status, Long adminId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found with id: " + interviewId));

        // Validate status transition
        InterviewStatus currentStatus = interview.getStatus();

        if (currentStatus == InterviewStatus.CANCELLED) {
            throw new BadRequestException("Cannot update status of a cancelled interview");
        }

        if (currentStatus == InterviewStatus.COMPLETED) {
            throw new BadRequestException("Interview is already completed");
        }

        if (status == InterviewStatus.COMPLETED && interview.getResult() != InterviewResult.PENDING) {
            // If marking as COMPLETED but result is already set, that's fine
            log.info("Marking interview as COMPLETED with existing result: {}", interview.getResult());
        }

        interview.setStatus(status);

        // If marking as NO_SHOW, automatically set result to FAIL
        if (status == InterviewStatus.NO_SHOW) {
            interview.setResult(InterviewResult.FAIL);
            interview.setResultNotes("Candidate did not show up for interview");
            interview.setResultUpdatedBy(adminId);
            interview.setResultUpdatedAt(LocalDateTime.now());
            log.info("Interview marked as NO_SHOW, result automatically set to FAIL for interview: {}", interviewId);
        }

        Interview saved = interviewRepository.save(interview);
        log.info("Interview status updated from {} to {} by admin: {}", currentStatus, status, adminId);

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public InterviewResponse setInterviewResult(Long interviewId, InterviewResultRequest request, Long adminId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found"));

        if (interview.getStatus() != InterviewStatus.COMPLETED && interview.getStatus() != InterviewStatus.NO_SHOW) {
            throw new BadRequestException("Interview result can only be set after interview is COMPLETED or marked as NO_SHOW");
        }

        interview.setResult(request.getResult());
        interview.setResultNotes(request.getResultNotes());
        interview.setResultUpdatedBy(adminId);
        interview.setResultUpdatedAt(LocalDateTime.now());

        Interview saved = interviewRepository.save(interview);
        log.info("Interview result set: {} for interview: {} by admin: {}", request.getResult(), interviewId, adminId);

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public InterviewResponse cancelInterview(Long interviewId, Long adminId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found"));

        if (interview.getStatus() == InterviewStatus.COMPLETED) {
            throw new BadRequestException("Cannot cancel a completed interview");
        }

        interview.setStatus(InterviewStatus.CANCELLED);
        Interview saved = interviewRepository.save(interview);

        emailService.sendInterviewCancelledEmail(saved);
        log.info("Interview cancelled: {} by admin: {}", interviewId, adminId);

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteInterview(Long interviewId, Long adminId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found"));

        if (interview.getStatus() == InterviewStatus.SCHEDULED || interview.getStatus() == InterviewStatus.RESCHEDULED) {
            emailService.sendInterviewCancelledEmail(interview);
        }

        interviewRepository.delete(interview);
        log.info("Interview deleted: {} by admin: {}", interviewId, adminId);
    }

    @Override
    public Page<InterviewResponse> getAllInterviews(Long jobDemandId, String status, String result, Long agencyId, Pageable pageable) {
        InterviewStatus interviewStatus = null;
        InterviewResult interviewResult = null;

        if (status != null && !status.isEmpty()) {
            try {
                interviewStatus = InterviewStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid status. Allowed: SCHEDULED, RESCHEDULED, COMPLETED, CANCELLED, NO_SHOW");
            }
        }

        if (result != null && !result.isEmpty()) {
            try {
                interviewResult = InterviewResult.valueOf(result.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid result. Allowed: PENDING, PASS, FAIL, RE_INTERVIEW");
            }
        }

        return interviewRepository.findAllWithFilters(jobDemandId, interviewStatus, interviewResult, agencyId, pageable)
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

        if (interview.getCandidate().getAgency() == null || !interview.getCandidate().getAgency().getId().equals(agencyId)) {
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
                .status(entity.getStatus())
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