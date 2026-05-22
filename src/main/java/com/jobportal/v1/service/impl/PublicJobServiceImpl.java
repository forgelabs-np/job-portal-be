package com.jobportal.v1.service.impl;

import com.jobportal.v1.dto.jobDemand.response.JobDemandResponse;
import com.jobportal.v1.entity.JobDemand;
import com.jobportal.v1.enums.JobStatus;
import com.jobportal.v1.exception.ResourceNotFoundException;
import com.jobportal.v1.mapper.JobDemandMapper;
import com.jobportal.v1.repository.JobDemandRepository;
import com.jobportal.v1.service.PublicJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicJobServiceImpl implements PublicJobService {

    private final JobDemandRepository jobDemandRepository;

    @Override
    public Page<JobDemandResponse> getPublicJobs(Pageable pageable) {
        log.debug("Fetching public jobs with pageable: {}", pageable);

        Page<JobDemand> jobs = jobDemandRepository
                .findByIsPublicTrueAndStatusAndIsActiveTrue(JobStatus.OPEN, pageable);

        return jobs.map(JobDemandMapper::toResponse);
    }

    @Override
    public JobDemandResponse getPublicJobById(Long id) {
        log.debug("Fetching public job by id: {}", id);

        JobDemand job = jobDemandRepository
                .findByIdAndIsPublicTrueAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Job not found or not public with id: " + id
                ));

        return JobDemandMapper.toResponse(job);
    }
}