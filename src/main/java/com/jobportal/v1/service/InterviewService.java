package com.jobportal.v1.service;

import com.jobportal.v1.dto.interview.request.InterviewRequest;
import com.jobportal.v1.dto.interview.request.InterviewResultRequest;
import com.jobportal.v1.dto.interview.response.InterviewResponse;
import com.jobportal.v1.enums.InterviewResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InterviewService {

    // Admin methods
    InterviewResponse createOrUpdateInterview(InterviewRequest request, Long adminId);
    InterviewResponse getInterviewById(Long interviewId);
    InterviewResponse getInterviewByApplicationId(Long jobApplicationId);

    // ✅ Combined method - no status parameter needed
    InterviewResponse updateInterviewResult(Long interviewId, InterviewResultRequest request, Long adminId);

    InterviewResponse cancelInterview(Long interviewId, Long adminId);
    void deleteInterview(Long interviewId, Long adminId);

    // ✅ Updated to use only result, not status
    Page<InterviewResponse> getAllInterviews(Long jobDemandId, String result, Long agencyId, Pageable pageable);

    // Agency methods
    Page<InterviewResponse> getAgencyInterviews(Long agencyId, Pageable pageable);
    InterviewResponse getAgencyInterviewById(Long interviewId, Long agencyId);

    // Self-candidate methods
    Page<InterviewResponse> getCandidateInterviews(Long userId, Pageable pageable);
    InterviewResponse getCandidateInterviewById(Long interviewId, Long userId);
}