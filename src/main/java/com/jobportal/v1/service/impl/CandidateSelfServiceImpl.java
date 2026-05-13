package com.jobportal.v1.service.impl;

import com.jobportal.v1.dto.candidate.request.CandidateProfileUpdateRequest;
import com.jobportal.v1.dto.candidate.response.CandidateResponse;
import com.jobportal.v1.dto.candidate.response.DocumentResponse;
import com.jobportal.v1.dto.candidate.response.StatusResponse;
import com.jobportal.v1.dto.jobApplicationReport.response.JobApplicationResponse;
import com.jobportal.v1.entity.*;
import com.jobportal.v1.enums.ApplicationStatus;
import com.jobportal.v1.exception.BadRequestException;
import com.jobportal.v1.exception.ResourceNotFoundException;
import com.jobportal.v1.repository.*;
import com.jobportal.v1.service.CandidateSelfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CandidateSelfServiceImpl implements CandidateSelfService {

    private final CandidateRepository candidateRepository;
    private final JobDemandRepository jobDemandRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final CandidateDocumentRepository documentRepository;
    private final CandidateStatusRepository statusRepository;

    @Override
    public CandidateResponse getMyProfile(Long userId) {
        Candidate candidate = candidateRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate profile not found"));

        if (!candidate.isSelfRegistered()) {
            throw new BadRequestException("This profile is managed by an agency. Please contact your agency.");
        }

        return mapToResponse(candidate);
    }

    @Override
    @Transactional
    public CandidateResponse updateMyProfile(Long userId, CandidateProfileUpdateRequest request) {
        Candidate candidate = candidateRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate profile not found"));

        if (!candidate.isSelfRegistered()) {
            throw new BadRequestException("This profile is managed by an agency. Please contact your agency.");
        }

        if (request.getTrade() != null) candidate.setTrade(request.getTrade());
        if (request.getDateOfBirth() != null) candidate.setDateOfBirth(request.getDateOfBirth());
        if (request.getMaritalStatus() != null) candidate.setMaritalStatus(request.getMaritalStatus());
        if (request.getPassportNumber() != null) candidate.setPassportNumber(request.getPassportNumber());
        if (request.getPassportIssueDate() != null) candidate.setPassportIssueDate(request.getPassportIssueDate());
        if (request.getPassportExpiryDate() != null) candidate.setPassportExpiryDate(request.getPassportExpiryDate());
        if (request.getDocumentsFolderLink() != null) candidate.setDocumentsFolderLink(request.getDocumentsFolderLink());
        if (request.getIntroVideoLink() != null) candidate.setIntroVideoLink(request.getIntroVideoLink());

        Candidate saved = candidateRepository.save(candidate);
        log.info("Self-registered candidate updated profile: {}", userId);

        return mapToResponse(saved);
    }

    @Override
    public Page<JobApplicationResponse> getMyApplications(Long userId, Pageable pageable) {
        Candidate candidate = candidateRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate profile not found"));

        return jobApplicationRepository.findByCandidateId(candidate.getId(), pageable)
                .map(this::mapToApplicationResponse);
    }

    @Override
    @Transactional
    public JobApplicationResponse applyForJob(Long userId, Long jobDemandId, String notes) {
        Candidate candidate = candidateRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate profile not found"));

        if (!candidate.isSelfRegistered()) {
            throw new BadRequestException("Agency-managed candidates must apply through their agency.");
        }

        if (!candidate.getIsEnabled()) {
            throw new BadRequestException("Your profile is disabled. Please contact support.");
        }

        JobDemand job = jobDemandRepository.findById(jobDemandId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));

        if (!job.isOpen()) {
            throw new BadRequestException("Job is not open for applications");
        }

        boolean alreadyApplied = jobApplicationRepository.existsByJobDemandIdAndCandidateId(jobDemandId, candidate.getId());
        if (alreadyApplied) {
            throw new BadRequestException("You have already applied for this job");
        }

        JobApplication application = new JobApplication();
        application.setJobDemand(job);
        application.setCandidate(candidate);
        application.setAgency(null);
        application.setNotes(notes);
        application.setStatus(ApplicationStatus.PENDING);

        JobApplication saved = jobApplicationRepository.save(application);
        log.info("Self-registered candidate applied for job: {} by user: {}", jobDemandId, userId);

        return mapToApplicationResponse(saved);
    }

    private CandidateResponse mapToResponse(Candidate entity) {
        Integer age = null;
        if (entity.getDateOfBirth() != null) {
            age = Period.between(entity.getDateOfBirth(), LocalDate.now()).getYears();
        }

        Boolean isPassportValid = null;
        if (entity.getPassportExpiryDate() != null) {
            isPassportValid = entity.getPassportExpiryDate().isAfter(LocalDate.now());
        }

        List<DocumentResponse> documents = documentRepository.findByCandidateId(entity.getId()).stream()
                .map(doc -> DocumentResponse.builder()
                        .id(doc.getId())
                        .documentType(doc.getDocumentType().name())
                        .documentName(doc.getDocumentName())
                        .documentLink(doc.getDocumentLink())
                        .notes(doc.getNotes())
                        .uploadedAt(doc.getUploadedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                        .build())
                .collect(Collectors.toList());

        StatusResponse statuses = statusRepository.findByCandidateId(entity.getId())
                .map(status -> StatusResponse.builder()
                        .pccStatus(status.getPccStatus().name())
                        .slcStatus(status.getSlcStatus().name())
                        .workPermitStatus(status.getWorkPermitStatus().name())
                        .visaStatus(status.getVisaStatus().name())
                        .build())
                .orElse(null);

        return CandidateResponse.builder()
                .id(entity.getId())
                .agencyId(entity.getAgency() != null ? entity.getAgency().getId() : null)
                .agencyName(entity.getAgency() != null ? entity.getAgency().getFullName() : null)
                .userId(entity.getUser() != null ? entity.getUser().getId() : null)
                .userEmail(entity.getUser() != null ? entity.getUser().getEmail() : null)
                .candidateType(entity.getCandidateType().name())
                .createdByType(entity.getCreatedByType().name())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .fullName(entity.getFullName())
                .trade(entity.getTrade())
                .dateOfBirth(entity.getDateOfBirth())
                .age(age)
                .maritalStatus(entity.getMaritalStatus())
                .passportNumber(entity.getPassportNumber())
                .passportIssueDate(entity.getPassportIssueDate())
                .passportExpiryDate(entity.getPassportExpiryDate())
                .isPassportValid(isPassportValid)
                .documentsFolderLink(entity.getDocumentsFolderLink())
                .introVideoLink(entity.getIntroVideoLink())
                .isEnabled(entity.getIsEnabled())
                .documents(documents)
                .statuses(statuses)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private JobApplicationResponse mapToApplicationResponse(JobApplication entity) {
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