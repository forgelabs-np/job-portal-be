package com.jobportal.v1.service.impl;

import com.jobportal.v1.dto.agency.request.AgencyProfileRequest;
import com.jobportal.v1.dto.agency.response.AgencyProfileResponse;
import com.jobportal.v1.entity.AgencyProfile;
import com.jobportal.v1.entity.User;
import com.jobportal.v1.exception.BadRequestException;
import com.jobportal.v1.exception.ResourceNotFoundException;
import com.jobportal.v1.repository.AgencyProfileRepository;
import com.jobportal.v1.repository.UserRepository;
import com.jobportal.v1.service.AgencyProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgencyProfileServiceImpl implements AgencyProfileService {

    private final AgencyProfileRepository agencyProfileRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public AgencyProfileResponse createOrUpdateProfile(Long userId, AgencyProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.isAgency()) {
            throw new BadRequestException("Only agencies can have a profile");
        }

        if (!user.isApproved()) {
            throw new BadRequestException("Your account is not approved yet. Please wait for admin approval.");
        }

        AgencyProfile profile = agencyProfileRepository.findByUserId(userId)
                .orElse(new AgencyProfile());

        profile.setUser(user);
        profile.setCompanyName(request.getCompanyName());
        profile.setCompanyDescription(request.getCompanyDescription());
        profile.setCompanyWebsite(request.getCompanyWebsite());
        profile.setCompanyLogoUrl(request.getCompanyLogoUrl());
        profile.setCompanyAddress(request.getCompanyAddress());
        profile.setCompanyPhone(request.getCompanyPhone());
        profile.setRegistrationNumber(request.getRegistrationNumber());
        profile.setTaxId(request.getTaxId());
        profile.setContactPersonName(request.getContactPersonName());
        profile.setContactPersonEmail(request.getContactPersonEmail());
        profile.setContactPersonPhone(request.getContactPersonPhone());

        // Check if profile is complete (all required fields filled)
        profile.setProfileComplete(isProfileDataComplete(request));

        AgencyProfile savedProfile = agencyProfileRepository.save(profile);

        log.info("Agency profile {} for user: {}",
                profile.getId() == null ? "created" : "updated", user.getEmail());

        return toResponse(savedProfile);
    }

    @Override
    public AgencyProfileResponse getProfileByUserId(Long userId) {
        AgencyProfile profile = agencyProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + userId));
        return toResponse(profile);
    }

    @Override
    public AgencyProfileResponse getMyProfile(Long userId) {
        return getProfileByUserId(userId);
    }

    @Override
    public boolean isProfileComplete(Long userId) {
        return agencyProfileRepository.findByUserId(userId)
                .map(AgencyProfile::isProfileComplete)
                .orElse(false);
    }

    private boolean isProfileDataComplete(AgencyProfileRequest request) {
        return request.getCompanyName() != null && !request.getCompanyName().trim().isEmpty() &&
                request.getContactPersonName() != null && !request.getContactPersonName().trim().isEmpty() &&
                request.getContactPersonEmail() != null && !request.getContactPersonEmail().trim().isEmpty();
    }

    private AgencyProfileResponse toResponse(AgencyProfile profile) {
        return AgencyProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .companyName(profile.getCompanyName())
                .companyDescription(profile.getCompanyDescription())
                .companyWebsite(profile.getCompanyWebsite())
                .companyLogoUrl(profile.getCompanyLogoUrl())
                .companyAddress(profile.getCompanyAddress())
                .companyPhone(profile.getCompanyPhone())
                .registrationNumber(profile.getRegistrationNumber())
                .taxId(profile.getTaxId())
                .contactPersonName(profile.getContactPersonName())
                .contactPersonEmail(profile.getContactPersonEmail())
                .contactPersonPhone(profile.getContactPersonPhone())
                .profileComplete(profile.isProfileComplete())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}