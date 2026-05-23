package com.jobportal.v1.service;

import com.jobportal.v1.dto.jobDemand.response.JobDemandResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PublicJobService {


    Page<JobDemandResponse> getPublicJobs(Pageable pageable);

    JobDemandResponse getPublicJobById(Long id);
}