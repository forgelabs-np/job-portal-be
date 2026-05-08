package com.jobportal.v1.dto.agency.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgencyApplicationStatusDistribution {
    private Long pending;
    private Long reviewed;
    private Long shortlisted;
    private Long rejected;
    private Long withdrawn;
}
