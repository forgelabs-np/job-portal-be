package com.jobportal.v1.service;

import com.jobportal.v1.dto.lead.response.LeadResponse;
import com.jobportal.v1.entity.Lead;
import org.springframework.stereotype.Component;

@Component
public class LeadHelper {

    /**
     * Map Lead entity to LeadResponse DTO.
     */
    public LeadResponse mapToResponse(Lead entity) {
        return LeadResponse.builder()
                .id(entity.getId())
                .fullName(entity.getFullName())
                .phoneNumber(entity.getPhoneNumber())
                .email(entity.getEmail())
                .location(entity.getLocation())
                .subject(entity.getSubject())
                .description(entity.getDescription())
                .isRead(entity.getIsRead())
                .isProcessed(entity.getIsProcessed())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
