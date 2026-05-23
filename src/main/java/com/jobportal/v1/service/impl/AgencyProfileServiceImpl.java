package com.jobportal.v1.service.impl;

import com.jobportal.v1.dto.agency.request.AgencyProfileRequest;
import com.jobportal.v1.dto.agency.response.*;
import com.jobportal.v1.dto.dashboard.response.*;
import com.jobportal.v1.entity.*;
import com.jobportal.v1.enums.ApprovalStatus;
import com.jobportal.v1.exception.BadRequestException;
import com.jobportal.v1.exception.ResourceNotFoundException;
import com.jobportal.v1.repository.*;
import com.jobportal.v1.service.AgencyProfileService;
import com.jobportal.v1.util.FileUploadUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgencyProfileServiceImpl implements AgencyProfileService {

    private final AgencyProfileRepository agencyProfileRepository;
    private final AgencyDocumentRepository agencyDocumentRepository;
    private final UserRepository userRepository;
    private final FileUploadUtil fileUploadUtil;

    @Override
    @Transactional
    public AgencyProfileResponse createOrUpdateProfile(Long userId, AgencyProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.isAgency()) {
            throw new BadRequestException("Only agencies can have a profile");
        }

        AgencyProfile profile = agencyProfileRepository.findByUserId(userId)
                .orElse(new AgencyProfile());

        boolean isNewProfile = (profile.getId() == null);

        // Capture old values for change detection
        String oldCompanyName = profile.getCompanyName();
        String oldContactPersonName = profile.getContactPersonName();
        String oldContactPersonEmail = profile.getContactPersonEmail();
        ApprovalStatus oldUserStatus = user.getApprovalStatus();

        // Update profile fields
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

        boolean isBasicComplete = isProfileDataComplete(request);
        profile.setProfileComplete(isBasicComplete);

        // Detect if there were actual changes
        boolean hasChanges = !isNewProfile && (
                !equals(request.getCompanyName(), oldCompanyName) ||
                        !equals(request.getContactPersonName(), oldContactPersonName) ||
                        !equals(request.getContactPersonEmail(), oldContactPersonEmail)
        );

        // If changes detected and user was APPROVED or REJECTED, reset to PENDING
        if (!isNewProfile && hasChanges &&
                (oldUserStatus == ApprovalStatus.APPROVED || oldUserStatus == ApprovalStatus.REJECTED)) {

            user.setApprovalStatus(ApprovalStatus.PENDING);
            user.setRejectionReason(null);
            user.setApprovedAt(null);
            user.setApprovedBy(null);
            userRepository.save(user);

            log.info("User approval status reset to PENDING due to profile changes by agency: {}", user.getEmail());
        }

        AgencyProfile savedProfile = agencyProfileRepository.save(profile);

        log.info("Agency profile {} for user: {}",
                isNewProfile ? "created" : "updated", user.getEmail());

        return toResponse(savedProfile, user);
    }

    private boolean equals(String s1, String s2) {
        if (s1 == null && s2 == null) return true;
        if (s1 == null || s2 == null) return false;
        return s1.equals(s2);
    }

    @Override
    public AgencyProfileResponse getProfileByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        AgencyProfile profile = agencyProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + userId));

        return toResponse(profile, user);
    }

    @Override
    public AgencyProfileResponse getMyProfile(Long userId) {
        return getProfileByUserId(userId);
    }

    @Override
    public boolean isProfileComplete(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return false;

        AgencyProfile profile = agencyProfileRepository.findByUserId(userId).orElse(null);
        if (profile == null) return false;

        return profile.isProfileComplete() && user.isApproved();
    }

    @Override
    @Transactional
    public AgencyDocumentResponse uploadDocument(Long userId, String documentType, MultipartFile file) {
        AgencyProfile profile = agencyProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found. Please create profile first."));

        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        try {
            AgencyDocument existingDocument = agencyDocumentRepository
                    .findByAgencyProfileIdAndDocumentType(profile.getId(), documentType)
                    .orElse(null);

            String filePath;

            if (existingDocument != null) {
                fileUploadUtil.deleteFile(existingDocument.getDocumentPath());
                filePath = fileUploadUtil.uploadAgencyDocument(profile.getId(), documentType, file);

                existingDocument.setDocumentName(file.getOriginalFilename());
                existingDocument.setDocumentPath(filePath);
                existingDocument.setFileSize(file.getSize());
                existingDocument.setContentType(file.getContentType());
                existingDocument.setStatus(ApprovalStatus.PENDING);
                existingDocument.setRejectionReason(null);
                existingDocument.setUpdatedAt(LocalDateTime.now());

                AgencyDocument updated = agencyDocumentRepository.save(existingDocument);
                log.info("Document re-uploaded for agency: {}, type: {}", userId, documentType);
                return toDocumentResponse(updated);
            } else {
                filePath = fileUploadUtil.uploadAgencyDocument(profile.getId(), documentType, file);

                AgencyDocument document = new AgencyDocument();
                document.setAgencyProfile(profile);
                document.setDocumentType(documentType);
                document.setDocumentName(file.getOriginalFilename());
                document.setDocumentPath(filePath);
                document.setFileSize(file.getSize());
                document.setContentType(file.getContentType());
                document.setStatus(ApprovalStatus.PENDING);

                AgencyDocument saved = agencyDocumentRepository.save(document);
                log.info("New document uploaded for agency: {}, type: {}", userId, documentType);
                return toDocumentResponse(saved);
            }

        } catch (IOException e) {
            log.error("Failed to upload document", e);
            throw new RuntimeException("Failed to upload document", e);
        }
    }

    @Override
    public List<AgencyDocumentResponse> getMyDocuments(Long userId) {
        AgencyProfile profile = agencyProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        return agencyDocumentRepository.findByAgencyProfileId(profile.getId()).stream()
                .map(this::toDocumentResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteDocument(Long userId, Long documentId) {
        AgencyProfile profile = agencyProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        AgencyDocument document = agencyDocumentRepository.findByIdAndAgencyProfileId(documentId, profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));

        fileUploadUtil.deleteFile(document.getDocumentPath());
        agencyDocumentRepository.delete(document);

        log.info("Document deleted for agency: {}, documentId: {}", userId, documentId);
    }

    private boolean isProfileDataComplete(AgencyProfileRequest request) {
        return request.getCompanyName() != null && !request.getCompanyName().trim().isEmpty() &&
                request.getContactPersonName() != null && !request.getContactPersonName().trim().isEmpty() &&
                request.getContactPersonEmail() != null && !request.getContactPersonEmail().trim().isEmpty();
    }

    private AgencyProfileResponse toResponse(AgencyProfile profile, User user) {
        List<AgencyDocumentResponse> documents = agencyDocumentRepository.findByAgencyProfileId(profile.getId()).stream()
                .map(this::toDocumentResponse)
                .collect(Collectors.toList());

        return AgencyProfileResponse.builder()
                .id(profile.getId())
                .userId(user.getId())
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
                .profileApprovalStatus(user.getApprovalStatus().name())
                .profileRejectionReason(user.getRejectionReason())
                .documents(documents)
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
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