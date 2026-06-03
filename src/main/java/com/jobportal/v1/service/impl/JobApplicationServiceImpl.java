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
import com.jobportal.v1.mapper.ApplicationMapper;
import com.jobportal.v1.repository.*;
import com.jobportal.v1.service.JobApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
    private final ApplicationMapper applicationMapper;

    @Override
    @Transactional
    public JobApplicationResponse applyForJob(JobApplicationRequest request, Long agencyId) {
        // 1. Validate job exists
        JobDemand job = jobDemandRepository.findById(request.getJobDemandId())
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        // 2. Job must be open
        if (!job.isOpen()) {
            throw new BadRequestException("Job is not open for applications");
        }

        // 3. Job must be active
        if (!job.getIsActive()) {
            throw new BadRequestException("Job is not active");
        }

        // 4. Job must have remaining slots
        if (job.getRemainingSlots() != null && job.getRemainingSlots() <= 0) {
            throw new BadRequestException("No remaining slots available for this job");
        }

        // 5. Job deadline should not be expired
        if (job.getDeadline() != null && job.getDeadline().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Job deadline has expired");
        }

        // 6. Validate agency is assigned to this job
        boolean isAssigned = assignmentRepository.existsByJobDemandIdAndAgencyIdAndIsEnabledTrue(
                request.getJobDemandId(), agencyId);
        if (!isAssigned) {
            throw new BadRequestException("You are not authorized to apply for this job");
        }

        // 7. Validate candidate exists and belongs to agency
        Candidate candidate = candidateRepository.findByIdAndAgencyId(request.getCandidateId(), agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));

        if (!candidate.getIsEnabled()) {
            throw new BadRequestException("Candidate is disabled. Please enable candidate first.");
        }

        // 8. Optional: Check profile completion
        if (!candidate.isProfileComplete()) {
            throw new BadRequestException("Candidate profile is incomplete. Please complete candidate profile before applying.");
        }

        // 9. Check candidate documents
        if (candidate.isAgencyManaged()) {
            List<CandidateDocument> documents = documentRepository.findByCandidateId(candidate.getId());

            if (documents.isEmpty()) {
                throw new BadRequestException("Cannot apply for job. Candidate has no documents uploaded. Please upload required documents first.");
            }

            boolean hasRejectedDocuments = documents.stream()
                    .anyMatch(doc -> doc.getStatus() == ApprovalStatus.REJECTED);

            if (hasRejectedDocuments) {
                long rejectedCount = documents.stream()
                        .filter(doc -> doc.getStatus() == ApprovalStatus.REJECTED)
                        .count();
                throw new BadRequestException("Cannot apply for job. Candidate has " + rejectedCount + " rejected document(s). Please fix rejected documents first.");
            }
        }

        // 10. Check for existing application
        Optional<JobApplication> existingApplication = jobApplicationRepository
                .findByJobDemandIdAndCandidateId(request.getJobDemandId(), request.getCandidateId());

        if (existingApplication.isPresent()) {
            JobApplication existing = existingApplication.get();

            // If application is WITHDRAWN, allow re-application
            if (existing.getStatus() == ApplicationStatus.WITHDRAWN) {
                existing.setStatus(ApplicationStatus.PENDING);
                existing.setNotes(request.getNotes());
                existing.setAppliedAt(LocalDateTime.now());
                existing.setRejectionReason(null);
                existing.setReviewedBy(null);
                existing.setReviewedAt(null);

                JobApplication updated = jobApplicationRepository.save(existing);
                log.info("Re-applied for job: Job {} by Candidate {} from Agency {} (withdrawn application reactivated)",
                        job.getTitle(), candidate.getFirstName() + candidate.getLastName(), agencyId);
                return mapToAgencyResponse(updated);
            }

            // Block re-application for active statuses
            if (existing.getStatus() == ApplicationStatus.PENDING ||
                    existing.getStatus() == ApplicationStatus.REVIEWED ||
                    existing.getStatus() == ApplicationStatus.SHORTLISTED) {
                throw new BadRequestException("This candidate has already applied for this job with status: " + existing.getStatus());
            }

            // Allow re-application for REJECTED
            if (existing.getStatus() == ApplicationStatus.REJECTED) {
                existing.setStatus(ApplicationStatus.PENDING);
                existing.setNotes(request.getNotes());
                existing.setAppliedAt(LocalDateTime.now());
                existing.setRejectionReason(null);
                existing.setReviewedBy(null);
                existing.setReviewedAt(null);

                JobApplication updated = jobApplicationRepository.save(existing);
                log.info("Re-applied for job: Job {} by Candidate {} from Agency {} (rejected application reactivated)",
                        job.getTitle(), candidate.getFirstName() + candidate.getLastName(), agencyId);
                return mapToAgencyResponse(updated);
            }
        }

        // 11. Create new application
        JobApplication application = new JobApplication();
        application.setJobDemand(job);
        application.setCandidate(candidate);
        application.setAgency(candidate.getAgency());
        application.setNotes(request.getNotes());
        application.setStatus(ApplicationStatus.PENDING);

        JobApplication saved = jobApplicationRepository.save(application);

        // 12. Update counters
        job.incrementAppliedCount();
        jobDemandRepository.save(job);

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
        long total = applicationMapper.countAgencyApplications(jobDemandId, agencyId, status);
        List<AdminApplicationResponse> content = applicationMapper.getAgencyApplications(
                jobDemandId, agencyId, status,
                pageable.getPageSize(),
                (int) pageable.getOffset()
        );

        return new PageImpl<>(content, pageable, total);
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

        // Check if all documents are approved before SHORTLISTING (for agency candidates)
        if (request.getStatus() == ApplicationStatus.SHORTLISTED) {
            Candidate candidate = application.getCandidate();
            List<CandidateDocument> documents = documentRepository.findByCandidateId(candidate.getId());

            boolean allDocumentsApproved = !documents.isEmpty() && documents.stream()
                    .allMatch(doc -> doc.getStatus() == ApprovalStatus.APPROVED);

            if (!allDocumentsApproved) {
                long pendingCount = documents.stream()
                        .filter(doc -> doc.getStatus() == ApprovalStatus.PENDING).count();
                long rejectedCount = documents.stream()
                        .filter(doc -> doc.getStatus() == ApprovalStatus.REJECTED).count();

                String message = String.format(
                        "Cannot shortlist candidate. Document status - Pending: %d, Rejected: %d. Please ensure all documents are approved first.",
                        pendingCount, rejectedCount);
                throw new BadRequestException(message);
            }
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
        long total = applicationMapper.countSelfApplications(jobDemandId, status);
        List<JobApplicationResponse> content = applicationMapper.getSelfApplications(
                jobDemandId, status,
                pageable.getPageSize(),
                (int) pageable.getOffset()
        );

        return new PageImpl<>(content, pageable, total);
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

        if (application.getAgency() != null) {
            throw new BadRequestException("This is not a self-candidate application. Use agency application endpoint.");
        }

        if (request.getStatus() == ApplicationStatus.REJECTED &&
                (request.getRejectionReason() == null || request.getRejectionReason().trim().isEmpty())) {
            throw new BadRequestException("Rejection reason is required when rejecting an application");
        }

        // Check if all documents are approved before SHORTLISTING
        if (request.getStatus() == ApplicationStatus.SHORTLISTED) {
            Candidate candidate = application.getCandidate();
            List<CandidateDocument> documents = documentRepository.findByCandidateId(candidate.getId());

            boolean allDocumentsApproved = !documents.isEmpty() && documents.stream()
                    .allMatch(doc -> doc.getStatus() == ApprovalStatus.APPROVED);

            if (!allDocumentsApproved) {
                long pendingCount = documents.stream()
                        .filter(doc -> doc.getStatus() == ApprovalStatus.PENDING).count();
                long rejectedCount = documents.stream()
                        .filter(doc -> doc.getStatus() == ApprovalStatus.REJECTED).count();

                String message = String.format(
                        "Cannot shortlist candidate. Document status - Pending: %d, Rejected: %d. Please ensure all documents are approved first.",
                        pendingCount, rejectedCount);
                throw new BadRequestException(message);
            }
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