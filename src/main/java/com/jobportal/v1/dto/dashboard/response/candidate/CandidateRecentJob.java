package com.jobportal.v1.dto.dashboard.response.candidate;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CandidateRecentJob {
    private Long id;
    private String title;
    private String country;
    private String city;
    private Double salaryAmount;
    private String salaryCurrency;
    private Integer totalSlots;
    private Integer remainingSlots;
    private String deadline;
}