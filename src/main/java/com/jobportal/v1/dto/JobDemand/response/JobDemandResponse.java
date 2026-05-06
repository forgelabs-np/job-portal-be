package com.jobportal.v1.dto.JobDemand.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jobportal.v1.dto.country.response.CountryResponse;
import com.jobportal.v1.enums.GenderPreference;
import com.jobportal.v1.enums.JobStatus;
import com.jobportal.v1.enums.SalaryPeriod;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class JobDemandResponse {
    private Long id;
    private String title;
    private CountryResponse country;
    private String city;
    private String description;
    private String requirements;

    private Integer totalSlots;
    private Integer filledSlots;
    private Integer remainingSlots;
    private Integer appliedCount;

    private JobStatus status;
    private Boolean isOpen;

    private Double salaryAmount;
    private String salaryCurrency;
    private SalaryPeriod salaryPeriod;

    private GenderPreference genderPreference;
    private List<String> preferredNationalities;
    private Integer minExperienceYears;
    private Integer maxExperienceYears;
    private String requiredSkills;
    private String educationLevel;
    private Integer workingHoursPerWeek;

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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    private Long createdBy;
}