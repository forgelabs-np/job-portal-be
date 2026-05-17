package com.jobportal.v1.dto.candidate.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CandidateDocumentResponse {
    private Long id;
    private String documentType;
    private String documentName;
    private String documentPath;
    private String notes;
    private String uploadedAt;
    private String status;
    private String rejectionReason;
}