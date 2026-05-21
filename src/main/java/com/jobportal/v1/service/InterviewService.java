package com.jobportal.v1.service;

import com.jobportal.v1.dto.interview.request.InterviewRequest;
import com.jobportal.v1.dto.interview.request.InterviewResultRequest;
import com.jobportal.v1.dto.interview.request.InterviewUpdateRequest;
import com.jobportal.v1.dto.interview.response.InterviewResponse;
import com.jobportal.v1.enums.InterviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface InterviewService {

    InterviewResponse createOrUpdateInterview(InterviewRequest request, Long adminId);

    InterviewResponse getInterviewById(Long interviewId);

    InterviewResponse getInterviewByApplicationId(Long jobApplicationId);

    InterviewResponse updateInterviewStatus(Long interviewId, InterviewStatus status, Long adminId);

    InterviewResponse setInterviewResult(Long interviewId, InterviewResultRequest request, Long adminId);

    InterviewResponse cancelInterview(Long interviewId, Long adminId);

    void deleteInterview(Long interviewId, Long adminId);

    Page<InterviewResponse> getAllInterviews(Long jobDemandId, String status, String result, Long agencyId, Pageable pageable);

    // Agency methods
    Page<InterviewResponse> getAgencyInterviews(Long agencyId, Pageable pageable);

    InterviewResponse getAgencyInterviewById(Long interviewId, Long agencyId);

    // Self-candidate methods
    Page<InterviewResponse> getCandidateInterviews(Long userId, Pageable pageable);

    InterviewResponse getCandidateInterviewById(Long interviewId, Long userId);
}