package com.jobportal.v1.dto.agency.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgencyRecentCandidate {
    private Long id;
    private String fullName;
    private String trade;
    private Boolean isEnabled;
    private String createdAt;
}
