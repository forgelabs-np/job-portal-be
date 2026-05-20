package com.jobportal.v1.repository;

import com.jobportal.v1.entity.Interview;
import com.jobportal.v1.enums.InterviewResult;
import com.jobportal.v1.enums.InterviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, Long> {

    Optional<Interview> findByJobApplicationId(Long jobApplicationId);

    Page<Interview> findByCandidateId(Long candidateId, Pageable pageable);

    Page<Interview> findByStatus(InterviewStatus status, Pageable pageable);

    @Query("SELECT i FROM Interview i WHERE i.candidate.agency.id = :agencyId")
    Page<Interview> findByAgencyId(@Param("agencyId") Long agencyId, Pageable pageable);

    @Query("SELECT i FROM Interview i WHERE " +
            "(:jobDemandId IS NULL OR i.jobApplication.jobDemand.id = :jobDemandId) AND " +
            "(:status IS NULL OR i.status = :status) AND " +
            "(:result IS NULL OR i.result = :result) AND " +
            "(:agencyId IS NULL OR i.candidate.agency.id = :agencyId)")
    Page<Interview> findAllWithFilters(@Param("jobDemandId") Long jobDemandId,
                                       @Param("status") InterviewStatus status,
                                       @Param("result") InterviewResult result,
                                       @Param("agencyId") Long agencyId,
                                       Pageable pageable);

    List<Interview> findByScheduledAtBetweenAndStatusInAndReminderSentFalse(
            LocalDateTime from,
            LocalDateTime to,
            List<InterviewStatus> statuses
    );

    boolean existsByJobApplicationId(Long jobApplicationId);
}