package com.jobportal.v1.service.impl;

import com.jobportal.v1.dto.JobDemand.request.JobAgencyAssignmentRequest;
import com.jobportal.v1.dto.JobDemand.response.JobAgencyAssignmentResponse;
import com.jobportal.v1.dto.agency.response.AgencyJobDetailResponse;
import com.jobportal.v1.dto.agency.response.AgencyJobResponse;
import com.jobportal.v1.dto.country.response.CountryResponse;
import com.jobportal.v1.entity.AgencyProfile;
import com.jobportal.v1.entity.JobAgencyAssignment;
import com.jobportal.v1.entity.JobDemand;
import com.jobportal.v1.entity.User;
import com.jobportal.v1.entity.master.Country;
import com.jobportal.v1.enums.ApprovalStatus;
import com.jobportal.v1.enums.JobStatus;
import com.jobportal.v1.exception.BadRequestException;
import com.jobportal.v1.exception.ResourceNotFoundException;
import com.jobportal.v1.repository.AgencyProfileRepository;
import com.jobportal.v1.repository.JobAgencyAssignmentRepository;
import com.jobportal.v1.repository.JobDemandRepository;
import com.jobportal.v1.repository.UserRepository;
import com.jobportal.v1.service.JobAgencyAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobAgencyAssignmentServiceImpl implements JobAgencyAssignmentService {

    private final JobAgencyAssignmentRepository assignmentRepository;
    private final JobDemandRepository jobDemandRepository;
    private final UserRepository userRepository;
    private final AgencyProfileRepository agencyProfileRepository;  // Add this

    @Override
    @Transactional
    public List<JobAgencyAssignmentResponse> assignAgenciesToJob(JobAgencyAssignmentRequest request, Long adminId) {
        // Validate Job Demand exists and is active
        JobDemand jobDemand = jobDemandRepository.findById(request.getJobDemandId())
                .orElseThrow(() -> new ResourceNotFoundException("Job demand not found"));

        if (!jobDemand.getIsActive()) {
            throw new BadRequestException("Cannot assign agencies to an inactive job demand");
        }

        if (jobDemand.getStatus() == JobStatus.COMPLETED) {
            throw new BadRequestException("Cannot assign agencies to a completed job demand");
        }

        if (jobDemand.getStatus() == JobStatus.CLOSED) {
            throw new BadRequestException("Cannot assign agencies to a closed job demand");
        }

        List<JobAgencyAssignmentResponse> responses = new ArrayList<>();
        List<Long> invalidAgencies = new ArrayList<>();
        List<Long> unapprovedAgencies = new ArrayList<>();
        List<Long> nonAgencyUsers = new ArrayList<>();
        List<Long> incompleteProfileAgencies = new ArrayList<>();  // New

        for (Long agencyId : request.getAgencyIds()) {
            User agency = userRepository.findById(agencyId)
                    .orElse(null);

            if (agency == null) {
                invalidAgencies.add(agencyId);
                continue;
            }

            // Validate user is an agency
            if (!agency.isAgency()) {
                nonAgencyUsers.add(agencyId);
                continue;
            }

            // Validate agency is approved
            if (agency.getApprovalStatus() != ApprovalStatus.APPROVED) {
                unapprovedAgencies.add(agencyId);
                continue;
            }

            // Validate agency is active
            if (!agency.isActive()) {
                unapprovedAgencies.add(agencyId);
                continue;
            }

            // Validate agency email is verified
            if (!agency.isEmailVerified()) {
                unapprovedAgencies.add(agencyId);
                continue;
            }

            // NEW: Validate agency has completed profile
            AgencyProfile profile = agencyProfileRepository.findByUserId(agencyId).orElse(null);
            if (profile == null || !profile.isProfileComplete()) {
                incompleteProfileAgencies.add(agencyId);
                continue;
            }

            // Check if assignment already exists
            java.util.Optional<JobAgencyAssignment> existing = assignmentRepository
                    .findByJobDemandIdAndAgencyId(jobDemand.getId(), agencyId);

            JobAgencyAssignment assignment;
            if (existing.isPresent()) {
                assignment = existing.get();
                assignment.setIsEnabled(true);
                assignment.setAssignedBy(adminId);
                assignment.setAssignedAt(LocalDateTime.now());
            } else {
                assignment = new JobAgencyAssignment();
                assignment.setJobDemand(jobDemand);
                assignment.setAgency(agency);
                assignment.setIsEnabled(true);
                assignment.setAssignedBy(adminId);
                assignment.setAssignedAt(LocalDateTime.now());
            }

            JobAgencyAssignment saved = assignmentRepository.save(assignment);
            responses.add(mapToResponse(saved));
        }

        // Throw meaningful error messages
        if (!invalidAgencies.isEmpty()) {
            throw new BadRequestException("Agencies not found: " + invalidAgencies);
        }

        if (!nonAgencyUsers.isEmpty()) {
            throw new BadRequestException("Following users are not agencies: " + nonAgencyUsers);
        }

        if (!unapprovedAgencies.isEmpty()) {
            throw new BadRequestException("Following agencies are not approved or verified: " + unapprovedAgencies);
        }

        if (!incompleteProfileAgencies.isEmpty()) {
            throw new BadRequestException("Following agencies have not completed their profile: " + incompleteProfileAgencies +
                    ". Agencies must complete their profile before being assigned to jobs.");
        }

        if (responses.isEmpty()) {
            throw new BadRequestException("No valid agencies found to assign");
        }

        log.info("Agencies {} assigned to job: {} by admin: {}", request.getAgencyIds(), jobDemand.getTitle(), adminId);
        return responses;
    }

    @Override
    @Transactional
    public void removeAgencyFromJob(Long jobDemandId, Long agencyId) {
        JobDemand jobDemand = jobDemandRepository.findById(jobDemandId)
                .orElseThrow(() -> new ResourceNotFoundException("Job demand not found"));

        User agency = userRepository.findById(agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Agency not found"));

        if (!agency.isAgency()) {
            throw new BadRequestException("User is not an agency");
        }

        JobAgencyAssignment assignment = assignmentRepository
                .findByJobDemandIdAndAgencyId(jobDemandId, agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));

        assignmentRepository.delete(assignment);
        log.info("Agency {} removed from job {}", agencyId, jobDemandId);
    }

    @Override
    @Transactional
    public JobAgencyAssignmentResponse toggleAgencyJobAccess(Long jobDemandId, Long agencyId, Boolean enabled) {
        JobDemand jobDemand = jobDemandRepository.findById(jobDemandId)
                .orElseThrow(() -> new ResourceNotFoundException("Job demand not found"));

        User agency = userRepository.findById(agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Agency not found"));

        if (!agency.isAgency()) {
            throw new BadRequestException("User is not an agency");
        }

        if (enabled) {
            if (agency.getApprovalStatus() != ApprovalStatus.APPROVED) {
                throw new BadRequestException("Cannot enable access for unapproved agency");
            }
            if (!agency.isActive()) {
                throw new BadRequestException("Cannot enable access for inactive agency");
            }
            if (!agency.isEmailVerified()) {
                throw new BadRequestException("Cannot enable access for agency with unverified email");
            }

            AgencyProfile profile = agencyProfileRepository.findByUserId(agencyId).orElse(null);
            if (profile == null || !profile.isProfileComplete()) {
                throw new BadRequestException("Agency must complete their profile before enabling job access");
            }
        }

        JobAgencyAssignment assignment = assignmentRepository
                .findByJobDemandIdAndAgencyId(jobDemandId, agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));

        assignment.setIsEnabled(enabled);
        JobAgencyAssignment saved = assignmentRepository.save(assignment);

        log.info("Agency {} access for job {} set to {}", agencyId, jobDemandId, enabled);
        return mapToResponse(saved);
    }

    @Override
    public List<JobAgencyAssignmentResponse> getAgenciesByJob(Long jobDemandId) {
        JobDemand jobDemand = jobDemandRepository.findById(jobDemandId)
                .orElseThrow(() -> new ResourceNotFoundException("Job demand not found"));

        return assignmentRepository.findByJobDemandId(jobDemandId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AgencyJobDetailResponse> getJobsByAgency(Long agencyId) {
        User agency = userRepository.findById(agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Agency not found"));

        if (!agency.isAgency()) {
            throw new BadRequestException("User is not an agency");
        }

        return assignmentRepository.findByAgencyId(agencyId).stream()
                .map(assignment -> {
                    JobDemand job = assignment.getJobDemand();
                    return AgencyJobDetailResponse.builder()
                            .assignmentId(assignment.getId())
                            .isAccessEnabled(assignment.getIsEnabled())
                            .assignedAt(assignment.getAssignedAt() != null ?
                                    assignment.getAssignedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null)
                            .assignedBy(assignment.getAssignedBy())
                            .jobId(job.getId())
                            .jobTitle(job.getTitle())
                            .country(job.getCountry() != null ? job.getCountry().getName() : null)
                            .city(job.getCity())
                            .totalSlots(job.getTotalSlots())
                            .filledSlots(job.getFilledSlots())
                            .remainingSlots(job.getRemainingSlots())
                            .salaryAmount(job.getSalaryAmount())
                            .salaryCurrency(job.getSalaryCurrency())
                            .status(job.getStatus().name())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public Page<AgencyJobResponse> getMyAssignedJobs(Long agencyId, Pageable pageable) {
        User agency = userRepository.findById(agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Agency not found"));

        if (!agency.isAgency()) {
            throw new BadRequestException("User is not an agency");
        }

        if (agency.getApprovalStatus() != ApprovalStatus.APPROVED) {
            throw new BadRequestException("Only approved agencies can access assigned jobs");
        }

        if (!agency.isActive()) {
            throw new BadRequestException("Inactive agency cannot access assigned jobs");
        }

        AgencyProfile profile = agencyProfileRepository.findByUserId(agencyId).orElse(null);
        if (profile == null || !profile.isProfileComplete()) {
            throw new BadRequestException("Please complete your agency profile before applying to jobs");
        }

        // Get paginated assignments
        Page<JobAgencyAssignment> assignments = assignmentRepository.findByAgencyIdAndIsEnabledTrue(agencyId, pageable);

        return assignments.map(assignment -> {
            JobDemand job = assignment.getJobDemand();
            return AgencyJobResponse.builder()
                    .id(job.getId())
                    .title(job.getTitle())
                    .country(mapToCountryResponse(job.getCountry()))
                    .city(job.getCity())
                    .description(job.getDescription())
                    .requirements(job.getRequirements())
                    .totalSlots(job.getTotalSlots())
                    .filledSlots(job.getFilledSlots())
                    .remainingSlots(job.getRemainingSlots())
                    .appliedCount(job.getAppliedCount())
                    .status(job.getStatus())
                    .isOpen(job.isOpen())
                    .salaryAmount(job.getSalaryAmount())
                    .salaryCurrency(job.getSalaryCurrency())
                    .salaryPeriod(job.getSalaryPeriod())
                    .genderPreference(job.getGenderPreference())
                    .preferredNationalities(job.getPreferredNationalities())
                    .minExperienceYears(job.getMinExperienceYears())
                    .maxExperienceYears(job.getMaxExperienceYears())
                    .requiredSkills(job.getRequiredSkills())
                    .educationLevel(job.getEducationLevel())
                    .workingHoursPerWeek(job.getWorkingHoursPerWeek())
                    .contractDurationYears(job.getContractDurationYears())
                    .overtimePolicy(job.getOvertimePolicy())
                    .accommodationProvided(job.getAccommodationProvided())
                    .accommodationDetails(job.getAccommodationDetails())
                    .foodProvided(job.getFoodProvided())
                    .foodDetails(job.getFoodDetails())
                    .transportationProvided(job.getTransportationProvided())
                    .transportationDetails(job.getTransportationDetails())
                    .medicalInsuranceProvided(job.getMedicalInsuranceProvided())
                    .medicalInsuranceDetails(job.getMedicalInsuranceDetails())
                    .airTicketProvided(job.getAirTicketProvided())
                    .airTicketDetails(job.getAirTicketDetails())
                    .leavePolicy(job.getLeavePolicy())
                    .probationPeriodMonths(job.getProbationPeriodMonths())
                    .terminationClause(job.getTerminationClause())
                    .additionalBenefits(job.getAdditionalBenefits())
                    .deadline(job.getDeadline())
                    .createdAt(job.getCreatedAt())
                    .updatedAt(job.getUpdatedAt())
                    .isAssigned(true)
                    .build();
        });
    }

    private CountryResponse mapToCountryResponse(Country country) {
        if (country == null) return null;
        return CountryResponse.builder()
                .id(country.getId())
                .name(country.getName())
                .code(country.getCode())
                .currencyCode(country.getCurrencyCode())
                .currencySymbol(country.getCurrencySymbol())
                .isEnabled(country.getIsEnabled())
                .build();
    }

    @Override
    public boolean canAgencyApplyToJob(Long agencyId, Long jobDemandId) {
        User agency = userRepository.findById(agencyId).orElse(null);
        if (agency == null || !agency.isAgency() || agency.getApprovalStatus() != ApprovalStatus.APPROVED) {
            return false;
        }

        // NEW: Check profile completion
        AgencyProfile profile = agencyProfileRepository.findByUserId(agencyId).orElse(null);
        if (profile == null || !profile.isProfileComplete()) {
            return false;
        }

        JobDemand job = jobDemandRepository.findById(jobDemandId).orElse(null);
        if (job == null || !job.isOpen()) {
            return false;
        }

        return assignmentRepository.existsByJobDemandIdAndAgencyIdAndIsEnabledTrue(jobDemandId, agencyId);
    }

    private JobAgencyAssignmentResponse mapToResponse(JobAgencyAssignment assignment) {
        return JobAgencyAssignmentResponse.builder()
                .id(assignment.getId())
                .jobDemandId(assignment.getJobDemand().getId())
                .jobTitle(assignment.getJobDemand().getTitle())
                .agencyId(assignment.getAgency().getId())
                .agencyName(assignment.getAgency().getFullName())
                .agencyEmail(assignment.getAgency().getEmail())
                .isEnabled(assignment.getIsEnabled())
                .assignedBy(assignment.getAssignedBy())
                .assignedAt(assignment.getAssignedAt() != null ?
                        assignment.getAssignedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null)
                .build();
    }
}