package com.jobportal.v1.dto.candidate.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DocumentVerificationStats {
    private Long totalDocuments;
    private Long pending;
    private Long approved;
    private Long rejected;
    private Double approvalRate;
}