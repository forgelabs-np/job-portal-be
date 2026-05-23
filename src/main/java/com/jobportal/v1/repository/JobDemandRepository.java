package com.jobportal.v1.repository;

import com.jobportal.v1.entity.JobDemand;
import com.jobportal.v1.enums.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobDemandRepository extends JpaRepository<JobDemand, Long> {

    Page<JobDemand> findByIsActiveTrue(Pageable pageable);

    Page<JobDemand> findByStatusAndIsActiveTrue(JobStatus status, Pageable pageable);

    @Query("SELECT j FROM JobDemand j WHERE j.isActive = true AND j.status = 'OPEN' AND j.remainingSlots > 0 AND (j.deadline IS NULL OR j.deadline > CURRENT_TIMESTAMP)")
    List<JobDemand> findAllOpenAndActive();

    Page<JobDemand> findByCreatedByAndIsActiveTrue(Long adminId, Pageable pageable);

    // Public job queries (for self-candidates)
    Page<JobDemand> findByIsPublicTrueAndStatusAndIsActiveTrue(JobStatus status, Pageable pageable);

    Optional<JobDemand> findByIdAndIsPublicTrueAndIsActiveTrue(Long id);

}