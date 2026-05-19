package com.jobportal.v1.service;

import com.jobportal.v1.dto.candidate.request.CandidateProfileRequest;
import com.jobportal.v1.dto.candidate.response.CandidateDocumentResponse;
import com.jobportal.v1.dto.candidate.response.CandidateJobApplicationResponse;
import com.jobportal.v1.dto.candidate.response.CandidateResponse;
import com.jobportal.v1.dto.jobApplicationReport.response.JobApplicationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CandidateSelfService {

    // Profile management
    CandidateResponse createOrUpdateProfile(Long userId, CandidateProfileRequest request);

    CandidateResponse getMyProfile(Long userId);

    // Document management
    CandidateDocumentResponse uploadDocument(Long userId, String documentType, MultipartFile file);

    List<CandidateDocumentResponse> getMyDocuments(Long userId);

    void deleteDocument(Long userId, Long documentId);

    // Job applications
    Page<JobApplicationResponse> getMyApplications(Long userId, Pageable pageable);

    CandidateJobApplicationResponse getMyApplicationById(Long userId, Long applicationId);

    JobApplicationResponse applyForJob(Long userId, Long jobDemandId, String notes);

    JobApplicationResponse withdrawApplication(Long userId, Long applicationId);
}