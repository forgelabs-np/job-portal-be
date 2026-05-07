package com.jobportal.v1.dto.candidate.request;

import lombok.Data;

@Data
public class DocumentRequest {
    private String documentType;
    private String documentName;
    private String documentLink;
    private String notes;
}