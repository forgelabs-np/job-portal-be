package com.jobportal.v1.repository;

import com.jobportal.v1.entity.JobAgencyAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobAgencyAssignmentRepository extends JpaRepository<JobAgencyAssignment, Long> {

    Optional<JobAgencyAssignment> findByJobDemandIdAndAgencyId(Long jobDemandId, Long agencyId);

    List<JobAgencyAssignment> findByJobDemandId(Long jobDemandId);

    List<JobAgencyAssignment> findByAgencyId(Long agencyId);

    List<JobAgencyAssignment> findByJobDemandIdAndIsEnabledTrue(Long jobDemandId);

    List<JobAgencyAssignment> findByAgencyIdAndIsEnabledTrue(Long agencyId);

    boolean existsByJobDemandIdAndAgencyIdAndIsEnabledTrue(Long jobDemandId, Long agencyId);

    @Modifying
    @Transactional
    @Query("DELETE FROM JobAgencyAssignment j WHERE j.jobDemand.id = :jobDemandId")
    void deleteByJobDemandId(@Param("jobDemandId") Long jobDemandId);

    @Modifying
    @Transactional
    @Query("DELETE FROM JobAgencyAssignment j WHERE j.agency.id = :agencyId")
    void deleteByAgencyId(@Param("agencyId") Long agencyId);
}