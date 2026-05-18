package com.jobportal.v1.service.impl;

import com.jobportal.v1.dto.agency.response.AgencyDocumentResponse;
import com.jobportal.v1.dto.candidate.response.StatusResponse;
import com.jobportal.v1.dto.security.CurrentUserResponse;
import com.jobportal.v1.entity.*;
import com.jobportal.v1.exception.ResourceNotFoundException;
import com.jobportal.v1.repository.*;
import com.jobportal.v1.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CurrentUserServiceImpl implements CurrentUserService {

    private final UserRepository userRepository;
    private final AgencyProfileRepository agencyProfileRepository;
    private final AgencyDocumentRepository agencyDocumentRepository;
    private final CandidateRepository candidateRepository;
    private final CandidateStatusRepository candidateStatusRepository;

    @Override
    public CurrentUserResponse getCurrentUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        CurrentUserResponse.CurrentUserResponseBuilder responseBuilder = CurrentUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .roles(user.getRoles().stream().map(Enum::name).collect(Collectors.toSet()))
                .emailVerified(user.isEmailVerified())
                .isActive(user.isActive())
                .createdAt(user.getCreatedAt())
                .lastLoginTime(user.getLastLoginTime());

        // Handle based on user role
        if (user.isAdmin()) {
            return responseBuilder
                    .adminProfile(CurrentUserResponse.AdminProfile.builder()
                            .role("ADMIN")
                            .permissions("FULL_ACCESS")
                            .build())
                    .isProfileComplete(true)
                    .onboardingStage("COMPLETED")  // Admin is always onboarded
                    .build();
        }

        if (user.isAgency()) {
            AgencyProfile agencyProfile = agencyProfileRepository.findByUserId(userId)
                    .orElse(null);

            if (agencyProfile != null) {
                List<AgencyDocumentResponse> documents = agencyDocumentRepository
                        .findByAgencyProfileId(agencyProfile.getId()).stream()
                        .map(this::toDocumentResponse)
                        .collect(Collectors.toList());

                responseBuilder
                        .agencyProfile(CurrentUserResponse.AgencyProfileResponse.builder()
                                .profileId(agencyProfile.getId())
                                .companyName(agencyProfile.getCompanyName())
                                .companyDescription(agencyProfile.getCompanyDescription())
                                .companyWebsite(agencyProfile.getCompanyWebsite())
                                .companyLogoUrl(agencyProfile.getCompanyLogoUrl())
                                .companyAddress(agencyProfile.getCompanyAddress())
                                .companyPhone(agencyProfile.getCompanyPhone())
                                .registrationNumber(agencyProfile.getRegistrationNumber())
                                .taxId(agencyProfile.getTaxId())
                                .contactPersonName(agencyProfile.getContactPersonName())
                                .contactPersonEmail(agencyProfile.getContactPersonEmail())
                                .contactPersonPhone(agencyProfile.getContactPersonPhone())
                                .profileComplete(agencyProfile.isProfileComplete())
                                .profileApprovalStatus(user.getApprovalStatus().name())
                                .profileRejectionReason(agencyProfile.getProfileRejectionReason())
                                .build())
                        .isProfileComplete(agencyProfile.isProfileComplete())
                        .profileApprovalStatus(user.getApprovalStatus().name())
                        .profileRejectionReason(agencyProfile.getProfileRejectionReason())
                        .documents(documents)
                        .onboardingStage(agencyProfile.isProfileComplete() ? "COMPLETED" : "PROFILE");
            }

            return responseBuilder.build();
        }

        if (user.isCandidate()) {
            Candidate candidate = candidateRepository.findByUserId(userId)
                    .orElse(null);

            // ✅ ISSUE 4 FIX: No profile created yet — tell frontend to show profile creation
            if (candidate == null) {
                return responseBuilder
                        .isProfileComplete(false)
                        .onboardingStage("PROFILE")  // Frontend knows to show profile form
                        .build();
            }

            // ✅ Calculate profile completeness dynamically
            boolean profileComplete = candidate.calculateProfileComplete();
            if (candidate.isProfileComplete() != profileComplete) {
                candidate.setProfileComplete(profileComplete);
                candidateRepository.save(candidate);
            }

            // Calculate age
            Integer age = null;
            if (candidate.getDateOfBirth() != null) {
                age = Period.between(candidate.getDateOfBirth(), LocalDate.now()).getYears();
            }

            // Check passport validity
            Boolean isPassportValid = null;
            if (candidate.getPassportExpiryDate() != null) {
                isPassportValid = candidate.getPassportExpiryDate().isAfter(LocalDate.now());
            }

            // Get statuses (only for agency-managed candidates)
            StatusResponse statuses = null;
            if (candidate.isAgencyManaged()) {
                statuses = candidateStatusRepository.findByCandidateId(candidate.getId())
                        .map(status -> StatusResponse.builder()
                                .pccStatus(status.getPccStatus().name())
                                .slcStatus(status.getSlcStatus().name())
                                .workPermitStatus(status.getWorkPermitStatus().name())
                                .visaStatus(status.getVisaStatus().name())
                                .build())
                        .orElse(null);
            }

            String onboardingStageStr = candidate.getOnboardingStage() != null
                    ? candidate.getOnboardingStage().name() : null;

            responseBuilder
                    .candidateProfile(CurrentUserResponse.CandidateProfileResponse.builder()
                            .candidateId(candidate.getId())
                            .firstName(candidate.getFirstName())
                            .lastName(candidate.getLastName())
                            .fullName(candidate.getFullName())
                            .trade(candidate.getTrade())
                            .dateOfBirth(candidate.getDateOfBirth() != null ?
                                    candidate.getDateOfBirth().toString() : null)
                            .age(age)
                            .maritalStatus(candidate.getMaritalStatus() != null ?
                                    candidate.getMaritalStatus().name() : null)
                            .passportNumber(candidate.getPassportNumber())
                            .passportIssueDate(candidate.getPassportIssueDate() != null ?
                                    candidate.getPassportIssueDate().toString() : null)
                            .passportExpiryDate(candidate.getPassportExpiryDate() != null ?
                                    candidate.getPassportExpiryDate().toString() : null)
                            .isPassportValid(isPassportValid)
                            .documentsFolderLink(candidate.getDocumentsFolderLink())
                            .introVideoLink(candidate.getIntroVideoLink())
                            .isEnabled(candidate.getIsEnabled())
                            .onboardingStage(onboardingStageStr)  // Inside nested profile
                            .candidateType(candidate.getCandidateType().name())
                            .createdByType(candidate.getCreatedByType().name())
                            .statuses(statuses)
                            .build())
                    .isProfileComplete(profileComplete)
                    .onboardingStage(onboardingStageStr);

            return responseBuilder.build();
        }

        return responseBuilder.build();
    }

    private AgencyDocumentResponse toDocumentResponse(AgencyDocument document) {
        return AgencyDocumentResponse.builder()
                .id(document.getId())
                .documentType(document.getDocumentType())
                .documentName(document.getDocumentName())
                .documentPath(document.getDocumentPath())
                .fileSize(document.getFileSize())
                .contentType(document.getContentType())
                .status(document.getStatus().name())
                .rejectionReason(document.getRejectionReason())
                .uploadedAt(document.getUploadedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .build();
    }
}