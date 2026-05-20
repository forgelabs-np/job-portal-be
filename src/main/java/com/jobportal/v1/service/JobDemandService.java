package com.jobportal.v1.service;

import com.jobportal.v1.dto.jobDemand.request.JobDemandRequest;
import com.jobportal.v1.dto.jobDemand.response.JobDemandResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface JobDemandService {

    JobDemandResponse createOrUpdateJobDemand(JobDemandRequest request, Long adminId);

    JobDemandResponse getJobDemandById(Long id);

    Page<JobDemandResponse> getAllJobDemands(Pageable pageable);

    Page<JobDemandResponse> getJobDemandsByStatus(String status, Pageable pageable);

    Page<JobDemandResponse> getJobDemandsByAdmin(Long adminId, Pageable pageable);

    List<JobDemandResponse> getOpenJobDemands();

    JobDemandResponse closeJobDemand(Long id, Long adminId);

    void deleteJobDemand(Long id, Long adminId);
}