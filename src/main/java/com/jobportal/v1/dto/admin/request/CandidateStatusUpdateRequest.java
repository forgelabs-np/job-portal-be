package com.jobportal.v1.dto.admin.request;

import com.jobportal.v1.enums.DocumentStatus;
import lombok.Data;

@Data
public class CandidateStatusUpdateRequest {
    private DocumentStatus pccStatus;
    private DocumentStatus slcStatus;
    private DocumentStatus workPermitStatus;
    private DocumentStatus visaStatus;
}