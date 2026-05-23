package com.jobportal.v1.mapper;

import com.jobportal.v1.dto.country.response.CountryResponse;
import com.jobportal.v1.dto.jobDemand.response.JobDemandResponse;
import com.jobportal.v1.entity.JobDemand;
import org.springframework.stereotype.Component;

@Component
public class JobDemandMapper {


// Convert JobDemand entity to JobDemandResponse DTO
    public static JobDemandResponse toResponse(JobDemand job) {
        if (job == null) {
            return null;
        }

        CountryResponse countryResponse = null;
        if (job.getCountry() != null) {
            countryResponse = CountryResponse.builder()
                    .id(job.getCountry().getId())
                    .name(job.getCountry().getName())
                    .code(job.getCountry().getCode())
                    .build();
        }

        return JobDemandResponse.builder()
                .id(job.getId())
                .title(job.getTitle())
                .country(countryResponse)
                .city(job.getCity())
                .description(job.getDescription())
                .requirements(job.getRequirements())
                .totalSlots(job.getTotalSlots())
                .remainingSlots(job.getRemainingSlots())
                .salaryAmount(job.getSalaryAmount())
                .salaryCurrency(job.getSalaryCurrency())
                .salaryPeriod(job.getSalaryPeriod())
                .genderPreference(job.getGenderPreference())
                .preferredNationalities(job.getPreferredNationalities())
                .minExperienceYears(job.getMinExperienceYears())
                .maxExperienceYears(job.getMaxExperienceYears())
                .requiredSkills(job.getRequiredSkills())
                .educationLevel(job.getEducationLevel())
                .contractDurationYears(job.getContractDurationYears())
                .workingHoursPerWeek(job.getWorkingHoursPerWeek())
                .overtimePolicy(job.getOvertimePolicy())
                .accommodationProvided(job.getAccommodationProvided())
                .foodProvided(job.getFoodProvided())
                .transportationProvided(job.getTransportationProvided())
                .medicalInsuranceProvided(job.getMedicalInsuranceProvided())
                .airTicketProvided(job.getAirTicketProvided())
                .leavePolicy(job.getLeavePolicy())
                .probationPeriodMonths(job.getProbationPeriodMonths())
                .terminationClause(job.getTerminationClause())
                .additionalBenefits(job.getAdditionalBenefits())
                .deadline(job.getDeadline())
                .status(job.getStatus())
                .isPublic(job.getIsPublic())
                .build();
    }

    /**
     * Convert JobDemand entity to JobDemandResponse DTO (instance method version)
     */
    public JobDemandResponse toResponseInstance(JobDemand job) {
        return toResponse(job);
    }
}