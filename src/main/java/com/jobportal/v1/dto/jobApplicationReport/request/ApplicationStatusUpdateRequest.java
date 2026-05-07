package com.jobportal.v1.dto.jobApplicationReport.request;

import com.jobportal.v1.enums.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApplicationStatusUpdateRequest {
    @NotNull(message = "Status is required")
    private ApplicationStatus status;

    private String rejectionReason; // Required if status = REJECTED
}
