package com.jobportal.v1.service.impl;

import com.jobportal.v1.dto.agency.request.ApproveAgencyRequest;
import com.jobportal.v1.dto.agency.request.RejectAgencyRequest;
import com.jobportal.v1.dto.agency.response.AgencyApprovalResponse;
import com.jobportal.v1.entity.User;
import com.jobportal.v1.enums.ApprovalStatus;
import com.jobportal.v1.enums.RoleEnum;
import com.jobportal.v1.exception.BadRequestException;
import com.jobportal.v1.exception.ResourceNotFoundException;
import com.jobportal.v1.repository.UserRepository;
import com.jobportal.v1.service.AdminService;
import com.jobportal.v1.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final EmailService emailService;

    @Override
    @Transactional
    public AgencyApprovalResponse approveAgency(ApproveAgencyRequest request, Long adminId) {
        User agency = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Agency not found"));

        if (!agency.isAgency()) {
            throw new BadRequestException("User is not an agency");
        }

        if (!agency.isPending()) {
            throw new BadRequestException("Agency is already " + agency.getApprovalStatus());
        }

        agency.approve(adminId);
        userRepository.save(agency);

        emailService.sendAgencyApprovalEmail(agency.getEmail(), agency.getFullName());

        log.info("Agency approved: {} by admin: {}", agency.getEmail(), adminId);

        return AgencyApprovalResponse.builder()
                .email(agency.getEmail())
                .approvalStatus(agency.getApprovalStatus())
                .approvedAt(agency.getApprovedAt())
                .message("Agency approved successfully")
                .build();
    }

    @Override
    @Transactional
    public AgencyApprovalResponse rejectAgency(RejectAgencyRequest request, Long adminId) {
        User agency = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Agency not found"));

        if (!agency.isAgency()) {
            throw new BadRequestException("User is not an agency");
        }

        if (!agency.isPending()) {
            throw new BadRequestException("Agency is already " + agency.getApprovalStatus());
        }

        agency.reject(request.getRejectionReason());
        userRepository.save(agency);

        emailService.sendAgencyRejectionEmail(agency.getEmail(), agency.getFullName(), request.getRejectionReason());

        log.info("Agency rejected: {} by admin: {}", agency.getEmail(), adminId);

        return AgencyApprovalResponse.builder()
                .email(agency.getEmail())
                .approvalStatus(agency.getApprovalStatus())
                .rejectionReason(agency.getRejectionReason())
                .message("Agency rejected successfully")
                .build();
    }

    @Override
    public List<AgencyApprovalResponse> getAllPendingAgencies() {
        List<User> pendingAgencies = userRepository.findByRolesContainingAndApprovalStatus(
                RoleEnum.AGENCY, ApprovalStatus.PENDING);

        return pendingAgencies.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AgencyApprovalResponse> getAllApprovedAgencies() {
        List<User> approvedAgencies = userRepository.findByRolesContainingAndApprovalStatus(
                RoleEnum.AGENCY, ApprovalStatus.APPROVED);

        return approvedAgencies.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<AgencyApprovalResponse> getAllRejectedAgencies() {
        List<User> rejectedAgencies = userRepository.findByRolesContainingAndApprovalStatus(
                RoleEnum.AGENCY, ApprovalStatus.REJECTED);

        return rejectedAgencies.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private AgencyApprovalResponse toResponse(User agency) {
        return AgencyApprovalResponse.builder()
                .email(agency.getEmail())
                .approvalStatus(agency.getApprovalStatus())
                .rejectionReason(agency.getRejectionReason())
                .approvedAt(agency.getApprovedAt())
                .message("Agency " + agency.getApprovalStatus().toString().toLowerCase())
                .build();
    }
}