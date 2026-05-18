package com.jobportal.v1.service;

import com.jobportal.v1.dto.admin.request.CandidateStatusUpdateRequest;
import com.jobportal.v1.dto.admin.response.AgencyCandidatesGroupResponse;
import com.jobportal.v1.dto.candidate.request.CandidateRequest;
import com.jobportal.v1.dto.candidate.response.CandidateDocumentResponse;
import com.jobportal.v1.dto.candidate.response.CandidateResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CandidateService {

    // Agency candidate management
    CandidateResponse createOrUpdateCandidate(CandidateRequest request, Long agencyId);

    CandidateResponse getCandidateById(Long id, Long agencyId);

    Page<CandidateResponse> getAllCandidates(Long agencyId, Boolean isEnabled, Pageable pageable);

    CandidateResponse toggleCandidateStatus(Long id, Long agencyId);

    void deleteCandidate(Long id, Long agencyId);

    // Document management
    CandidateDocumentResponse uploadCandidateDocument(Long candidateId, Long agencyId, String documentType, MultipartFile file);

    List<CandidateDocumentResponse> getCandidateDocuments(Long candidateId, Long agencyId);

    void deleteCandidateDocument(Long candidateId, Long agencyId, Long documentId);

    // Admin operations
    CandidateResponse updateCandidateStatus(Long candidateId, CandidateStatusUpdateRequest request, Long adminId);

    Page<AgencyCandidatesGroupResponse> getAllCandidatesGroupedByAgency(Pageable pageable);

    CandidateResponse adminToggleCandidateStatus(Long candidateId);

    Page<CandidateResponse> getSelfRegisteredCandidates(Pageable pageable);
}