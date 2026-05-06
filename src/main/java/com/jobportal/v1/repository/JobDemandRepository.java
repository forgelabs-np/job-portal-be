package com.jobportal.v1.repository;

import com.jobportal.v1.entity.JobDemand;
import com.jobportal.v1.enums.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface JobDemandRepository extends JpaRepository<JobDemand, Long> {

    Page<JobDemand> findByIsActiveTrue(Pageable pageable);

    Page<JobDemand> findByStatusAndIsActiveTrue(JobStatus status, Pageable pageable);

    List<JobDemand> findByStatusAndIsActiveTrueAndDeadlineBefore(JobStatus status, LocalDateTime date);

    @Query("SELECT j FROM JobDemand j WHERE j.isActive = true AND j.status = 'OPEN' AND j.remainingSlots > 0 AND (j.deadline IS NULL OR j.deadline > CURRENT_TIMESTAMP)")
    List<JobDemand> findAllOpenAndActive();

    Page<JobDemand> findByCreatedByAndIsActiveTrue(Long adminId, Pageable pageable);

    @Query("SELECT SUM(j.filledSlots) FROM JobDemand j WHERE j.createdBy = :adminId AND j.isActive = true")
    Integer getTotalFilledSlotsByAdmin(@Param("adminId") Long adminId);

    @Query("SELECT SUM(j.totalSlots) FROM JobDemand j WHERE j.createdBy = :adminId AND j.isActive = true")
    Integer getTotalSlotsByAdmin(@Param("adminId") Long adminId);

    @Modifying
    @Transactional
    @Query("UPDATE JobDemand j SET j.status = :status WHERE j.id = :id AND j.isActive = true")
    void updateJobStatus(@Param("id") Long id, @Param("status") JobStatus status);
}