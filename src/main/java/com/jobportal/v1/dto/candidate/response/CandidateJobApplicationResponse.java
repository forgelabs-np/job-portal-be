package com.jobportal.v1.dto.candidate.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class CandidateJobApplicationResponse {
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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    // Document information for candidate
    private List<DocumentInfo> documents;
    private Map<String, String> documentStatuses;
    private Boolean allDocumentsApproved;
    private Boolean isProfileComplete;
    private Boolean isEnabled;

    @Data
    @Builder
    public static class DocumentInfo {
        private Long id;
        private String documentType;
        private String documentPath;
        private String documentName;
        private String status;
        private String rejectionReason;
        private String uploadedAt;
    }
}