package com.jobportal.v1.dto.jobApplicationReport.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class JobApplicationResponse {
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
}