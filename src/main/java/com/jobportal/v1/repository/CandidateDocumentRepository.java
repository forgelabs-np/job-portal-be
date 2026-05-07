package com.jobportal.v1.repository;
import com.jobportal.v1.entity.CandidateDocument;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CandidateDocumentRepository extends JpaRepository<CandidateDocument, Long> {

    List<CandidateDocument> findByCandidateId(Long candidateId);

    @Modifying
    @Transactional
    @Query("DELETE FROM CandidateDocument d WHERE d.candidate.id = :candidateId")
    void deleteByCandidateId(@Param("candidateId") Long candidateId);
}
