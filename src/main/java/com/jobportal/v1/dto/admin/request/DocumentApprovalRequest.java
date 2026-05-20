package com.jobportal.v1.dto.admin.request;

import com.jobportal.v1.enums.ApprovalStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DocumentApprovalRequest {
    @NotNull(message = "Document ID is required")
    private Long documentId;

    @NotNull(message = "CandidateID is required")
    private Long candidateId;

    @NotNull(message = "Status is required")
    private ApprovalStatus status;

    private String rejectionReason;
}