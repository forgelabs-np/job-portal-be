package com.jobportal.v1.repository;

import com.jobportal.v1.entity.CandidateDocument;
import com.jobportal.v1.enums.ApprovalStatus;
import com.jobportal.v1.enums.DocumentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CandidateDocumentRepository extends JpaRepository<CandidateDocument, Long> {
    List<CandidateDocument> findByCandidateId(Long candidateId);

    Optional<CandidateDocument> findByIdAndCandidateId(Long documentId, Long candidateId);

    Optional<CandidateDocument> findByCandidateIdAndDocumentType(Long candidateId, DocumentType documentType);

    boolean existsByCandidateIdAndDocumentType(Long candidateId, DocumentType documentType);

    void deleteByCandidateId(Long candidateId);

    Page<CandidateDocument> findByStatus(ApprovalStatus status, Pageable pageable);

    long countByStatus(ApprovalStatus status);
}