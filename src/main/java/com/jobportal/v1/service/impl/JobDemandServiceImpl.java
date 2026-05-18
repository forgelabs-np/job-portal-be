package com.jobportal.v1.service.impl;

import com.jobportal.v1.dto.JobDemand.request.JobDemandRequest;
import com.jobportal.v1.dto.JobDemand.response.JobDemandResponse;
import com.jobportal.v1.dto.country.response.CountryResponse;
import com.jobportal.v1.entity.JobDemand;
import com.jobportal.v1.entity.master.Country;
import com.jobportal.v1.enums.JobStatus;
import com.jobportal.v1.exception.BadRequestException;
import com.jobportal.v1.exception.ResourceNotFoundException;
import com.jobportal.v1.repository.CountryRepository;
import com.jobportal.v1.repository.JobDemandRepository;
import com.jobportal.v1.service.JobDemandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobDemandServiceImpl implements JobDemandService {

    private final JobDemandRepository jobDemandRepository;
    private final CountryRepository countryRepository;

    @Override
    @Transactional
    public JobDemandResponse createOrUpdateJobDemand(JobDemandRequest request, Long adminId) {
        Country country = countryRepository.findById(request.getCountryId())
                .orElseThrow(() -> new ResourceNotFoundException("Country not found"));

        if (!country.getIsEnabled()) {
            throw new BadRequestException("Country is not enabled for job posting");
        }

        JobDemand jobDemand;
        boolean isUpdate = false;

        if (request.getId() != null && request.getId() > 0) {
            jobDemand = jobDemandRepository.findById(request.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Job demand not found with id: " + request.getId()));
            isUpdate = true;
            request.setFilledSlots(jobDemand.getFilledSlots());
            log.info("Updating job demand: {} by admin: {}", request.getTitle(), adminId);
        } else {
            jobDemand = new JobDemand();
            jobDemand.setCreatedBy(adminId);
            jobDemand.setFilledSlots(0);
            jobDemand.setAppliedCount(0);
            jobDemand.setStatus(JobStatus.OPEN);
            jobDemand.setIsActive(true);
            log.info("Creating new job demand: {} by admin: {}", request.getTitle(), adminId);
        }

        mapRequestToEntity(request, jobDemand, country);

        JobDemand saved = jobDemandRepository.save(jobDemand);

        String message = isUpdate ? "Job demand updated successfully" : "Job demand created successfully";
        log.info("Job demand {}: {} by admin: {}", isUpdate ? "updated" : "created", saved.getTitle(), adminId);

        return mapToResponse(saved);
    }

    @Override
    public JobDemandResponse getJobDemandById(Long id) {
        JobDemand jobDemand = jobDemandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job demand not found"));
        return mapToResponse(jobDemand);
    }

    @Override
    public Page<JobDemandResponse> getAllJobDemands(Pageable pageable) {
        return jobDemandRepository.findByIsActiveTrue(pageable)
                .map(this::mapToResponse);
    }

    @Override
    public Page<JobDemandResponse> getJobDemandsByStatus(String status, Pageable pageable) {
        JobStatus jobStatus;
        try {
            jobStatus = JobStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status. Allowed values: OPEN, CLOSED, COMPLETED, CANCELLED");
        }
        return jobDemandRepository.findByStatusAndIsActiveTrue(jobStatus, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public Page<JobDemandResponse> getJobDemandsByAdmin(Long adminId, Pageable pageable) {
        return jobDemandRepository.findByCreatedByAndIsActiveTrue(adminId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public List<JobDemandResponse> getOpenJobDemands() {
        return jobDemandRepository.findAllOpenAndActive().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public JobDemandResponse closeJobDemand(Long id, Long adminId) {
        JobDemand jobDemand = jobDemandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job demand not found"));

        if (jobDemand.getStatus() == JobStatus.COMPLETED) {
            throw new BadRequestException("Cannot close a completed job demand");
        }
        if (jobDemand.getStatus() == JobStatus.CLOSED) {
            throw new BadRequestException("Job demand is already closed");
        }

        jobDemand.setStatus(JobStatus.CLOSED);
        JobDemand saved = jobDemandRepository.save(jobDemand);
        log.info("Job demand closed: {} by admin: {}", saved.getTitle(), adminId);

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteJobDemand(Long id, Long adminId) {
        JobDemand jobDemand = jobDemandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job demand not found"));

        jobDemand.setIsActive(false);
        jobDemandRepository.save(jobDemand);
        log.info("Job demand deleted: {} by admin: {}", jobDemand.getTitle(), adminId);
    }

    private void mapRequestToEntity(JobDemandRequest request, JobDemand entity, Country country) {
        entity.setTitle(request.getTitle());
        entity.setCountry(country);
        entity.setCity(request.getCity());
        entity.setDescription(request.getDescription());
        entity.setRequirements(request.getRequirements());
        entity.setTotalSlots(request.getTotalSlots());
        entity.setSalaryAmount(request.getSalaryAmount());
        entity.setSalaryCurrency(country.getCurrencyCode()); // Currency from Country entity
        entity.setGenderPreference(request.getGenderPreference());
        entity.setPreferredNationalities(request.getPreferredNationalities());
        entity.setWorkingHoursPerWeek(request.getWorkingHoursPerWeek());
        entity.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : false);

        if (request.getSalaryPeriod() != null) {
            entity.setSalaryPeriod(request.getSalaryPeriod());
        }

        entity.setMinExperienceYears(request.getMinExperienceYears());
        entity.setMaxExperienceYears(request.getMaxExperienceYears());
        entity.setRequiredSkills(request.getRequiredSkills());
        entity.setEducationLevel(request.getEducationLevel());
        entity.setContractDurationYears(request.getContractDurationYears());
        entity.setOvertimePolicy(request.getOvertimePolicy());

        entity.setAccommodationProvided(request.getAccommodationProvided() != null ? request.getAccommodationProvided() : false);
        entity.setAccommodationDetails(request.getAccommodationDetails());
        entity.setFoodProvided(request.getFoodProvided() != null ? request.getFoodProvided() : false);
        entity.setFoodDetails(request.getFoodDetails());
        entity.setTransportationProvided(request.getTransportationProvided() != null ? request.getTransportationProvided() : false);
        entity.setTransportationDetails(request.getTransportationDetails());
        entity.setMedicalInsuranceProvided(request.getMedicalInsuranceProvided() != null ? request.getMedicalInsuranceProvided() : false);
        entity.setMedicalInsuranceDetails(request.getMedicalInsuranceDetails());
        entity.setAirTicketProvided(request.getAirTicketProvided() != null ? request.getAirTicketProvided() : false);
        entity.setAirTicketDetails(request.getAirTicketDetails());
        entity.setLeavePolicy(request.getLeavePolicy());
        entity.setProbationPeriodMonths(request.getProbationPeriodMonths());
        entity.setTerminationClause(request.getTerminationClause());
        entity.setAdditionalBenefits(request.getAdditionalBenefits());
        entity.setDeadline(request.getDeadline());

        entity.calculateRemainingSlots();
    }

    private JobDemandResponse mapToResponse(JobDemand entity) {
        return JobDemandResponse.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .country(mapToCountryResponse(entity.getCountry()))
                .city(entity.getCity())
                .description(entity.getDescription())
                .requirements(entity.getRequirements())
                .totalSlots(entity.getTotalSlots())
                .filledSlots(entity.getFilledSlots())
                .remainingSlots(entity.getRemainingSlots())
                .appliedCount(entity.getAppliedCount())
                .status(entity.getStatus())
                .isOpen(entity.isOpen())
                .salaryAmount(entity.getSalaryAmount())
                .salaryCurrency(entity.getSalaryCurrency())
                .salaryPeriod(entity.getSalaryPeriod())
                .genderPreference(entity.getGenderPreference())
                .preferredNationalities(entity.getPreferredNationalities())
                .minExperienceYears(entity.getMinExperienceYears())
                .maxExperienceYears(entity.getMaxExperienceYears())
                .requiredSkills(entity.getRequiredSkills())
                .educationLevel(entity.getEducationLevel())
                .workingHoursPerWeek(entity.getWorkingHoursPerWeek())
                .contractDurationYears(entity.getContractDurationYears())
                .overtimePolicy(entity.getOvertimePolicy())
                .accommodationProvided(entity.getAccommodationProvided())
                .accommodationDetails(entity.getAccommodationDetails())
                .foodProvided(entity.getFoodProvided())
                .foodDetails(entity.getFoodDetails())
                .transportationProvided(entity.getTransportationProvided())
                .transportationDetails(entity.getTransportationDetails())
                .medicalInsuranceProvided(entity.getMedicalInsuranceProvided())
                .medicalInsuranceDetails(entity.getMedicalInsuranceDetails())
                .airTicketProvided(entity.getAirTicketProvided())
                .airTicketDetails(entity.getAirTicketDetails())
                .leavePolicy(entity.getLeavePolicy())
                .probationPeriodMonths(entity.getProbationPeriodMonths())
                .terminationClause(entity.getTerminationClause())
                .additionalBenefits(entity.getAdditionalBenefits())
                .deadline(entity.getDeadline())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .createdBy(entity.getCreatedBy())
                .isPublic(entity.getIsPublic())
                .build();
    }

    private CountryResponse mapToCountryResponse(Country country) {
        return CountryResponse.builder()
                .id(country.getId())
                .name(country.getName())
                .code(country.getCode())
                .currencyCode(country.getCurrencyCode())
                .currencySymbol(country.getCurrencySymbol())
                .isEnabled(country.getIsEnabled())
                .build();
    }
}