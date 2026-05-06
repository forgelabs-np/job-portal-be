package com.jobportal.v1.dto.agency.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AgencyActionRequest {
    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Action is required")
    private String action; // APPROVE or REJECT

    private String rejectionReason; // Required only if action is REJECT
}