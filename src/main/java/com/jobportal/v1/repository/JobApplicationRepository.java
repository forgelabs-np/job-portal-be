package com.jobportal.v1.repository;

import com.jobportal.v1.entity.JobApplication;
import com.jobportal.v1.enums.ApplicationStatus;
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
import java.util.Optional;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    // Agency queries
    Page<JobApplication> findByAgencyId(Long agencyId, Pageable pageable);

    Page<JobApplication> findByAgencyIdAndJobDemandId(Long agencyId, Long jobDemandId, Pageable pageable);

    Page<JobApplication> findByAgencyIdAndStatus(Long agencyId, ApplicationStatus status, Pageable pageable);

    Page<JobApplication> findByAgencyIdAndJobDemandIdAndStatus(Long agencyId, Long jobDemandId, ApplicationStatus status, Pageable pageable);

    Optional<JobApplication> findByIdAndAgencyId(Long id, Long agencyId);

    Page<JobApplication> findByCandidateId(Long candidateId, Pageable pageable);

    Optional<JobApplication> findByJobDemandIdAndCandidateId(Long jobDemandId, Long candidateId);

    Page<JobApplication> findAll(Pageable pageable);

    Optional<JobApplication> findByIdAndCandidateId(Long id, Long candidateId);

    Page<JobApplication> findByJobDemandId(Long jobDemandId, Pageable pageable);

    Page<JobApplication> findByStatus(ApplicationStatus status, Pageable pageable);

    Page<JobApplication> findByCandidateIdAndStatus(Long candidateId, ApplicationStatus status, Pageable pageable);

    Page<JobApplication> findByJobDemandIdAndStatus(Long jobDemandId, ApplicationStatus status, Pageable pageable);

    Page<JobApplication> findByJobDemandIdAndAgencyId(Long jobDemandId, Long agencyId, Pageable pageable);

    Page<JobApplication> findByJobDemandIdAndAgencyIdAndStatus(Long jobDemandId, Long agencyId, ApplicationStatus status, Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE JobApplication ja SET ja.status = :status, ja.reviewedBy = :reviewedBy, ja.reviewedAt = CURRENT_TIMESTAMP, ja.rejectionReason = :rejectionReason WHERE ja.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") ApplicationStatus status,
                      @Param("reviewedBy") Long reviewedBy, @Param("rejectionReason") String rejectionReason);

    @Query("SELECT COUNT(ja) FROM JobApplication ja WHERE ja.jobDemand.id = :jobDemandId AND ja.status = 'PENDING'")
    Long countPendingByJobDemand(@Param("jobDemandId") Long jobDemandId);

    @Query("SELECT ja FROM JobApplication ja WHERE ja.agency IS NULL")
    Page<JobApplication> findByAgencyIsNull(Pageable pageable);

    @Query("SELECT ja FROM JobApplication ja WHERE ja.jobDemand.id = :jobDemandId AND ja.agency IS NULL")
    Page<JobApplication> findByJobDemandIdAndAgencyIsNull(@Param("jobDemandId") Long jobDemandId, Pageable pageable);

    @Query("SELECT ja FROM JobApplication ja WHERE ja.status = :status AND ja.agency IS NULL")
    Page<JobApplication> findByStatusAndAgencyIsNull(@Param("status") ApplicationStatus status, Pageable pageable);

    @Query("SELECT ja FROM JobApplication ja WHERE ja.jobDemand.id = :jobDemandId AND ja.status = :status AND ja.agency IS NULL")
    Page<JobApplication> findByJobDemandIdAndStatusAndAgencyIsNull(@Param("jobDemandId") Long jobDemandId,
                                                                   @Param("status") ApplicationStatus status,
                                                                   Pageable pageable);

    boolean existsByCandidateIdAndStatusIn(Long candidateId, List<ApplicationStatus> statuses);
}