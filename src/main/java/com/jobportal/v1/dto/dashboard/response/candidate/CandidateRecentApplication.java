package com.jobportal.v1.dto.dashboard.response.candidate;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CandidateRecentApplication {
    private Long id;
    private String jobTitle;
    private String country;
    private String city;
    private Double salaryAmount;
    private String salaryCurrency;
    private String status;
    private String appliedAt;
}