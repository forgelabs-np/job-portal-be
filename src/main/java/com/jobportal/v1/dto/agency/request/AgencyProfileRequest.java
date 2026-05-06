package com.jobportal.v1.dto.agency.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AgencyProfileRequest {
    @NotBlank(message = "Company name is required")
    private String companyName;

    private String companyDescription;
    private String companyWebsite;
    private String companyLogoUrl;
    private String companyAddress;

    private String companyPhone;

    private String registrationNumber;
    private String taxId;

    @NotBlank(message = "Contact person name is required")
    private String contactPersonName;

    @NotBlank(message = "Contact person email is required")
    @Email(message = "Invalid email format")
    private String contactPersonEmail;

    private String contactPersonPhone;
}