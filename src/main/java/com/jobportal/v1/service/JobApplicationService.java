package com.jobportal.v1.service;

import com.jobportal.v1.dto.admin.response.AdminApplicationResponse;
import com.jobportal.v1.dto.agency.response.AgencyJobApplicationResponse;
import com.jobportal.v1.dto.jobApplicationReport.request.ApplicationStatusUpdateRequest;
import com.jobportal.v1.dto.jobApplicationReport.request.JobApplicationRequest;
import com.jobportal.v1.dto.jobApplicationReport.response.JobApplicationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface JobApplicationService {

    // Agency methods
    JobApplicationResponse applyForJob(JobApplicationRequest request, Long agencyId);

    Page<JobApplicationResponse> getMyApplications(Long agencyId, Long jobDemandId, String status, Pageable pageable);

    AgencyJobApplicationResponse getMyApplicationById(Long applicationId, Long agencyId);

    JobApplicationResponse withdrawApplication(Long applicationId, Long agencyId);

    // Admin methods
    Page<AdminApplicationResponse> getAllApplications(Long jobDemandId, Long agencyId, String status, Pageable pageable);

    AdminApplicationResponse updateApplicationStatus(Long applicationId, ApplicationStatusUpdateRequest request, Long adminId);

    AgencyJobApplicationResponse getApplicationDetails(Long applicationId);
}