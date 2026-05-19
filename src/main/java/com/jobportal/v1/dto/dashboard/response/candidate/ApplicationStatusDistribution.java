package com.jobportal.v1.dto.dashboard.response.candidate;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApplicationStatusDistribution {
    private Long pending;
    private Long reviewed;
    private Long shortlisted;
    private Long rejected;
    private Long withdrawn;
}