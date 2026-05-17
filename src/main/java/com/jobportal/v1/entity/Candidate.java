package com.jobportal.v1.entity;

import com.jobportal.v1.enums.CandidateType;
import com.jobportal.v1.enums.CreatedByType;
import com.jobportal.v1.enums.MaritalStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "candidates")
@Data
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "agency_id", nullable = true)
    private User agency;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    private String trade;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "marital_status")
    private MaritalStatus maritalStatus;

    @Column(name = "passport_number")
    private String passportNumber;

    @Column(name = "passport_issue_date")
    private LocalDate passportIssueDate;

    @Column(name = "passport_expiry_date")
    private LocalDate passportExpiryDate;

    @Column(name = "documents_folder_link")
    private String documentsFolderLink;

    @Column(name = "intro_video_link")
    private String introVideoLink;

    @Column(name = "is_enabled")
    private Boolean isEnabled = true;

    @Column(name = "created_by")
    private Long createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "candidate_type", nullable = false)
    private CandidateType candidateType = CandidateType.AGENCY_MANAGED;

    @Enumerated(EnumType.STRING)
    @Column(name = "created_by_type", nullable = false)
    private CreatedByType createdByType = CreatedByType.AGENCY;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "profile_complete")
    private Boolean profileComplete = false;

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean isAgencyManaged() {
        return candidateType == CandidateType.AGENCY_MANAGED;
    }

    public boolean isSelfRegistered() {
        return candidateType == CandidateType.SELF_REGISTERED;
    }

    public boolean hasLoginAccess() {
        return user != null;
    }

    public boolean isProfileComplete() {
        return profileComplete != null && profileComplete;
    }
}