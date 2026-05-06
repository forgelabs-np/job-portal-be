package com.jobportal.v1.dto.dashboard.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecentCandidate {
    private Long id;
    private String candidateName;
    private String jobTitle;
    private String agencyName;
    private String status;
    private String submittedAt;
}
