package com.jobportal.v1.dto.agency.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReRequestApprovalRequest {
    @NotNull(message = "User ID is required")
    private Long userId;

    private String additionalMessage;
}