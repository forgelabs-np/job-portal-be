package com.jobportal.v1.repository;

import com.jobportal.v1.entity.Interview;
import com.jobportal.v1.enums.InterviewResult;
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

    @Query("SELECT i FROM Interview i WHERE i.candidate.agency.id = :agencyId")
    Page<Interview> findByAgencyId(@Param("agencyId") Long agencyId, Pageable pageable);

    // ✅ Updated - uses only result field
    @Query("SELECT i FROM Interview i WHERE " +
            "(:jobDemandId IS NULL OR i.jobApplication.jobDemand.id = :jobDemandId) AND " +
            "(:result IS NULL OR i.result = :result) AND " +
            "(:agencyId IS NULL OR i.candidate.agency.id = :agencyId)")
    Page<Interview> findAllWithFilters(@Param("jobDemandId") Long jobDemandId,
                                       @Param("result") InterviewResult result,
                                       @Param("agencyId") Long agencyId,
                                       Pageable pageable);

    // For scheduler - day before reminder
    List<Interview> findByScheduledAtBetweenAndResultInAndReminderSentFalse(
            LocalDateTime from,
            LocalDateTime to,
            List<InterviewResult> results
    );

    boolean existsByJobApplicationId(Long jobApplicationId);
}