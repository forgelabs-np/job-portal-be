package com.jobportal.v1.dto.agency.request;

import com.jobportal.v1.enums.ApprovalStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProfileApprovalRequest {
    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Status is required")
    private ApprovalStatus status;

    private String rejectionReason;
}