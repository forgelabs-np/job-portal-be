package com.jobportal.v1.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Approval status for documents and profiles")
public enum ApprovalStatus {
    PENDING,
    APPROVED,
    REJECTED
}