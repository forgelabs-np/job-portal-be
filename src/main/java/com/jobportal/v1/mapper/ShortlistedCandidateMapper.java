package com.jobportal.v1.mapper;

import com.jobportal.v1.dto.candidate.response.ShortlistedCandidateFullResponse;
import com.jobportal.v1.dto.candidate.response.ShortlistedCandidateMinimalResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ShortlistedCandidateMapper {

    // Admin methods (all candidates)
    List<ShortlistedCandidateMinimalResponse> getShortlistedCandidatesMinimal(
            @Param("applicationType") String applicationType,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    long countShortlistedCandidates(@Param("applicationType") String applicationType);

    // Agency-specific methods
    List<ShortlistedCandidateMinimalResponse> getShortlistedCandidatesForAgency(
            @Param("agencyId") Long agencyId,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    long countShortlistedCandidatesForAgency(@Param("agencyId") Long agencyId);

    // Detail view
    ShortlistedCandidateFullResponse getShortlistedCandidateFull(@Param("applicationId") Long applicationId);
}