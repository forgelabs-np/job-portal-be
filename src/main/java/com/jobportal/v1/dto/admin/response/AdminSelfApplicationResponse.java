package com.jobportal.v1.dto.admin.response;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class AdminSelfApplicationResponse {
    private Long id;
    private Long jobDemandId;
    private String jobTitle;
    private String jobCountry;
    private String jobCity;
    private Double salaryAmount;
    private String salaryCurrency;

    // Candidate info
    private Long candidateId;
    private String candidateName;
    private String candidateEmail;
    private String candidatePhone;
    private String candidateTrade;
    private String candidatePassportNumber;
    private Integer candidateAge;
    private String candidateMaritalStatus;
    private Boolean isProfileComplete;
    private Boolean isEnabled;

    // Application info
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

    // Document information
    private List<DocumentInfo> documents;
    private Map<String, String> documentStatuses;
    private Boolean allDocumentsApproved;

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