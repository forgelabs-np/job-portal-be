package com.jobportal.v1.dto.JobDemand.request;

import com.jobportal.v1.enums.GenderPreference;
import com.jobportal.v1.enums.SalaryPeriod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class JobDemandRequest {

    private Long id;

    @NotBlank(message = "Job title is required")
    private String title;

    @NotNull(message = "Country ID is required")
    private Long countryId;

    private String city;

    @NotBlank(message = "Description is required")
    private String description;

    @NotBlank(message = "Requirements are required")
    private String requirements;

    @NotNull(message = "Total slots is required")
    @Min(value = 1, message = "Total slots must be at least 1")
    private Integer totalSlots;

    private Integer filledSlots;  // Internal use only

    @NotNull(message = "Salary amount is required")
    @Min(value = 0, message = "Salary amount must be positive")
    private Double salaryAmount;

    private SalaryPeriod salaryPeriod = SalaryPeriod.MONTHLY;

    @NotNull(message = "Gender preference is required")
    private GenderPreference genderPreference;

    @NotNull(message = "Preferred nationalities are required")
    private List<String> preferredNationalities;

    @NotNull(message = "Working hours per week is required")
    @Min(value = 1, message = "Working hours per week must be at least 1")
    private Integer workingHoursPerWeek;

    // Optional fields
    private Integer minExperienceYears;
    private Integer maxExperienceYears;
    private String requiredSkills;
    private String educationLevel;
    private Integer contractDurationYears;
    private String overtimePolicy;
    private Boolean accommodationProvided;
    private String accommodationDetails;
    private Boolean foodProvided;
    private String foodDetails;
    private Boolean transportationProvided;
    private String transportationDetails;
    private Boolean medicalInsuranceProvided;
    private String medicalInsuranceDetails;
    private Boolean airTicketProvided;
    private String airTicketDetails;
    private String leavePolicy;
    private Integer probationPeriodMonths;
    private String terminationClause;
    private String additionalBenefits;
    private LocalDateTime deadline;
}