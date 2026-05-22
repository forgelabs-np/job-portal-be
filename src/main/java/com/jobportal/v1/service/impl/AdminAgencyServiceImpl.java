package com.jobportal.v1.service.impl;

import com.jobportal.v1.dto.admin.request.AgencyDocumentApprovalRequest;
import com.jobportal.v1.dto.agency.request.ProfileApprovalRequest;
import com.jobportal.v1.dto.agency.response.AgencyDocumentResponse;
import com.jobportal.v1.dto.agency.response.AgencyProfileResponse;
import com.jobportal.v1.entity.AgencyDocument;
import com.jobportal.v1.entity.AgencyProfile;
import com.jobportal.v1.entity.User;
import com.jobportal.v1.enums.ApprovalStatus;
import com.jobportal.v1.enums.RoleEnum;
import com.jobportal.v1.exception.BadRequestException;
import com.jobportal.v1.exception.ResourceNotFoundException;
import com.jobportal.v1.repository.AgencyDocumentRepository;
import com.jobportal.v1.repository.AgencyProfileRepository;
import com.jobportal.v1.repository.UserRepository;
import com.jobportal.v1.service.AdminAgencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAgencyServiceImpl implements AdminAgencyService {

    private final AgencyProfileRepository agencyProfileRepository;
    private final AgencyDocumentRepository agencyDocumentRepository;
    private final UserRepository userRepository;

    @Override
    public List<AgencyDocumentResponse> getPendingDocuments() {
        List<AgencyDocument> allDocs = agencyDocumentRepository.findAll();
        return allDocs.stream()
                .filter(doc -> doc.getStatus() == ApprovalStatus.PENDING)
                .map(this::toDocumentResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AgencyDocumentResponse> getDocumentsByAgency(Long agencyId) {
        AgencyProfile profile = agencyProfileRepository.findByUserId(agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Agency profile not found"));

        return agencyDocumentRepository.findByAgencyProfileId(profile.getId()).stream()
                .map(this::toDocumentResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AgencyDocumentResponse processDocumentApproval(AgencyDocumentApprovalRequest request, Long adminId) {
        //Validate both documentId AND agencyId
        AgencyDocument document = agencyDocumentRepository.findByIdAndAgencyProfileId(
                        request.getDocumentId(), request.getAgencyId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document not found with id: " + request.getDocumentId() +
                                " for agency: " + request.getAgencyId()));

        ApprovalStatus status = request.getStatus();

        if (status == ApprovalStatus.APPROVED) {
            document.setStatus(ApprovalStatus.APPROVED);
            document.setRejectionReason(null);
            log.info("Document approved: {} by admin: {}", document.getId(), adminId);
        } else if (status == ApprovalStatus.REJECTED) {
            if (request.getRejectionReason() == null || request.getRejectionReason().trim().isEmpty()) {
                throw new BadRequestException("Rejection reason is required when rejecting a document");
            }
            document.setStatus(ApprovalStatus.REJECTED);
            document.setRejectionReason(request.getRejectionReason());
            log.info("Document rejected: {} by admin: {}", document.getId(), adminId);
        } else {
            throw new BadRequestException("Invalid status. Only APPROVED or REJECTED are allowed.");
        }

        agencyDocumentRepository.save(document);
        return toDocumentResponse(document);
    }

    @Override
    public Page<AgencyProfileResponse> getProfilesByStatus(ApprovalStatus status, Pageable pageable) {
        Page<User> agencies;

        if (status == null) {
            agencies = userRepository.findByRolesContaining(RoleEnum.AGENCY, pageable);
        } else {
            agencies = userRepository.findByRolesContainingAndApprovalStatus(RoleEnum.AGENCY, status, pageable);
        }

        return agencies.map(user -> {
            AgencyProfile profile = agencyProfileRepository.findByUserId(user.getId()).orElse(null);
            return toProfileResponse(user, profile);
        });
    }

    @Override
    @Transactional
    public AgencyProfileResponse processProfileApproval(ProfileApprovalRequest request, Long adminId) {
        User agency = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Agency not found"));

        if (!agency.isAgency()) {
            throw new BadRequestException("User is not an agency");
        }

        AgencyProfile profile = agencyProfileRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Agency profile not found"));

        ApprovalStatus status = request.getStatus();

        if (status == ApprovalStatus.APPROVED) {
            // Check if all documents are approved
            List<AgencyDocument> pendingDocs = agencyDocumentRepository.findByAgencyProfileIdAndStatus(
                    profile.getId(), ApprovalStatus.PENDING);

            List<AgencyDocument> rejectedDocs = agencyDocumentRepository.findByAgencyProfileIdAndStatus(
                    profile.getId(), ApprovalStatus.REJECTED);

            if (!pendingDocs.isEmpty()) {
                throw new BadRequestException("Cannot approve profile. " + pendingDocs.size() + " document(s) pending approval.");
            }

            if (!rejectedDocs.isEmpty()) {
                throw new BadRequestException("Cannot approve profile. " + rejectedDocs.size() + " document(s) rejected. Please fix rejected documents first.");
            }

            // Approve the user (this updates users.approval_status)
            agency.approve(adminId);
            userRepository.save(agency);

            // Update profile with approval info
            profile.approveProfile(adminId);
            agencyProfileRepository.save(profile);

            log.info("Profile approved for agency: {} by admin: {}", request.getUserId(), adminId);

        } else if (status == ApprovalStatus.REJECTED) {
            if (request.getRejectionReason() == null || request.getRejectionReason().trim().isEmpty()) {
                throw new BadRequestException("Rejection reason is required when rejecting a profile");
            }

            // Reject the user (this updates users.approval_status)
            agency.reject(request.getRejectionReason());
            userRepository.save(agency);

            // Update profile with rejection info
            profile.rejectProfile(request.getRejectionReason());
            agencyProfileRepository.save(profile);

            log.info("Profile rejected for agency: {} by admin: {}", request.getUserId(), adminId);

        } else {
            throw new BadRequestException("Invalid status. Only APPROVED or REJECTED are allowed for profile processing.");
        }

        return toProfileResponse(agency, profile);
    }

    @Override
    public AgencyProfileResponse getProfileDetails(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        AgencyProfile profile = agencyProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Agency profile not found"));

        return toProfileResponse(user, profile);
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

    private AgencyProfileResponse toProfileResponse(User user, AgencyProfile profile) {
        if (profile == null) return null;

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
}