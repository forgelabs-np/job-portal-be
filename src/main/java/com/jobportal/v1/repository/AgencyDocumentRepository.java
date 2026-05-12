package com.jobportal.v1.repository;

import com.jobportal.v1.entity.AgencyDocument;
import com.jobportal.v1.enums.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgencyDocumentRepository extends JpaRepository<AgencyDocument, Long> {

    List<AgencyDocument> findByAgencyProfileId(Long agencyProfileId);

    Optional<AgencyDocument> findByIdAndAgencyProfileId(Long id, Long agencyProfileId);

    List<AgencyDocument> findByAgencyProfileIdAndStatus(Long agencyProfileId, ApprovalStatus status);

    Optional<AgencyDocument> findByAgencyProfileIdAndDocumentType(Long agencyProfileId, String documentType);
}