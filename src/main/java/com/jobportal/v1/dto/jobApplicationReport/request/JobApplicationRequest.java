package com.jobportal.v1.dto.jobApplicationReport.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JobApplicationRequest {
    @NotNull(message = "Job demand ID is required")
    private Long jobDemandId;

    @NotNull(message = "Candidate ID is required")
    private Long candidateId;

    private String notes;
}