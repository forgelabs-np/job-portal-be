package com.jobportal.v1.dto.candidate.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DocumentResponse {
    private Long id;
    private String documentType;
    private String documentName;
    private String documentLink;
    private String notes;
    private String uploadedAt;
}