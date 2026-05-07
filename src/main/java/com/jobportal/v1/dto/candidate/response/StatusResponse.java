package com.jobportal.v1.dto.candidate.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StatusResponse {
    private String pccStatus;
    private String slcStatus;
    private String workPermitStatus;
    private String visaStatus;
}
