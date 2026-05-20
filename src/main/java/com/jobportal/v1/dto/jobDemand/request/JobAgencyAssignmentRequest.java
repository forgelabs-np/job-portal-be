package com.jobportal.v1.dto.jobDemand.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class JobAgencyAssignmentRequest {

    @NotNull(message = "Job Demand ID is required")
    private Long jobDemandId;

    @NotNull(message = "Agency IDs are required")
    private List<Long> agencyIds;
}