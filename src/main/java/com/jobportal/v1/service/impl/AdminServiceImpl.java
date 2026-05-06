package com.jobportal.v1.service.impl;

import com.jobportal.v1.dto.agency.request.AgencyActionRequest;
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
    public AgencyApprovalResponse processAgencyAction(AgencyActionRequest request, Long adminId) {
        User agency = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Agency not found"));

        if (!agency.isAgency()) {
            throw new BadRequestException("User is not an agency");
        }

        if (!agency.isPending()) {
            throw new BadRequestException("Agency is already " + agency.getApprovalStatus());
        }

        String action = request.getAction().toUpperCase();

        if ("APPROVE".equals(action)) {
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

        } else if ("REJECT".equals(action)) {
            if (request.getRejectionReason() == null || request.getRejectionReason().trim().isEmpty()) {
                throw new BadRequestException("Rejection reason is required");
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
        } else {
            throw new BadRequestException("Invalid action. Use APPROVE or REJECT");
        }
    }

    @Override
    public List<AgencyApprovalResponse> getAgenciesByStatus(ApprovalStatus status) {
        List<User> agencies;

        if (status == null) {
            agencies = userRepository.findByRolesContaining(RoleEnum.AGENCY);
        } else {
            agencies = userRepository.findByRolesContainingAndApprovalStatus(RoleEnum.AGENCY, status);
        }

        return agencies.stream()
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