package com.jobportal.v1.dto.dashboard.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecentAgency {
    private Long id;
    private String fullName;
    private String email;
    private String approvalStatus;
    private String createdAt;
}
