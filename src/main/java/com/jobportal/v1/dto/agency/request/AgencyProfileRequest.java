package com.jobportal.v1.dto.agency.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class AgencyProfileRequest {
    // Basic Information
    @NotBlank(message = "Company name is required")
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
    @NotBlank(message = "Contact person name is required")
    private String contactPersonName;

    @NotBlank(message = "Contact person email is required")
    @Email(message = "Invalid email format")
    private String contactPersonEmail;

    private String contactPersonPhone;
}