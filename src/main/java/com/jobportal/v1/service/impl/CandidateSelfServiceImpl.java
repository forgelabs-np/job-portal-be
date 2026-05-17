package com.jobportal.v1.service.impl;

import com.jobportal.v1.dto.candidate.request.CandidateProfileRequest;
import com.jobportal.v1.dto.candidate.response.CandidateDocumentResponse;
import com.jobportal.v1.dto.candidate.response.CandidateResponse;
import com.jobportal.v1.dto.candidate.response.StatusResponse;
import com.jobportal.v1.dto.jobApplicationReport.response.JobApplicationResponse;
import com.jobportal.v1.entity.*;
import com.jobportal.v1.enums.*;
import com.jobportal.v1.exception.BadRequestException;
import com.jobportal.v1.exception.ResourceNotFoundException;
import com.jobportal.v1.repository.*;
import com.jobportal.v1.service.CandidateSelfService;
import com.jobportal.v1.util.DocumentValidationUtil;
import com.jobportal.v1.util.FileUploadUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    private final CandidateDocumentRepository documentRepository;
    private final CandidateStatusRepository statusRepository;
    private final UserRepository userRepository;
    private final JobDemandRepository jobDemandRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final FileUploadUtil fileUploadUtil;


    @Override
    @Transactional
    public CandidateResponse createOrUpdateProfile(Long userId, CandidateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Candidate candidate;
        boolean isUpdate = false;

        if (request.getId() != null && request.getId() > 0) {
            candidate = candidateRepository.findByIdAndUserId(request.getId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Candidate profile not found with id: " + request.getId()));
            isUpdate = true;
            log.info("Updating candidate profile: {} by user: {}", request.getFirstName(), userId);
        } else {
            java.util.Optional<Candidate> existing = candidateRepository.findByUserId(userId);
            if (existing.isPresent()) {
                candidate = existing.get();
                isUpdate = true;
                log.info("Updating existing candidate profile: {} by user: {}", request.getFirstName(), userId);
            } else {
                candidate = new Candidate();
                candidate.setUser(user);
                candidate.setAgency(null);
                candidate.setCandidateType(CandidateType.SELF_REGISTERED);
                candidate.setCreatedByType(CreatedByType.CANDIDATE);
                candidate.setCreatedBy(userId);
                candidate.setIsEnabled(true);

                //Set profile complete for self-registered candidates
                candidate.setProfileComplete(true);

                log.info("Creating new candidate profile: {} by user: {}", request.getFirstName(), userId);
            }
        }

        // Map fields
        if (request.getFirstName() != null) candidate.setFirstName(request.getFirstName());
        if (request.getLastName() != null) candidate.setLastName(request.getLastName());
        if (request.getTrade() != null) candidate.setTrade(request.getTrade());
        if (request.getDateOfBirth() != null) candidate.setDateOfBirth(request.getDateOfBirth());
        if (request.getMaritalStatus() != null) candidate.setMaritalStatus(request.getMaritalStatus());
        if (request.getPassportNumber() != null) candidate.setPassportNumber(request.getPassportNumber());
        if (request.getPassportIssueDate() != null) candidate.setPassportIssueDate(request.getPassportIssueDate());
        if (request.getPassportExpiryDate() != null) candidate.setPassportExpiryDate(request.getPassportExpiryDate());
        if (request.getDocumentsFolderLink() != null) candidate.setDocumentsFolderLink(request.getDocumentsFolderLink());
        if (request.getIntroVideoLink() != null) candidate.setIntroVideoLink(request.getIntroVideoLink());

        Candidate saved = candidateRepository.save(candidate);

        // Create status if new
        if (!isUpdate) {
            CandidateStatus status = new CandidateStatus();
            status.setCandidate(saved);
            statusRepository.save(status);
        }

        log.info("Candidate profile {} for user: {}", isUpdate ? "updated" : "created", userId);
        return mapToResponse(saved);
    }

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
    public CandidateDocumentResponse uploadDocument(Long userId, String documentType, MultipartFile file) {
        Candidate candidate = candidateRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate profile not found. Please create your profile first."));

        if (!candidate.isSelfRegistered()) {
            throw new BadRequestException("Only self-registered candidates can upload documents. Your profile is managed by an agency.");
        }

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty. Please select a file to upload.");
        }

        DocumentType validatedDocType = DocumentValidationUtil.validateAndGetDocumentType(documentType);

        try {
            // Check if document already exists for this type
            java.util.Optional<CandidateDocument> existingDoc = documentRepository
                    .findByCandidateIdAndDocumentType(candidate.getId(), validatedDocType);

            String filePath;
            CandidateDocument document;

            if (existingDoc.isPresent()) {
                document = existingDoc.get();
                // Delete old file
                if (document.getDocumentPath() != null) {
                    fileUploadUtil.deleteFile(document.getDocumentPath());
                }
                // Upload new file (validation happens inside FileUploadUtil)
                filePath = fileUploadUtil.uploadSelfCandidateDocument(candidate.getId(), validatedDocType.name(), file);

                document.setDocumentName(file.getOriginalFilename());
                document.setDocumentPath(filePath);
                document.setNotes(null);
                document.setUploadedAt(java.time.LocalDateTime.now());

                log.info("Document re-uploaded for candidate: {}, type: {}", userId, validatedDocType);
            } else {
                // Upload new file (validation happens inside FileUploadUtil)
                filePath = fileUploadUtil.uploadSelfCandidateDocument(candidate.getId(), validatedDocType.name(), file);

                document = new CandidateDocument();
                document.setCandidate(candidate);
                document.setDocumentType(validatedDocType);
                document.setDocumentName(file.getOriginalFilename());
                document.setDocumentPath(filePath);

                log.info("New document uploaded for candidate: {}, type: {}", userId, validatedDocType);
            }

            CandidateDocument saved = documentRepository.save(document);
            return toDocumentResponse(saved);

        } catch (IOException e) {
            log.error("Failed to upload document for user: {}", userId, e);
            throw new RuntimeException("Failed to upload document: " + e.getMessage(), e);
        }
    }

    @Override
    public List<CandidateDocumentResponse> getMyDocuments(Long userId) {
        Candidate candidate = candidateRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate profile not found"));

        return documentRepository.findByCandidateId(candidate.getId()).stream()
                .map(this::toDocumentResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteDocument(Long userId, Long documentId) {
        Candidate candidate = candidateRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate profile not found"));

        CandidateDocument document = documentRepository.findByIdAndCandidateId(documentId, candidate.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        fileUploadUtil.deleteFile(document.getDocumentPath());
        documentRepository.delete(document);

        log.info("Document deleted for candidate: {}, documentId: {}", userId, documentId);
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

        List<CandidateDocumentResponse> documents = documentRepository.findByCandidateId(entity.getId()).stream()
                .map(this::toDocumentResponse)
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
                .isProfileComplete(entity.isProfileComplete())
                .statuses(statuses)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private CandidateDocumentResponse toDocumentResponse(CandidateDocument document) {
        return CandidateDocumentResponse.builder()
                .id(document.getId())
                .documentType(document.getDocumentType().name())
                .documentName(document.getDocumentName())
                .documentPath(document.getDocumentPath())
                .notes(document.getNotes())
                .uploadedAt(document.getUploadedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .status(document.getStatus() != null ? document.getStatus().name() : "PENDING")
                .rejectionReason(document.getRejectionReason())
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