package com.jobportal.v1.dto.agency.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AgencyDocumentResponse {
    private Long id;
    private String documentType;
    private String documentName;
    private String documentPath;
    private Long fileSize;
    private String contentType;
    private String status;
    private String rejectionReason;
    private String uploadedAt;
}