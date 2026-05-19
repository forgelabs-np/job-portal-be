package com.jobportal.v1.service;

import com.jobportal.v1.dto.admin.request.DocumentApprovalRequest;
import com.jobportal.v1.dto.candidate.response.CandidateDocumentResponse;
import com.jobportal.v1.dto.candidate.response.DocumentVerificationStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AdminCandidateDocumentService {

    Page<CandidateDocumentResponse> getPendingDocuments(Pageable pageable);

    List<CandidateDocumentResponse> getDocumentsByCandidate(Long candidateId);

    CandidateDocumentResponse processDocumentApproval(DocumentApprovalRequest request, Long adminId);

    DocumentVerificationStats getDocumentStatistics();
}