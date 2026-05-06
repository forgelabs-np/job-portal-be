package com.jobportal.v1.dto.dashboard.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecentJobDemand {
    private Long id;
    private String title;
    private String country;
    private Integer totalSlots;
    private Integer filledSlots;
    private Integer remainingSlots;
    private Integer appliedCount;
    private String status;
    private String createdAt;
}