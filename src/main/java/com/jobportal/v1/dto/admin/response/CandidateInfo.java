package com.jobportal.v1.dto.admin.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CandidateInfo {
    private Long id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String trade;
    private Boolean isEnabled;
    private String pccStatus;
    private String slcStatus;
    private String workPermitStatus;
    private String visaStatus;
    private String createdAt;
}