package com.jobportal.v1.service.impl;

import com.jobportal.v1.dto.admin.request.CandidateStatusUpdateRequest;
import com.jobportal.v1.dto.admin.response.AgencyCandidatesGroupResponse;
import com.jobportal.v1.dto.admin.response.CandidateInfo;
import com.jobportal.v1.dto.candidate.request.CandidateRequest;
import com.jobportal.v1.dto.candidate.response.CandidateResponse;
import com.jobportal.v1.dto.candidate.response.DocumentResponse;
import com.jobportal.v1.dto.candidate.response.StatusResponse;
import com.jobportal.v1.entity.*;
import com.jobportal.v1.enums.DocumentType;
import com.jobportal.v1.enums.RoleEnum;
import com.jobportal.v1.exception.ResourceNotFoundException;
import com.jobportal.v1.repository.*;
import com.jobportal.v1.service.CandidateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CandidateServiceImpl implements CandidateService {

    private final CandidateRepository candidateRepository;
    private final CandidateDocumentRepository documentRepository;
    private final CandidateStatusRepository statusRepository;
    private final UserRepository userRepository;

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
            log.info("Updating candidate: {} {} by agency: {}", request.getFirstName(), request.getLastName(), agencyId);
        } else {
            candidate = new Candidate();
            candidate.setAgency(agency);
            candidate.setCreatedBy(agencyId);
            candidate.setIsEnabled(true);
            log.info("Creating new candidate: {} {} by agency: {}", request.getFirstName(), request.getLastName(), agencyId);
        }

        mapRequestToEntity(request, candidate);
        Candidate saved = candidateRepository.save(candidate);

        if (!isUpdate) {
            CandidateStatus status = new CandidateStatus();
            status.setCandidate(saved);
            statusRepository.save(status);
        }

        // Handle documents - delete old and add new
        documentRepository.deleteByCandidateId(saved.getId());
        if (request.getDocuments() != null && !request.getDocuments().isEmpty()) {
            for (var docReq : request.getDocuments()) {
                CandidateDocument doc = new CandidateDocument();
                doc.setCandidate(saved);
                doc.setDocumentType(DocumentType.valueOf(docReq.getDocumentType()));
                doc.setDocumentName(docReq.getDocumentName());
                doc.setDocumentLink(docReq.getDocumentLink());
                doc.setNotes(docReq.getNotes());
                documentRepository.save(doc);
            }
        }

        String message = isUpdate ? "Candidate updated successfully" : "Candidate created successfully";
        log.info("Candidate {}: {} {}", isUpdate ? "updated" : "created", request.getFirstName(), request.getLastName());

        return mapToResponse(saved);
    }

    @Override
    public CandidateResponse getCandidateById(Long id, Long agencyId) {
        Candidate candidate = candidateRepository.findByIdAndAgencyId(id, agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));
        return mapToResponse(candidate);
    }

    @Override
    public Page<CandidateResponse> getAllCandidates(Long agencyId, Boolean isEnabled, Pageable pageable) {
        Page<Candidate> candidates;

        if (isEnabled == null) {
            candidates = candidateRepository.findByAgencyId(agencyId, pageable);
        } else {
            candidates = candidateRepository.findByAgencyIdAndIsEnabled(agencyId, isEnabled, pageable);
        }

        return candidates.map(this::mapToResponse);
    }

    @Override
    @Transactional
    public CandidateResponse toggleCandidateStatus(Long id, Long agencyId) {
        Candidate candidate = candidateRepository.findByIdAndAgencyId(id, agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found"));

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
    public List<AgencyCandidatesGroupResponse> getAllCandidatesGroupedByAgency() {
        // Get all agencies
        List<User> agencies = userRepository.findByRolesContaining(RoleEnum.AGENCY);

        List<AgencyCandidatesGroupResponse> response = new ArrayList<>();

        for (User agency : agencies) {
            List<Candidate> candidates = candidateRepository.findByAgencyId(agency.getId());

            if (!candidates.isEmpty()) {
                List<CandidateInfo> candidateInfos = candidates.stream()
                        .map(candidate -> {
                            // Get statuses
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
                                    .workPermitStatus(status != null ? status.getWorkPermitStatus().name() : "NOT_STARTED")
                                    .visaStatus(status != null ? status.getVisaStatus().name() : "NOT_STARTED")
                                    .createdAt(candidate.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                                    .build();
                        })
                        .collect(Collectors.toList());

                response.add(AgencyCandidatesGroupResponse.builder()
                        .agencyId(agency.getId())
                        .agencyName(agency.getFullName())
                        .agencyEmail(agency.getEmail())
                        .candidates(candidateInfos)
                        .build());
            }
        }

        return response;
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
                .agencyId(entity.getAgency().getId())
                .agencyName(entity.getAgency().getFullName())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .fullName(entity.getFirstName() + " " + entity.getLastName())
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
}