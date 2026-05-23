package com.jobportal.v1.service.impl;

import com.jobportal.v1.dto.admin.request.CandidateStatusUpdateRequest;
import com.jobportal.v1.dto.admin.response.AgencyCandidatesGroupResponse;
import com.jobportal.v1.dto.admin.response.CandidateInfo;
import com.jobportal.v1.dto.candidate.request.CandidateRequest;
import com.jobportal.v1.dto.candidate.response.CandidateDocumentResponse;
import com.jobportal.v1.dto.candidate.response.CandidateResponse;
import com.jobportal.v1.dto.candidate.response.StatusResponse;
import com.jobportal.v1.entity.*;
import com.jobportal.v1.enums.*;
import com.jobportal.v1.exception.BadRequestException;
import com.jobportal.v1.exception.ResourceNotFoundException;
import com.jobportal.v1.repository.*;
import com.jobportal.v1.service.CandidateService;
import com.jobportal.v1.util.FileUploadUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CandidateServiceImpl implements CandidateService {

    private final CandidateRepository candidateRepository;
    private final CandidateDocumentRepository documentRepository;
    private final CandidateStatusRepository statusRepository;
    private final UserRepository userRepository;
    private final FileUploadUtil fileUploadUtil;
    private final JobApplicationRepository jobApplicationRepository;

    @Override
    @Transactional
    public CandidateResponse createOrUpdateCandidate(CandidateRequest request, Long agencyId) {
        User agency = userRepository.findById(agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Agency not found"));

        Candidate candidate;
        boolean isUpdate = false;

        if (request.getId() != null && request.getId() > 0) {
            candidate = candidateRepository.findByIdAndAgencyId(request.getId(), agencyId)
                    .orElseThrow(() -> new ResourceNotFoundException("Candidate not found with id: " + request.getId()));
            isUpdate = true;

            // Block modification of self-registered candidates
            if (!candidate.isAgencyManaged()) {
                throw new BadRequestException("Agency cannot modify self-registered candidates");
            }

            log.info("Updating candidate: {} {} by agency: {}", request.getFirstName(), request.getLastName(), agencyId);
        } else {
            candidate = new Candidate();
            candidate.setAgency(agency);
            candidate.setCreatedBy(agencyId);
            candidate.setCreatedByType(CreatedByType.AGENCY);
            candidate.setCandidateType(CandidateType.AGENCY_MANAGED);
            candidate.setIsEnabled(true);
            log.info("Creating new candidate: {} {} by agency: {}", request.getFirstName(), request.getLastName(), agencyId);
        }

        mapRequestToEntity(request, candidate);
        Candidate saved = candidateRepository.save(candidate);

        // Recalculate profileComplete based on rules
        saved.setProfileComplete(saved.calculateProfileComplete());
        candidateRepository.save(saved);

        // Create status only for agency-managed candidates
        if (!isUpdate && saved.isAgencyManaged()) {
            CandidateStatus status = new CandidateStatus();
            status.setCandidate(saved);
            statusRepository.save(status);
        }

        if (request.getDocuments() != null && !request.getDocuments().isEmpty()) {
            for (var docReq : request.getDocuments()) {
                boolean exists = documentRepository.existsByCandidateIdAndDocumentType(
                        saved.getId(), DocumentType.valueOf(docReq.getDocumentType()));

                if (!exists) {
                    CandidateDocument doc = new CandidateDocument();
                    doc.setCandidate(saved);
                    doc.setDocumentType(DocumentType.valueOf(docReq.getDocumentType()));
                    doc.setDocumentName(docReq.getDocumentName());
                    doc.setNotes(docReq.getNotes());
                    documentRepository.save(doc);
                }
            }
        }

        log.info("Candidate {}: {} {}", isUpdate ? "updated" : "created", request.getFirstName(), request.getLastName());

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public CandidateDocumentResponse uploadCandidateDocument(Long candidateId, Long agencyId, String documentType, MultipartFile file) {
        Candidate candidate = candidateRepository.findByIdAndAgencyId(candidateId, agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found with id: " + candidateId));

        //   Block document upload for self-registered candidates
        if (!candidate.isAgencyManaged()) {
            throw new BadRequestException("Agency cannot upload documents for self-registered candidates");
        }

        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        DocumentType validatedDocType;
        try {
            validatedDocType = DocumentType.valueOf(documentType.toUpperCase());
        } catch (IllegalArgumentException e) {
            String allowedTypes = String.join(", ",
                    java.util.Arrays.stream(DocumentType.values())
                            .map(Enum::name)
                            .toArray(String[]::new));
            throw new BadRequestException("Invalid document type: " + documentType +
                    ". Allowed types: " + allowedTypes);
        }

        try {
            java.util.Optional<CandidateDocument> existingDoc = documentRepository
                    .findByCandidateIdAndDocumentType(candidate.getId(), validatedDocType);

            String filePath;
            CandidateDocument document;

            if (existingDoc.isPresent()) {
                document = existingDoc.get();
                if (document.getDocumentPath() != null) {
                    fileUploadUtil.deleteFile(document.getDocumentPath());
                }
                filePath = fileUploadUtil.uploadAgencyCandidateDocument(candidate.getId(), documentType, file);

                document.setDocumentName(file.getOriginalFilename());
                document.setDocumentPath(filePath);
                document.setNotes(null);
                document.setUploadedAt(java.time.LocalDateTime.now());

                //Reset status to PENDING on re-upload
                document.setStatus(ApprovalStatus.PENDING);
                document.setRejectionReason(null);
                document.setVerifiedBy(null);
                document.setVerifiedAt(null);

                log.info("Document re-uploaded for agency candidate: {}, type: {}", candidateId, documentType);
            } else {
                filePath = fileUploadUtil.uploadAgencyCandidateDocument(candidate.getId(), documentType, file);

                document = new CandidateDocument();
                document.setCandidate(candidate);
                document.setDocumentType(validatedDocType);
                document.setDocumentName(file.getOriginalFilename());
                document.setDocumentPath(filePath);
                document.setUploadedAt(java.time.LocalDateTime.now());

                log.info("New document uploaded for agency candidate: {}, type: {}", candidateId, documentType);
            }

            CandidateDocument saved = documentRepository.save(document);
            return toDocumentResponse(saved);

        } catch (IOException e) {
            log.error("Failed to upload document for candidate: {}", candidateId, e);
            throw new RuntimeException("Failed to upload document: " + e.getMessage(), e);
        }
    }

    @Override
    public List<CandidateDocumentResponse> getCandidateDocuments(Long candidateId, Long agencyId) {
        Candidate candidate = candidateRepository.findByIdAndAgencyId(candidateId, agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));

        //   Block viewing documents of self-registered candidates
        if (!candidate.isAgencyManaged()) {
            throw new BadRequestException("Agency cannot view documents of self-registered candidates");
        }

        return documentRepository.findByCandidateId(candidate.getId()).stream()
                .map(this::toDocumentResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteCandidateDocument(Long candidateId, Long agencyId, Long documentId) {
        Candidate candidate = candidateRepository.findByIdAndAgencyId(candidateId, agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));

        //   Block document deletion for self-registered candidates
        if (!candidate.isAgencyManaged()) {
            throw new BadRequestException("Agency cannot delete documents of self-registered candidates");
        }

        CandidateDocument document = documentRepository.findByIdAndCandidateId(documentId, candidate.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        if (document.getDocumentPath() != null) {
            fileUploadUtil.deleteFile(document.getDocumentPath());
        }

        documentRepository.delete(document);
        log.info("Document deleted for agency candidate: {}, documentId: {}", candidateId, documentId);
    }

    @Override
    public CandidateResponse getCandidateById(Long id, Long agencyId) {
        Candidate candidate = candidateRepository.findByIdAndAgencyId(id, agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));

        //   Block access to self-registered candidates
        if (!candidate.isAgencyManaged()) {
            throw new BadRequestException("Agency cannot access self-registered candidates");
        }

        return mapToResponse(candidate);
    }

    @Override
    public Page<CandidateResponse> getAllCandidates(Long agencyId, Boolean isEnabled, Pageable pageable) {
        // This method already only returns agency-managed candidates via repository query
        // The repository method findByAgencyId only returns candidates belonging to that agency
        // which are implicitly agency-managed. But for safety, we add a type filter.

        Page<Candidate> candidates;

        if (isEnabled == null) {
            candidates = candidateRepository.findByAgencyId(agencyId, pageable);
        } else {
            candidates = candidateRepository.findByAgencyIdAndIsEnabled(agencyId, isEnabled, pageable);
        }

        // Filter out any self-registered candidates (defensive programming)
        candidates = new PageImpl<>(
                candidates.getContent().stream()
                        .filter(Candidate::isAgencyManaged)
                        .collect(Collectors.toList()),
                pageable,
                candidates.getTotalElements()
        );

        return candidates.map(this::mapToResponse);
    }

    @Override
    @Transactional
    public CandidateResponse toggleCandidateStatus(Long id, Long agencyId) {
        Candidate candidate = candidateRepository.findByIdAndAgencyId(id, agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));

        // Block toggling status for self-registered candidates
        if (!candidate.isAgencyManaged()) {
            throw new BadRequestException("Agency cannot enable/disable self-registered candidates");
        }

        // Check if we're trying to DISABLE (candidate is currently enabled)
        if (candidate.getIsEnabled()) {
            boolean hasActiveApplications = jobApplicationRepository.existsByCandidateIdAndStatusIn(
                    candidate.getId(),
                    Arrays.asList(
                            ApplicationStatus.PENDING,
                            ApplicationStatus.REVIEWED,
                            ApplicationStatus.SHORTLISTED
                    )
            );

            if (hasActiveApplications) {
                throw new BadRequestException("Cannot disable candidate with active applications (Pending, Reviewed, or Shortlisted). Please resolve active applications first.");
            }
        }

        // Toggle the status
        boolean newStatus = !candidate.getIsEnabled();
        candidate.setIsEnabled(newStatus);
        Candidate saved = candidateRepository.save(candidate);

        String action = newStatus ? "enabled" : "disabled";
        log.info("Candidate {}: {} {} by agency: {}", action, candidate.getFirstName(), candidate.getLastName(), agencyId);

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteCandidate(Long id, Long agencyId) {
        Candidate candidate = candidateRepository.findByIdAndAgencyId(id, agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));

        // Block deletion of self-registered candidates
        if (!candidate.isAgencyManaged()) {
            throw new BadRequestException("Agency cannot delete self-registered candidates");
        }

        List<CandidateDocument> documents = documentRepository.findByCandidateId(id);
        for (CandidateDocument doc : documents) {
            if (doc.getDocumentPath() != null) {
                fileUploadUtil.deleteFile(doc.getDocumentPath());
            }
        }

        documentRepository.deleteByCandidateId(id);
        statusRepository.findByCandidateId(id).ifPresent(statusRepository::delete);
        candidateRepository.delete(candidate);

        log.info("Candidate deleted: {} {} by agency: {}", candidate.getFirstName(), candidate.getLastName(), agencyId);
    }

    @Override
    @Transactional
    public CandidateResponse updateCandidateStatus(Long candidateId, CandidateStatusUpdateRequest request, Long adminId) {
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));

        CandidateStatus status = statusRepository.findByCandidateId(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate status not found"));

        if (request.getPccStatus() != null) {
            status.setPccStatus(request.getPccStatus());
        }
        if (request.getSlcStatus() != null) {
            status.setSlcStatus(request.getSlcStatus());
        }
        if (request.getWorkPermitStatus() != null) {
            status.setWorkPermitStatus(request.getWorkPermitStatus());
        }
        if (request.getVisaStatus() != null) {
            status.setVisaStatus(request.getVisaStatus());
        }

        statusRepository.save(status);
        log.info("Candidate status updated for candidate: {} by admin: {}", candidateId, adminId);

        return mapToResponse(candidate);
    }

    @Override
    public Page<AgencyCandidatesGroupResponse> getAllCandidatesGroupedByAgency(Pageable pageable) {
        Page<User> agencies = userRepository.findByRolesContaining(RoleEnum.AGENCY, pageable);

        List<AgencyCandidatesGroupResponse> responseList = new ArrayList<>();

        for (User agency : agencies.getContent()) {
            List<Candidate> candidates = candidateRepository.findByAgencyIdAndCandidateType(
                    agency.getId(), CandidateType.AGENCY_MANAGED);

            if (!candidates.isEmpty()) {
                List<CandidateInfo> candidateInfos = candidates.stream()
                        .map(candidate -> {
                            CandidateStatus status = statusRepository.findByCandidateId(candidate.getId()).orElse(null);

                            return CandidateInfo.builder()
                                    .id(candidate.getId())
                                    .firstName(candidate.getFirstName())
                                    .lastName(candidate.getLastName())
                                    .fullName(candidate.getFirstName() + " " + candidate.getLastName())
                                    .trade(candidate.getTrade())
                                    .isEnabled(candidate.getIsEnabled())
                                    .pccStatus(status != null ? status.getPccStatus().name() : "PENDING")
                                    .slcStatus(status != null ? status.getSlcStatus().name() : "PENDING")
                                    .workPermitStatus(status != null ? status.getWorkPermitStatus().name() : "PENDING")
                                    .visaStatus(status != null ? status.getVisaStatus().name() : "PENDING")
                                    .createdAt(candidate.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                                    .build();
                        })
                        .collect(Collectors.toList());

                responseList.add(AgencyCandidatesGroupResponse.builder()
                        .agencyId(agency.getId())
                        .agencyName(agency.getFullName())
                        .agencyEmail(agency.getEmail())
                        .candidates(candidateInfos)
                        .build());
            }
        }

        return new PageImpl<>(responseList, pageable, agencies.getTotalElements());
    }

    @Override
    @Transactional
    public CandidateResponse adminToggleCandidateStatus(Long candidateId) {
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));
        candidate.setIsEnabled(!candidate.getIsEnabled());

        Candidate saved = candidateRepository.save(candidate);
        log.info("Admin toggled candidate status for: {}, new status: {}",
                candidate.getFullName(), saved.getIsEnabled());
        return mapToResponse(saved);
    }

    @Override
    public Page<CandidateResponse> getSelfRegisteredCandidates(Pageable pageable) {
        return candidateRepository.findByCandidateType(CandidateType.SELF_REGISTERED, pageable)
                .map(this::mapToResponse);
    }

    private void mapRequestToEntity(CandidateRequest request, Candidate entity) {
        entity.setFirstName(request.getFirstName());
        entity.setLastName(request.getLastName());
        entity.setTrade(request.getTrade());
        entity.setDateOfBirth(request.getDateOfBirth());
        entity.setMaritalStatus(request.getMaritalStatus());
        entity.setPassportNumber(request.getPassportNumber());
        entity.setPassportIssueDate(request.getPassportIssueDate());
        entity.setPassportExpiryDate(request.getPassportExpiryDate());
        entity.setDocumentsFolderLink(request.getDocumentsFolderLink());
        entity.setIntroVideoLink(request.getIntroVideoLink());
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

        StatusResponse statuses = null;
        if (entity.isAgencyManaged()) {
            statuses = statusRepository.findByCandidateId(entity.getId())
                    .map(status -> StatusResponse.builder()
                            .pccStatus(status.getPccStatus().name())
                            .slcStatus(status.getSlcStatus().name())
                            .workPermitStatus(status.getWorkPermitStatus().name())
                            .visaStatus(status.getVisaStatus().name())
                            .build())
                    .orElse(null);
        }

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
                .profileComplete(entity.isProfileComplete())
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
                .uploadedAt(document.getUploadedAt() != null ?
                        document.getUploadedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null)
                .status(document.getStatus() != null ? document.getStatus().name() : "PENDING")
                .rejectionReason(document.getRejectionReason())
                .build();
    }
}