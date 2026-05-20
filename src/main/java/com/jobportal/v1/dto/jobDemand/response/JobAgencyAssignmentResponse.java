package com.jobportal.v1.dto.jobDemand.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JobAgencyAssignmentResponse {
    private Long id;
    private Long jobDemandId;
    private String jobTitle;
    private Long agencyId;
    private String agencyName;
    private String agencyEmail;
    private Boolean isEnabled;
    private Long assignedBy;
    private String assignedAt;
}
