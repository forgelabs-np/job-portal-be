package com.jobportal.v1.dto.agency.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgencyJobResponse {
    private Long id;
    private String title;
    private String country;
    private String city;
    private Integer totalSlots;
    private Integer filledSlots;
    private Integer remainingSlots;
    private Double salaryAmount;
    private String salaryCurrency;
    private String deadline;
    private Boolean isAssigned;
}