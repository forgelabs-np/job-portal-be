package com.jobportal.v1.service;

import com.jobportal.v1.dto.candidate.response.ShortlistedCandidateFullResponse;
import com.jobportal.v1.dto.candidate.response.ShortlistedCandidateMinimalResponse;
import com.jobportal.v1.exception.BadRequestException;
import com.jobportal.v1.exception.ResourceNotFoundException;
import com.jobportal.v1.mapper.ShortlistedCandidateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShortlistedCandidateService {

    private final ShortlistedCandidateMapper shortlistedCandidateMapper;

    @Transactional(readOnly = true)
    public Page<ShortlistedCandidateMinimalResponse> getShortlistedCandidates(String applicationType, Pageable pageable) {
        // Validate applicationType
        if (applicationType != null && !applicationType.isEmpty()) {
            String upperType = applicationType.toUpperCase();
            if (!upperType.equals("SELF") && !upperType.equals("AGENCY")) {
                throw new BadRequestException("Invalid applicationType. Allowed: SELF, AGENCY, or null for all");
            }
            applicationType = upperType;
        }

        long total = shortlistedCandidateMapper.countShortlistedCandidates(applicationType);
        List<ShortlistedCandidateMinimalResponse> content = shortlistedCandidateMapper.getShortlistedCandidatesMinimal(
                applicationType,
                pageable.getPageSize(),
                (int) pageable.getOffset()
        );

        return new PageImpl<>(content, pageable, total);
    }

    @Transactional(readOnly = true)
    public Page<ShortlistedCandidateMinimalResponse> getShortlistedCandidatesForAgency(Long agencyId, Pageable pageable) {
        long total = shortlistedCandidateMapper.countShortlistedCandidatesForAgency(agencyId);
        List<ShortlistedCandidateMinimalResponse> content = shortlistedCandidateMapper.getShortlistedCandidatesForAgency(
                agencyId,
                pageable.getPageSize(),
                (int) pageable.getOffset()
        );

        return new PageImpl<>(content, pageable, total);
    }

    @Transactional(readOnly = true)
    public ShortlistedCandidateFullResponse getShortlistedCandidateById(Long applicationId) {
        ShortlistedCandidateFullResponse response = shortlistedCandidateMapper.getShortlistedCandidateFull(applicationId);
        if (response == null) {
            throw new ResourceNotFoundException("Shortlisted candidate not found with application id: " + applicationId);
        }
        return response;
    }
}