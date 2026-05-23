package com.jobportal.v1.repository;

import com.jobportal.v1.entity.Candidate;
import com.jobportal.v1.enums.CandidateType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, Long> {

    // Agency methods
    Page<Candidate> findByAgencyId(Long agencyId, Pageable pageable);

    List<Candidate> findByAgencyId(Long agencyId);

    Page<Candidate> findByAgencyIdAndIsEnabled(Long agencyId, Boolean isEnabled, Pageable pageable);

    Optional<Candidate> findByIdAndAgencyId(Long id, Long agencyId);

    Page<Candidate> findByAgencyIdAndIsEnabledTrue(Long agencyId, Pageable pageable);

    // Self-registered candidate methods
    Optional<Candidate> findByUserId(Long userId);

    Optional<Candidate> findByIdAndUserId(Long id, Long userId);

    Page<Candidate> findByCandidateType(CandidateType type, Pageable pageable);

    boolean existsByUserId(Long userId);

    List<Candidate> findByAgencyIdAndCandidateType(Long agencyId, CandidateType candidateType);

    // With pagination
    Page<Candidate> findByAgencyIdAndCandidateType(Long agencyId, CandidateType candidateType, Pageable pageable);
}