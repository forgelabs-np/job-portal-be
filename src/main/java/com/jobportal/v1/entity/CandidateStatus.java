package com.jobportal.v1.entity;

import com.jobportal.v1.enums.DocumentStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "candidate_statuses")
@Data
public class CandidateStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "candidate_id", nullable = false, unique = true)
    private Candidate candidate;

    @Enumerated(EnumType.STRING)
    @Column(name = "pcc_status")
    private DocumentStatus pccStatus = DocumentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "slc_status")
    private DocumentStatus slcStatus = DocumentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_permit_status")
    private DocumentStatus workPermitStatus = DocumentStatus.NOT_STARTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "visa_status")
    private DocumentStatus visaStatus = DocumentStatus.NOT_STARTED;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}