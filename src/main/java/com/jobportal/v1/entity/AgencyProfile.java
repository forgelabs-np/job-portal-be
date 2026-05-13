package com.jobportal.v1.entity;

import com.jobportal.v1.enums.ApprovalStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "agency_profiles")
@Data
public class AgencyProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    // Basic Information
    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "company_description", columnDefinition = "TEXT")
    private String companyDescription;

    @Column(name = "company_website")
    private String companyWebsite;

    @Column(name = "company_logo_url")
    private String companyLogoUrl;

    @Column(name = "company_address", columnDefinition = "TEXT")
    private String companyAddress;

    @Column(name = "company_phone", length = 20)
    private String companyPhone;

    // Registration Information
    @Column(name = "registration_number")
    private String registrationNumber;

    @Column(name = "tax_id")
    private String taxId;

    // Contact Person
    @Column(name = "contact_person_name", nullable = false)
    private String contactPersonName;

    @Column(name = "contact_person_email", nullable = false)
    private String contactPersonEmail;

    @Column(name = "contact_person_phone", length = 20)
    private String contactPersonPhone;

    @Column(name = "profile_complete", nullable = false)
    private boolean profileComplete = false;

    @Column(name = "profile_approved_at")
    private LocalDateTime profileApprovedAt;

    @Column(name = "profile_approved_by")
    private Long profileApprovedBy;

    @Column(name = "profile_rejection_reason")
    private String profileRejectionReason;

    @Column(name = "is_active")
    private boolean isActive = true;

    @OneToMany(mappedBy = "agencyProfile", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AgencyDocument> documents = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void approveProfile(Long adminId) {
        this.profileApprovedAt = LocalDateTime.now();
        this.profileApprovedBy = adminId;
        this.profileRejectionReason = null;
        this.profileComplete = true;

        if (this.user != null) {
            this.user.approve(adminId);
        }
    }

    public void rejectProfile(String reason) {
        this.profileRejectionReason = reason;
        this.profileComplete = false;

        if (this.user != null) {
            this.user.reject(reason);
        }
    }

    public boolean isProfileApproved() {
        return this.user != null && this.user.isApproved();
    }
}