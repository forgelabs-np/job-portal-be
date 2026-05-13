package com.jobportal.v1.dto.security;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.jobportal.v1.dto.agency.response.AgencyDocumentResponse;
import com.jobportal.v1.dto.candidate.response.StatusResponse;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@Builder
public class CurrentUserResponse {

    // Common user info
    private Long id;
    private String email;
    private String fullName;
    private String phoneNumber;
    private Set<String> roles;
    private boolean emailVerified;
    private boolean isActive;

    // Admin specific
    private AdminProfile adminProfile;

    // Agency specific
    private AgencyProfileResponse agencyProfile;
    private boolean isProfileComplete;
    private String profileApprovalStatus;
    private String profileRejectionReason;
    private List<AgencyDocumentResponse> documents;

    // Candidate specific
    private CandidateProfileResponse candidateProfile;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginTime;

    // Inner classes
    @Data
    @Builder
    public static class AdminProfile {
        private String role;
        private String permissions;
    }

    @Data
    @Builder
    public static class AgencyProfileResponse {
        private Long profileId;
        private String companyName;
        private String companyDescription;
        private String companyWebsite;
        private String companyLogoUrl;
        private String companyAddress;
        private String companyPhone;
        private String registrationNumber;
        private String taxId;
        private String contactPersonName;
        private String contactPersonEmail;
        private String contactPersonPhone;
        private boolean profileComplete;
        private String profileApprovalStatus;
        private String profileRejectionReason;
    }

    @Data
    @Builder
    public static class CandidateProfileResponse {
        private Long candidateId;
        private String firstName;
        private String lastName;
        private String fullName;
        private String trade;
        private String dateOfBirth;
        private Integer age;
        private String maritalStatus;
        private String passportNumber;
        private String passportIssueDate;
        private String passportExpiryDate;
        private Boolean isPassportValid;
        private String documentsFolderLink;
        private String introVideoLink;
        private Boolean isEnabled;
        private String candidateType;
        private String createdByType;
        private StatusResponse statuses;
    }
}
