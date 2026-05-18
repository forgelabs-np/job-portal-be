package com.jobportal.v1.entity;

import com.jobportal.v1.enums.CandidateType;
import com.jobportal.v1.enums.CreatedByType;
import com.jobportal.v1.enums.MaritalStatus;
import com.jobportal.v1.enums.OnboardingStage;
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
    @Column(name = "onboarding_stage")
    private OnboardingStage onboardingStage = OnboardingStage.PROFILE;

    @Enumerated(EnumType.STRING)
    @Column(name = "created_by_type", nullable = false)
    private CreatedByType createdByType = CreatedByType.AGENCY;

    @Column(name = "profile_complete")
    private Boolean profileComplete = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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

    /**
     * Checks if profileComplete flag is set.
     * Used in response mapping — reflects the stored value.
     * Do NOT use this to calculate completeness; use calculateProfileComplete() for that.
     */
    public boolean isProfileComplete() {
        return profileComplete != null && profileComplete;
    }

    /**
     * Calculates whether the candidate profile meets the minimum required fields.
     *
     * Rules by candidate type:
     *
     * SELF_REGISTERED — stricter, because the candidate manages their own data:
     *   Required: firstName, lastName, trade, dateOfBirth, maritalStatus,
     *             passportNumber, passportIssueDate, passportExpiryDate
     *
     * AGENCY_MANAGED — looser, because agencies may add data incrementally:
     *   Required: firstName, lastName only
     *   (agency is responsible for completing the rest over time)
     *
     * Call this method after saving the candidate, then persist the result
     * back to profileComplete. Do not hardcode true/false on creation.
     */
    public boolean calculateProfileComplete() {
        // First name and last name are always required for both types
        if (isBlank(firstName) || isBlank(lastName)) {
            return false;
        }

        if (isSelfRegistered()) {
            // Self-registered candidates must fill all core fields
            // before their profile is considered complete
            return !isBlank(trade)
                    && dateOfBirth != null
                    && maritalStatus != null
                    && !isBlank(passportNumber)
                    && passportIssueDate != null
                    && passportExpiryDate != null;
        }

        // AGENCY_MANAGED: name is enough to consider profile created
        // Agency fills the rest progressively
        return true;
    }

    public boolean isOnboardingComplete() {
        return onboardingStage == OnboardingStage.COMPLETE;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}