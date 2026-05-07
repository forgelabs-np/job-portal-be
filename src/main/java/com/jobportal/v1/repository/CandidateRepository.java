package com.jobportal.v1.repository;

import com.jobportal.v1.entity.Candidate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, Long> {

    Page<Candidate> findByAgencyId(Long agencyId, Pageable pageable);

    Page<Candidate> findByAgencyIdAndIsEnabled(Long agencyId, Boolean isEnabled, Pageable pageable);

    Optional<Candidate> findByIdAndAgencyId(Long id, Long agencyId);

    Page<Candidate> findByAgencyIdAndIsEnabledTrue(Long agencyId, Pageable pageable);
}