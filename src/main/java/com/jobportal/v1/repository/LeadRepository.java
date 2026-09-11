package com.jobportal.v1.repository;

import com.jobportal.v1.entity.Lead;
import com.jobportal.v1.enums.LeadSubject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LeadRepository extends JpaRepository<Lead, Long> {

    @Query("SELECT l FROM Lead l WHERE " +
            "(:fullName IS NULL OR CAST(l.fullName AS string) LIKE CONCAT('%', CAST(:fullName AS string), '%')) AND " +
            "(:email IS NULL OR CAST(l.email AS string) LIKE CONCAT('%', CAST(:email AS string), '%')) AND " +
            "(:subject IS NULL OR l.subject = :subject) AND " +
            "(:isRead IS NULL OR l.isRead = :isRead) AND " +
            "(:isProcessed IS NULL OR l.isProcessed = :isProcessed)")
    Page<Lead> findAllWithFilters(
            @Param("fullName") String fullName,
            @Param("email") String email,
            @Param("subject") LeadSubject subject,
            @Param("isRead") Boolean isRead,
            @Param("isProcessed") Boolean isProcessed,
            Pageable pageable);

    long countByIsReadFalse();

    long countByIsProcessedFalse();
}
