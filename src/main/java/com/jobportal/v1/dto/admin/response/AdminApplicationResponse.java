package com.jobportal.v1.dto.admin.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminApplicationResponse {
    private Long id;

    // Job details
    private Long jobDemandId;
    private String jobTitle;
    private String jobCountry;
    private String jobCity;

    // Agency details
    private Long agencyId;
    private String agencyName;
    private String agencyEmail;

    // Candidate details
    private Long candidateId;
    private String candidateName;
    private String candidateTrade;
    private String candidatePassportNumber;
    private String candidateType;

    // Application details
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
}