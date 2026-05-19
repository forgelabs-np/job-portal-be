package com.jobportal.v1.dto.agency.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class AgencyJobApplicationResponse {
    private Long id;
    private Long jobDemandId;
    private String jobTitle;
    private String country;
    private String city;
    private Double salaryAmount;
    private String salaryCurrency;
    private Long candidateId;
    private String candidateName;
    private String candidateTrade;
    private String notes;
    private String status;
    private String appliedAt;
    private String rejectionReason;
    private Long reviewedBy;
    private String reviewedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // Agency specific fields
    private Long agencyId;
    private String agencyName;

    // Document information for candidate
    private List<DocumentInfo> documents;
    private Map<String, String> documentStatuses;
    private Boolean allDocumentsApproved;
    private Boolean isProfileComplete;
    private Boolean isEnabled;

    // Candidate personal info for agency view
    private String candidateEmail;
    private String candidatePhone;
    private String candidatePassportNumber;
    private Integer candidateAge;
    private String candidateMaritalStatus;

    @Data
    @Builder
    public static class DocumentInfo {
        private Long id;
        private String documentType;
        private String documentName;
        private String documentPath;
        private String status;
        private String rejectionReason;
        private String uploadedAt;
    }
}