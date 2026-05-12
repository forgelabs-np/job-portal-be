package com.jobportal.v1.dto.agency.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class AgencyProfileResponse {
    private Long id;
    private Long userId;

    // Basic Information
    private String companyName;
    private String companyDescription;
    private String companyWebsite;
    private String companyLogoUrl;
    private String companyAddress;
    private String companyPhone;

    // Registration Information
    private String registrationNumber;
    private String taxId;

    // Contact Person
    private String contactPersonName;
    private String contactPersonEmail;
    private String contactPersonPhone;

    // Profile Status
    private boolean profileComplete;
    private String profileApprovalStatus;
    private String profileRejectionReason;

    // Documents
    private List<AgencyDocumentResponse> documents;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}