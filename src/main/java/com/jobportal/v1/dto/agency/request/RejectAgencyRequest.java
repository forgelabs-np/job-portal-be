package com.jobportal.v1.dto.agency.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RejectAgencyRequest {
    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Rejection reason is required")
    private String rejectionReason;
}