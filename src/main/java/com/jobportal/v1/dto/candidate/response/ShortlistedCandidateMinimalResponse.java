package com.jobportal.v1.dto.candidate.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class ShortlistedCandidateMinimalResponse {
    private Long applicationId;
    private Long jobDemandId;
    private String jobTitle;
    private String jobCountry;
    private String jobCity;
    private Long candidateId;
    private String candidateName;
    private String candidateTrade;
    private String candidateType;
    private Long agencyId;
    private String agencyName;
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime appliedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime shortlistedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime interviewScheduledAt;

    private String interviewStatus;
}