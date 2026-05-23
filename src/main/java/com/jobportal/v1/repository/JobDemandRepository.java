package com.jobportal.v1.repository;

import com.jobportal.v1.entity.JobDemand;
import com.jobportal.v1.enums.JobStatus;
import com.jobportal.v1.repository.projection.DailyJobCountProjection;
import com.jobportal.v1.repository.projection.JobAggregateStatsProjection;
import com.jobportal.v1.repository.projection.JobStatusCountProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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

    // Dashboard queries
    @Query("SELECT COUNT(j) FROM JobDemand j WHERE j.isActive = true")
    Long countAllActiveJobs();

    @Query("SELECT COUNT(j) FROM JobDemand j WHERE j.isActive = true AND j.status = :status")
    Long countByStatus(@Param("status") JobStatus status);

    @Query("SELECT COALESCE(SUM(j.totalSlots), 0) FROM JobDemand j WHERE j.isActive = true")
    Long sumTotalSlots();

    @Query("SELECT COALESCE(SUM(j.filledSlots), 0) FROM JobDemand j WHERE j.isActive = true")
    Long sumFilledSlots();

    @Query("SELECT COALESCE(SUM(j.appliedCount), 0) FROM JobDemand j WHERE j.isActive = true")
    Long sumAppliedCount();

    @Query("SELECT j FROM JobDemand j WHERE j.isActive = true ORDER BY j.createdAt DESC")
    List<JobDemand> findRecentJobs(Pageable pageable);

    @Query("SELECT COUNT(j) FROM JobDemand j WHERE j.createdAt BETWEEN :start AND :end")
    Long countByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Public job queries (for self-candidates)
    Page<JobDemand> findByIsPublicTrueAndStatusAndIsActiveTrue(JobStatus status, Pageable pageable);

    Optional<JobDemand> findByIdAndIsPublicTrueAndIsActiveTrue(Long id);

    @Query("SELECT COUNT(j) FROM JobDemand j WHERE j.isPublic = true AND j.status = :status AND j.isActive = true")
    long countByIsPublicTrueAndStatusAndIsActiveTrue(@Param("status") JobStatus status);

    @Query("SELECT j.status as status, COUNT(j) as count FROM JobDemand j WHERE j.isActive = true GROUP BY j.status")
    List<JobStatusCountProjection> getJobStatusCounts();

    @Query("SELECT COUNT(j) as totalJobs, COALESCE(SUM(j.totalSlots), 0) as totalSlots, COALESCE(SUM(j.filledSlots), 0) as filledSlots FROM JobDemand j WHERE j.isActive = true")
    JobAggregateStatsProjection getJobAggregateStats();

    @Query("SELECT FUNCTION('DATE', j.createdAt) as date, COUNT(j) as count " +
            "FROM JobDemand j " +
            "WHERE j.createdAt >= :startDate AND j.isActive = true " +
            "GROUP BY FUNCTION('DATE', j.createdAt)")
    List<DailyJobCountProjection> getDailyJobCounts(@Param("startDate") LocalDateTime startDate);
}