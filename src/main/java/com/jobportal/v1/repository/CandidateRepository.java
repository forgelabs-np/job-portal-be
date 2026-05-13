package com.jobportal.v1.repository;

import com.jobportal.v1.entity.Candidate;
import com.jobportal.v1.enums.CandidateType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, Long> {

    // ===== AGENCY METHODS =====
    Page<Candidate> findByAgencyId(Long agencyId, Pageable pageable);

    List<Candidate> findByAgencyId(Long agencyId);

    Page<Candidate> findByAgencyIdAndIsEnabled(Long agencyId, Boolean isEnabled, Pageable pageable);

    Optional<Candidate> findByIdAndAgencyId(Long id, Long agencyId);

    Page<Candidate> findByAgencyIdAndIsEnabledTrue(Long agencyId, Pageable pageable);

    // ===== SELF-REGISTERED CANDIDATE METHODS =====
    Optional<Candidate> findByUserId(Long userId);

    Optional<Candidate> findByIdAndUserId(Long id, Long userId);

    Page<Candidate> findByCandidateType(CandidateType type, Pageable pageable);

    boolean existsByUserId(Long userId);

    // ===== ADMIN METHODS =====
    @Query("SELECT c FROM Candidate c WHERE c.candidateType = :type AND c.agency.id = :agencyId")
    Page<Candidate> findByAgencyIdAndCandidateType(@Param("agencyId") Long agencyId, @Param("type") CandidateType type, Pageable pageable);
}