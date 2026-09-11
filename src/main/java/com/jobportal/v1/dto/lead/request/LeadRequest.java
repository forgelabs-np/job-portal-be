package com.jobportal.v1.dto.lead.request;

import com.jobportal.v1.enums.LeadSubject;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
@Schema(description = "Lead submission request")
public class LeadRequest {

    @Schema(description = "Full name of the lead", example = "John Doe")
    @NotBlank(message = "Full name is required")
    @Size(max = 150, message = "Full name must not exceed 150 characters")
    private String fullName;

    @Schema(description = "10-digit phone number", example = "9841234567")
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\d{10}$", message = "Phone number must be exactly 10 digits")
    private String phoneNumber;

    @Schema(description = "Email address", example = "john@example.com")
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Schema(description = "Location or address", example = "Kathmandu, Nepal")
    @Size(max = 255, message = "Location must not exceed 255 characters")
    private String location;

    @Schema(description = "Subject of the inquiry", example = "CONSULTATION")
    @NotNull(message = "Subject is required")
    private LeadSubject subject;

    @Schema(description = "Description or message", example = "I would like to schedule a consultation regarding job posting services.")
    @NotBlank(message = "Description is required")
    @Size(max = 300, message = "Description must not exceed 300 characters")
    private String description;
}
