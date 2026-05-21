package com.jobportal.v1.dto.admin.request;

import com.jobportal.v1.enums.ApprovalStatus;
import lombok.Data;

@Data
public class AgencyDocumentApprovalRequest {
    private Long documentId;
    private Long agencyId;           // Agency ID (or agencyProfileId)
    private ApprovalStatus status;
    private String rejectionReason;
}