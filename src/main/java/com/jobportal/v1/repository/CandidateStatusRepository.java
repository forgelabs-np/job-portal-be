package com.jobportal.v1.repository;

import com.jobportal.v1.entity.CandidateStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CandidateStatusRepository extends JpaRepository<CandidateStatus, Long> {

    Optional<CandidateStatus> findByCandidateId(Long candidateId);
}