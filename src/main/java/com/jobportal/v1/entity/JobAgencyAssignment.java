package com.jobportal.v1.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_agency_assignments", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"job_demand_id", "agency_id"})
})
@Data
public class JobAgencyAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "job_demand_id", nullable = false)
    private JobDemand jobDemand;

    @ManyToOne
    @JoinColumn(name = "agency_id", nullable = false)
    private User agency;

    @Column(name = "is_enabled")
    private Boolean isEnabled = true;

    @Column(name = "assigned_by")
    private Long assignedBy;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}