package com.jobportal.v1.dto.agency.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AgencyProfileResponse {
    private Long id;
    private Long userId;
    private String companyName;
    private String companyDescription;
    private String companyWebsite;
    private String companyLogoUrl;
    private String companyAddress;
    private String companyPhone;
    private String registrationNumber;
    private String taxId;
    private String contactPersonName;
    private String contactPersonEmail;
    private String contactPersonPhone;
    private boolean profileComplete;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}