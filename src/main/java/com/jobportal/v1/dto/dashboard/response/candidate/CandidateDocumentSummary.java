package com.jobportal.v1.dto.dashboard.response.candidate;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CandidateDocumentSummary {
    private String documentType;
    private String documentName;
    private String status;
    private String uploadedAt;
}