package com.jobportal.v1.dto.agency.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgencyRecentApplication {
    private Long id;
    private String jobTitle;
    private String candidateName;
    private String status;
    private String appliedAt;
}
