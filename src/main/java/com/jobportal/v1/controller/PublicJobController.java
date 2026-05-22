package com.jobportal.v1.controller;

import com.jobportal.v1.dto.ApiResponse;
import com.jobportal.v1.dto.jobDemand.response.JobDemandResponse;
import com.jobportal.v1.dto.PageRes;
import com.jobportal.v1.dto.country.response.CountryResponse;
import com.jobportal.v1.entity.JobDemand;
import com.jobportal.v1.enums.JobStatus;
import com.jobportal.v1.repository.JobDemandRepository;
import com.jobportal.v1.util.Pages;
import com.jobportal.v1.util.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public/jobs")
@Tag(name = "Public Jobs", description = "Public Job Listing APIs (No authentication required)")
public class PublicJobController {

    private final JobDemandRepository jobDemandRepository;

    @Operation(summary = "Get All Public Jobs", description = "Get paginated list of all public jobs (isPublic=true and status=OPEN)")
    @GetMapping
    public ResponseEntity<ApiResponse<PageRes<JobDemandResponse>>> getPublicJobs(
            @PageableDefault(size = 20) Pageable pageable) {

        Page<JobDemand> jobs = jobDemandRepository.findByIsPublicTrueAndStatusAndIsActiveTrue(JobStatus.OPEN, pageable);
        Page<JobDemandResponse> response = jobs.map(this::toResponse);
        PageRes<JobDemandResponse> pageRes = Pages.of(response);

        return ResponseUtil.ok("Public jobs retrieved", pageRes);
    }

    @Operation(summary = "Get Public Job by ID", description = "Get a single public job by ID (only if isPublic=true)")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobDemandResponse>> getPublicJobById(@PathVariable Long id) {
        JobDemand job = jobDemandRepository.findByIdAndIsPublicTrueAndIsActiveTrue(id)
                .orElseThrow(() -> new RuntimeException("Job not found or not public"));

        return ResponseUtil.ok("Job retrieved", toResponse(job));
    }

    private JobDemandResponse toResponse(JobDemand job) {
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
}