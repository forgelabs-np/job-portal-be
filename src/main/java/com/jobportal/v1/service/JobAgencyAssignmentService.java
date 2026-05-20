package com.jobportal.v1.service;

import com.jobportal.v1.dto.jobDemand.request.JobAgencyAssignmentRequest;
import com.jobportal.v1.dto.jobDemand.response.JobAgencyAssignmentResponse;
import com.jobportal.v1.dto.agency.response.AgencyJobDetailResponse;
import com.jobportal.v1.dto.agency.response.AgencyJobResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface JobAgencyAssignmentService {

    List<JobAgencyAssignmentResponse> assignAgenciesToJob(JobAgencyAssignmentRequest request, Long adminId);

    void removeAgencyFromJob(Long jobDemandId, Long agencyId);

    JobAgencyAssignmentResponse toggleAgencyJobAccess(Long jobDemandId, Long agencyId, Boolean enabled);

    List<JobAgencyAssignmentResponse> getAgenciesByJob(Long jobDemandId);

    List<AgencyJobDetailResponse> getJobsByAgency(Long agencyId);

    Page<AgencyJobResponse> getMyAssignedJobs(Long agencyId, Pageable pageable);

    boolean canAgencyApplyToJob(Long agencyId, Long jobDemandId);
}