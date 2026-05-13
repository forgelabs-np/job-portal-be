package com.jobportal.v1.service;

import com.jobportal.v1.dto.candidate.request.CandidateProfileUpdateRequest;
import com.jobportal.v1.dto.candidate.response.CandidateResponse;
import com.jobportal.v1.dto.jobApplicationReport.response.JobApplicationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CandidateSelfService {

    CandidateResponse getMyProfile(Long userId);

    CandidateResponse updateMyProfile(Long userId, CandidateProfileUpdateRequest request);

    Page<JobApplicationResponse> getMyApplications(Long userId, Pageable pageable);

    JobApplicationResponse applyForJob(Long userId, Long jobDemandId, String notes);
}