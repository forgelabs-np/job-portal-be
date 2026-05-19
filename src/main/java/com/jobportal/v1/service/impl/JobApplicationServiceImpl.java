package com.jobportal.v1.service.impl;

import com.jobportal.v1.dto.admin.response.AdminApplicationResponse;
import com.jobportal.v1.dto.admin.response.AdminSelfApplicationResponse;
import com.jobportal.v1.dto.agency.response.AgencyJobApplicationResponse;
import com.jobportal.v1.dto.jobApplicationReport.request.ApplicationStatusUpdateRequest;
import com.jobportal.v1.dto.jobApplicationReport.request.JobApplicationRequest;
import com.jobportal.v1.dto.jobApplicationReport.response.JobApplicationResponse;
import com.jobportal.v1.entity.*;
import com.jobportal.v1.enums.ApplicationStatus;
import com.jobportal.v1.enums.ApprovalStatus;
import com.jobportal.v1.exception.BadRequestException;
import com.jobportal.v1.exception.ResourceNotFoundException;
import com.jobportal.v1.repository.*;
import com.jobportal.v1.service.JobApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobApplicationServiceImpl implements JobApplicationService {

    private final JobApplicationRepository jobApplicationRepository;
    private final JobDemandRepository jobDemandRepository;
    private final CandidateRepository candidateRepository;
    private final JobAgencyAssignmentRepository assignmentRepository;
    private final CandidateDocumentRepository documentRepository;

    @Override
    @Transactional
    public JobApplicationResponse applyForJob(JobApplicationRequest request, Long agencyId) {
        // Validate job exists and is open
        JobDemand job = jobDemandRepository.findById(request.getJobDemandId())
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        if (!job.isOpen()) {
            throw new BadRequestException("Job is not open for applications");
        }

        // Validate agency is assigned to this job
        boolean isAssigned = assignmentRepository.existsByJobDemandIdAndAgencyIdAndIsEnabledTrue(
                request.getJobDemandId(), agencyId);
        if (!isAssigned) {
            throw new BadRequestException("You are not authorized to apply for this job");
        }

        // Validate candidate exists and belongs to agency
        Candidate candidate = candidateRepository.findByIdAndAgencyId(request.getCandidateId(), agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));

        if (!candidate.getIsEnabled()) {
            throw new BadRequestException("Candidate is disabled. Please enable candidate first.");
        }

        // Check for existing application
        Optional<JobApplication> existingApplication = jobApplicationRepository
                .findByJobDemandIdAndCandidateId(request.getJobDemandId(), request.getCandidateId());

        if (existingApplication.isPresent()) {
            JobApplication existing = existingApplication.get();

            // If application is WITHDRAWN, allow re-application by reactivating
            if (existing.getStatus() == ApplicationStatus.WITHDRAWN) {
                existing.setStatus(ApplicationStatus.PENDING);
                existing.setNotes(request.getNotes());
                existing.setAppliedAt(LocalDateTime.now());
                existing.setRejectionReason(null);
                existing.setReviewedBy(null);
                existing.setReviewedAt(null);

                JobApplication updated = jobApplicationRepository.save(existing);
                log.info("Re-applied for job: Job {} by Candidate {} from Agency {} (previous withdrawn application reactivated)",
                        job.getTitle(), candidate.getFirstName() + candidate.getLastName(), agencyId);
                return mapToAgencyResponse(updated);
            }

            // If application is already PENDING, REVIEWED, or SHORTLISTED, block re-application
            if (existing.getStatus() == ApplicationStatus.PENDING ||
                    existing.getStatus() == ApplicationStatus.REVIEWED ||
                    existing.getStatus() == ApplicationStatus.SHORTLISTED) {
                throw new BadRequestException("This candidate has already applied for this job with status: " + existing.getStatus());
            }

            // If application is REJECTED, allow re-application
            if (existing.getStatus() == ApplicationStatus.REJECTED) {
                existing.setStatus(ApplicationStatus.PENDING);
                existing.setNotes(request.getNotes());
                existing.setAppliedAt(LocalDateTime.now());
                existing.setRejectionReason(null);
                existing.setReviewedBy(null);
                existing.setReviewedAt(null);

                JobApplication updated = jobApplicationRepository.save(existing);
                log.info("Re-applied for job: Job {} by Candidate {} from Agency {} (previous rejected application reactivated)",
                        job.getTitle(), candidate.getFirstName() + candidate.getLastName(), agencyId);
                return mapToAgencyResponse(updated);
            }
        }

        // Create new application
        JobApplication application = new JobApplication();
        application.setJobDemand(job);
        application.setCandidate(candidate);
        application.setAgency(candidate.getAgency());
        application.setNotes(request.getNotes());
        application.setStatus(ApplicationStatus.PENDING);

        JobApplication saved = jobApplicationRepository.save(application);

        log.info("Application submitted: Job {} by Candidate {} from Agency {}",
                job.getTitle(), candidate.getFirstName() + candidate.getLastName(), agencyId);

        return mapToAgencyResponse(saved);
    }

    @Override
    public Page<JobApplicationResponse> getMyApplications(Long agencyId, Long jobDemandId, String status, Pageable pageable) {
        Page<JobApplication> applications;

        if (jobDemandId != null && status != null) {
            ApplicationStatus appStatus = ApplicationStatus.valueOf(status.toUpperCase());
            applications = jobApplicationRepository.findByAgencyIdAndJobDemandIdAndStatus(
                    agencyId, jobDemandId, appStatus, pageable);
        } else if (jobDemandId != null) {
            applications = jobApplicationRepository.findByAgencyIdAndJobDemandId(agencyId, jobDemandId, pageable);
        } else if (status != null) {
            ApplicationStatus appStatus = ApplicationStatus.valueOf(status.toUpperCase());
            applications = jobApplicationRepository.findByAgencyIdAndStatus(agencyId, appStatus, pageable);
        } else {
            applications = jobApplicationRepository.findByAgencyId(agencyId, pageable);
        }

        return applications.map(this::mapToAgencyResponse);
    }

    public AgencyJobApplicationResponse getMyApplicationById(Long applicationId, Long agencyId) {
        JobApplication application = jobApplicationRepository.findByIdAndAgencyId(applicationId, agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        return mapToAgencyJobApplicationResponse(application);
    }

    @Override
    @Transactional
    public JobApplicationResponse withdrawApplication(Long applicationId, Long agencyId) {
        JobApplication application = jobApplicationRepository.findByIdAndAgencyId(applicationId, agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new BadRequestException("Cannot withdraw application that is already " + application.getStatus());
        }

        application.setStatus(ApplicationStatus.WITHDRAWN);
        JobApplication saved = jobApplicationRepository.save(application);

        log.info("Application withdrawn: {} by Agency {}", applicationId, agencyId);

        return mapToAgencyResponse(saved);
    }

    @Override
    public Page<AdminApplicationResponse> getAllApplications(Long jobDemandId, Long agencyId, String status, Pageable pageable) {
        Page<JobApplication> applications;

        if (jobDemandId != null && agencyId != null && status != null) {
            ApplicationStatus appStatus = ApplicationStatus.valueOf(status.toUpperCase());
            applications = jobApplicationRepository.findByJobDemandIdAndAgencyIdAndStatus(
                    jobDemandId, agencyId, appStatus, pageable);
        } else if (jobDemandId != null && agencyId != null) {
            applications = jobApplicationRepository.findByJobDemandIdAndAgencyId(jobDemandId, agencyId, pageable);
        } else if (jobDemandId != null && status != null) {
            ApplicationStatus appStatus = ApplicationStatus.valueOf(status.toUpperCase());
            applications = jobApplicationRepository.findByJobDemandIdAndStatus(jobDemandId, appStatus, pageable);
        } else if (agencyId != null && status != null) {
            ApplicationStatus appStatus = ApplicationStatus.valueOf(status.toUpperCase());
            applications = jobApplicationRepository.findByAgencyIdAndStatus(agencyId, appStatus, pageable);
        } else if (jobDemandId != null) {
            applications = jobApplicationRepository.findByJobDemandId(jobDemandId, pageable);
        } else if (agencyId != null) {
            applications = jobApplicationRepository.findByAgencyId(agencyId, pageable);
        } else if (status != null) {
            ApplicationStatus appStatus = ApplicationStatus.valueOf(status.toUpperCase());
            applications = jobApplicationRepository.findByStatus(appStatus, pageable);
        } else {
            applications = jobApplicationRepository.findAll(pageable);
        }

        return applications.map(this::mapToAdminResponse);
    }

    @Override
    @Transactional
    public AdminApplicationResponse updateApplicationStatus(Long applicationId, ApplicationStatusUpdateRequest request, Long adminId) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        if (request.getStatus() == ApplicationStatus.REJECTED &&
                (request.getRejectionReason() == null || request.getRejectionReason().trim().isEmpty())) {
            throw new BadRequestException("Rejection reason is required when rejecting an application");
        }

        application.setStatus(request.getStatus());
        application.setReviewedBy(adminId);
        application.setReviewedAt(LocalDateTime.now());
        application.setRejectionReason(request.getRejectionReason());

        // If shortlisted, increment filled slots in job demand
        if (request.getStatus() == ApplicationStatus.SHORTLISTED) {
            application.getJobDemand().incrementFilledSlots();
            jobDemandRepository.save(application.getJobDemand());
        }

        JobApplication saved = jobApplicationRepository.save(application);

        log.info("Application {} status updated to {} by admin {}",
                applicationId, request.getStatus(), adminId);

        return mapToAdminResponse(saved);
    }

    @Override
    public AgencyJobApplicationResponse getApplicationDetails(Long applicationId) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        return mapToAgencyJobApplicationResponse(application);
    }

    @Override
    public Page<JobApplicationResponse> getAllSelfApplications(Long jobDemandId, String status, Pageable pageable) {
        Page<JobApplication> applications;

        if (jobDemandId != null && status != null) {
            ApplicationStatus appStatus = ApplicationStatus.valueOf(status.toUpperCase());
            applications = jobApplicationRepository.findByJobDemandIdAndStatusAndAgencyIsNull(jobDemandId, appStatus, pageable);
        } else if (jobDemandId != null) {
            applications = jobApplicationRepository.findByJobDemandIdAndAgencyIsNull(jobDemandId, pageable);
        } else if (status != null) {
            ApplicationStatus appStatus = ApplicationStatus.valueOf(status.toUpperCase());
            applications = jobApplicationRepository.findByStatusAndAgencyIsNull(appStatus, pageable);
        } else {
            applications = jobApplicationRepository.findByAgencyIsNull(pageable);
        }
        return applications.map(this::mapToJobApplicationResponse);
    }
    @Override
    public AdminSelfApplicationResponse getSelfApplicationDetails(Long applicationId) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        // Verify it's a self-candidate application (agency is null)
        if (application.getAgency() != null) {
            throw new BadRequestException("This is not a self-candidate application. Use agency application endpoint.");
        }

        return mapToAdminSelfApplicationResponse(application);
    }

    @Override
    @Transactional
    public AdminSelfApplicationResponse updateSelfApplicationStatus(Long applicationId, ApplicationStatusUpdateRequest request, Long adminId) {
        JobApplication application = jobApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        // Verify it's a self-candidate application
        if (application.getAgency() != null) {
            throw new BadRequestException("This is not a self-candidate application. Use agency application endpoint.");
        }

        if (request.getStatus() == ApplicationStatus.REJECTED &&
                (request.getRejectionReason() == null || request.getRejectionReason().trim().isEmpty())) {
            throw new BadRequestException("Rejection reason is required when rejecting an application");
        }

        application.setStatus(request.getStatus());
        application.setReviewedBy(adminId);
        application.setReviewedAt(LocalDateTime.now());
        application.setRejectionReason(request.getRejectionReason());

        if (request.getStatus() == ApplicationStatus.SHORTLISTED) {
            application.getJobDemand().incrementFilledSlots();
            jobDemandRepository.save(application.getJobDemand());
        }

        JobApplication saved = jobApplicationRepository.save(application);

        log.info("Self-application {} status updated to {} by admin {}",
                applicationId, request.getStatus(), adminId);

        return mapToAdminSelfApplicationResponse(saved);
    }


    private AdminSelfApplicationResponse mapToAdminSelfApplicationResponse(JobApplication entity) {
        Candidate candidate = entity.getCandidate();

        Integer age = null;
        if (candidate.getDateOfBirth() != null) {
            age = Period.between(candidate.getDateOfBirth(), LocalDate.now()).getYears();
        }

        List<CandidateDocument> documents = documentRepository.findByCandidateId(candidate.getId());

        // Map documents to DocumentInfo
        List<AdminSelfApplicationResponse.DocumentInfo> documentInfos = documents.stream()
                .map(doc -> AdminSelfApplicationResponse.DocumentInfo.builder()
                        .id(doc.getId())
                        .documentType(doc.getDocumentType().name())
                        .documentName(doc.getDocumentName())
                        .documentPath(doc.getDocumentPath())
                        .status(doc.getStatus() != null ? doc.getStatus().name() : "PENDING")
                        .rejectionReason(doc.getRejectionReason())
                        .uploadedAt(doc.getUploadedAt() != null ?
                                doc.getUploadedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null)
                        .build())
                .collect(Collectors.toList());

        // Create document status map
        Map<String, String> documentStatuses = documents.stream()
                .collect(Collectors.toMap(
                        doc -> doc.getDocumentType().name(),
                        doc -> doc.getStatus() != null ? doc.getStatus().name() : "PENDING",
                        (existing, replacement) -> existing
                ));

        // Check if all documents are approved
        boolean allDocumentsApproved = !documents.isEmpty() && documents.stream()
                .allMatch(doc -> doc.getStatus() == ApprovalStatus.APPROVED);

        return AdminSelfApplicationResponse.builder()
                .id(entity.getId())
                .jobDemandId(entity.getJobDemand().getId())
                .jobTitle(entity.getJobDemand().getTitle())
                .jobCountry(entity.getJobDemand().getCountry() != null ?
                        entity.getJobDemand().getCountry().getName() : null)
                .jobCity(entity.getJobDemand().getCity())
                .salaryAmount(entity.getJobDemand().getSalaryAmount())
                .salaryCurrency(entity.getJobDemand().getSalaryCurrency())
                .candidateId(candidate.getId())
                .candidateName(candidate.getFullName())
                .candidateEmail(candidate.getUser() != null ? candidate.getUser().getEmail() : null)
                .candidatePhone(candidate.getUser() != null ? candidate.getUser().getPhoneNumber() : null)
                .candidateTrade(candidate.getTrade())
                .candidatePassportNumber(candidate.getPassportNumber())
                .candidateAge(age)
                .candidateMaritalStatus(candidate.getMaritalStatus() != null ?
                        candidate.getMaritalStatus().name() : null)
                .isProfileComplete(candidate.isProfileComplete())
                .isEnabled(candidate.getIsEnabled())
                .notes(entity.getNotes())
                .status(entity.getStatus().name())
                .appliedAt(entity.getAppliedAt() != null ?
                        entity.getAppliedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null)
                .rejectionReason(entity.getRejectionReason())
                .reviewedBy(entity.getReviewedBy())
                .reviewedAt(entity.getReviewedAt() != null ?
                        entity.getReviewedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .documents(documentInfos)
                .documentStatuses(documentStatuses)
                .allDocumentsApproved(allDocumentsApproved)
                .build();
    }

    private JobApplicationResponse mapToAgencyResponse(JobApplication entity) {
        return JobApplicationResponse.builder()
                .id(entity.getId())
                .jobDemandId(entity.getJobDemand().getId())
                .jobTitle(entity.getJobDemand().getTitle())
                .country(entity.getJobDemand().getCountry() != null ?
                        entity.getJobDemand().getCountry().getName() : null)
                .city(entity.getJobDemand().getCity())
                .salaryAmount(entity.getJobDemand().getSalaryAmount())
                .salaryCurrency(entity.getJobDemand().getSalaryCurrency())
                .candidateId(entity.getCandidate().getId())
                .candidateName(entity.getCandidate().getFirstName() + entity.getCandidate().getLastName())
                .candidateTrade(entity.getCandidate().getTrade())
                .notes(entity.getNotes())
                .status(entity.getStatus().name())
                .appliedAt(entity.getAppliedAt() != null ?
                        entity.getAppliedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null)
                .rejectionReason(entity.getRejectionReason())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private AdminApplicationResponse mapToAdminResponse(JobApplication entity) {
        return AdminApplicationResponse.builder()
                .id(entity.getId())
                .jobDemandId(entity.getJobDemand().getId())
                .jobTitle(entity.getJobDemand().getTitle())
                .jobCountry(entity.getJobDemand().getCountry() != null ?
                        entity.getJobDemand().getCountry().getName() : null)
                .jobCity(entity.getJobDemand().getCity())
                .agencyId(entity.getAgency() != null ? entity.getAgency().getId() : null)
                .agencyName(entity.getAgency() != null ? entity.getAgency().getFullName() : null)
                .agencyEmail(entity.getAgency() != null ? entity.getAgency().getEmail() : null)
                .candidateId(entity.getCandidate().getId())
                .candidateName(entity.getCandidate().getFirstName() + entity.getCandidate().getLastName())
                .candidateTrade(entity.getCandidate().getTrade())
                .candidatePassportNumber(entity.getCandidate().getPassportNumber())
                .notes(entity.getNotes())
                .status(entity.getStatus().name())
                .appliedAt(entity.getAppliedAt() != null ?
                        entity.getAppliedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null)
                .rejectionReason(entity.getRejectionReason())
                .reviewedBy(entity.getReviewedBy())
                .reviewedAt(entity.getReviewedAt() != null ?
                        entity.getReviewedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private AgencyJobApplicationResponse mapToAgencyJobApplicationResponse(JobApplication entity) {
        Candidate candidate = entity.getCandidate();

        // Calculate age
        Integer age = null;
        if (candidate.getDateOfBirth() != null) {
            age = Period.between(candidate.getDateOfBirth(), LocalDate.now()).getYears();
        }

        // Get all documents for this candidate
        List<CandidateDocument> documents = documentRepository.findByCandidateId(candidate.getId());

        // Map documents to DocumentInfo
        List<AgencyJobApplicationResponse.DocumentInfo> documentInfos = documents.stream()
                .map(doc -> AgencyJobApplicationResponse.DocumentInfo.builder()
                        .id(doc.getId())
                        .documentType(doc.getDocumentType().name())
                        .documentName(doc.getDocumentName())
                        .documentPath(doc.getDocumentPath())
                        .status(doc.getStatus() != null ? doc.getStatus().name() : "PENDING")
                        .rejectionReason(doc.getRejectionReason())
                        .uploadedAt(doc.getUploadedAt() != null ?
                                doc.getUploadedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null)
                        .build())
                .collect(Collectors.toList());

        // Create document status map
        Map<String, String> documentStatuses = documents.stream()
                .collect(Collectors.toMap(
                        doc -> doc.getDocumentType().name(),
                        doc -> doc.getStatus() != null ? doc.getStatus().name() : "PENDING",
                        (existing, replacement) -> existing
                ));

        // Check if all documents are approved
        boolean allDocumentsApproved = !documents.isEmpty() && documents.stream()
                .allMatch(doc -> doc.getStatus() == ApprovalStatus.APPROVED);

        return AgencyJobApplicationResponse.builder()
                .id(entity.getId())
                .jobDemandId(entity.getJobDemand().getId())
                .jobTitle(entity.getJobDemand().getTitle())
                .country(entity.getJobDemand().getCountry() != null ?
                        entity.getJobDemand().getCountry().getName() : null)
                .city(entity.getJobDemand().getCity())
                .salaryAmount(entity.getJobDemand().getSalaryAmount())
                .salaryCurrency(entity.getJobDemand().getSalaryCurrency())
                .candidateId(candidate.getId())
                .candidateName(candidate.getFullName())
                .candidateTrade(candidate.getTrade())
                .notes(entity.getNotes())
                .status(entity.getStatus().name())
                .appliedAt(entity.getAppliedAt() != null ?
                        entity.getAppliedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null)
                .rejectionReason(entity.getRejectionReason())
                .reviewedBy(entity.getReviewedBy())
                .reviewedAt(entity.getReviewedAt() != null ?
                        entity.getReviewedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .agencyId(entity.getAgency() != null ? entity.getAgency().getId() : null)
                .agencyName(entity.getAgency() != null ? entity.getAgency().getFullName() : null)
                .documents(documentInfos)
                .documentStatuses(documentStatuses)
                .allDocumentsApproved(allDocumentsApproved)
                .isProfileComplete(candidate.isProfileComplete())
                .isEnabled(candidate.getIsEnabled())
                .candidateEmail(candidate.getUser() != null ? candidate.getUser().getEmail() : null)
                .candidatePhone(candidate.getUser() != null ? candidate.getUser().getPhoneNumber() : null)
                .candidatePassportNumber(candidate.getPassportNumber())
                .candidateAge(age)
                .candidateMaritalStatus(candidate.getMaritalStatus() != null ?
                        candidate.getMaritalStatus().name() : null)
                .build();
    }

    private JobApplicationResponse mapToJobApplicationResponse(JobApplication entity) {
        return JobApplicationResponse.builder()
                .id(entity.getId())
                .jobDemandId(entity.getJobDemand().getId())
                .jobTitle(entity.getJobDemand().getTitle())
                .country(entity.getJobDemand().getCountry() != null ?
                        entity.getJobDemand().getCountry().getName() : null)
                .city(entity.getJobDemand().getCity())
                .salaryAmount(entity.getJobDemand().getSalaryAmount())
                .salaryCurrency(entity.getJobDemand().getSalaryCurrency())
                .candidateId(entity.getCandidate().getId())
                .candidateName(entity.getCandidate().getFullName())
                .candidateTrade(entity.getCandidate().getTrade())
                .notes(entity.getNotes())
                .status(entity.getStatus().name())
                .appliedAt(entity.getAppliedAt() != null ?
                        entity.getAppliedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null)
                .rejectionReason(entity.getRejectionReason())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}