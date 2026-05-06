package com.jobportal.v1.dto.dashboard.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JobStatusDistribution {
    private Long open;
    private Long completed;
    private Long closed;
    private Long cancelled;
}
