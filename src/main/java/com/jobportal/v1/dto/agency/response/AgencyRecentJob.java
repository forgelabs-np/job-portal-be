package com.jobportal.v1.dto.agency.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgencyRecentJob {
    private Long id;
    private String title;
    private String country;
    private Integer totalSlots;
    private Integer remainingSlots;
    private String status;
}
