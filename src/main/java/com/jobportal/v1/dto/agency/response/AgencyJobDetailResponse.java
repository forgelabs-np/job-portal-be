package com.jobportal.v1.dto.agency.response;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgencyJobDetailResponse {
    // Assignment info
    private Long assignmentId;
    private Boolean isAccessEnabled;
    private String assignedAt;
    private Long assignedBy;

    // Job details
    private Long jobId;
    private String jobTitle;
    private String country;
    private String city;
    private Integer totalSlots;
    private Integer filledSlots;
    private Integer remainingSlots;
    private Double salaryAmount;
    private String salaryCurrency;
    private String status;
}