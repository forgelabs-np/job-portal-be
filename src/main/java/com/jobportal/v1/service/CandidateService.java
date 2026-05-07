package com.jobportal.v1.service;

import com.jobportal.v1.dto.admin.request.CandidateStatusUpdateRequest;
import com.jobportal.v1.dto.admin.response.AgencyCandidatesGroupResponse;
import com.jobportal.v1.dto.candidate.request.CandidateRequest;
import com.jobportal.v1.dto.candidate.response.CandidateResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CandidateService {

    CandidateResponse createOrUpdateCandidate(CandidateRequest request, Long agencyId);

    CandidateResponse getCandidateById(Long id, Long agencyId);

    Page<CandidateResponse> getAllCandidates(Long agencyId, Boolean isEnabled, Pageable pageable);

    CandidateResponse toggleCandidateStatus(Long id, Long agencyId);

    void deleteCandidate(Long id, Long agencyId);

    // Admin method
    CandidateResponse updateCandidateStatus(Long candidateId, CandidateStatusUpdateRequest request, Long adminId);

    List<AgencyCandidatesGroupResponse> getAllCandidatesGroupedByAgency();
}