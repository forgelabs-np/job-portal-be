package com.jobportal.v1.service;

import com.jobportal.v1.dto.JobDemand.request.JobAgencyAssignmentRequest;
import com.jobportal.v1.dto.JobDemand.response.JobAgencyAssignmentResponse;
import com.jobportal.v1.dto.agency.response.AgencyJobDetailResponse;
import com.jobportal.v1.dto.agency.response.AgencyJobResponse;

import java.util.List;

public interface JobAgencyAssignmentService {

    List<JobAgencyAssignmentResponse> assignAgenciesToJob(JobAgencyAssignmentRequest request, Long adminId);

    void removeAgencyFromJob(Long jobDemandId, Long agencyId);

    JobAgencyAssignmentResponse toggleAgencyJobAccess(Long jobDemandId, Long agencyId, Boolean enabled);

    List<JobAgencyAssignmentResponse> getAgenciesByJob(Long jobDemandId);

    List<AgencyJobDetailResponse> getJobsByAgency(Long agencyId);

    List<AgencyJobResponse> getMyAssignedJobs(Long agencyId);

    boolean canAgencyApplyToJob(Long agencyId, Long jobDemandId);
}