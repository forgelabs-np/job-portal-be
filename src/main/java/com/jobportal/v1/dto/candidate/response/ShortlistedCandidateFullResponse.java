package com.jobportal.v1.dto.candidate.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
public class ShortlistedCandidateFullResponse {
    // Application Info
    private Long applicationId;
    private String applicationStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime appliedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime shortlistedAt;

    private Long shortlistedBy;
    private Long reviewedBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reviewedAt;

    private String rejectionReason;

    // Job Info
    private Long jobDemandId;
    private String jobTitle;
    private String jobCountry;
    private String jobCity;
    private String jobDescription;
    private String jobRequirements;
    private Double salaryAmount;
    private String salaryCurrency;
    private Integer totalSlots;
    private Integer remainingSlots;

    // Candidate Info
    private Long candidateId;
    private String candidateName;
    private String candidateTrade;
    private String candidateType;
    private String candidateEmail;
    private String candidatePhone;
    private String candidatePassportNumber;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate candidateDateOfBirth;

    private String candidateMaritalStatus;
    private Boolean isProfileComplete;
    private Boolean isEnabled;
    private String onboardingStage;

    // Agency Info
    private Long agencyId;
    private String agencyName;
    private String agencyEmail;
    private String agencyPhone;

    // Interview Info
    private Long interviewId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime interviewScheduledAt;

    private String interviewStatus;
    private String interviewLink;
    private String interviewType;
    private String interviewVenue;
    private String interviewResult;
    private String interviewResultNotes;

    // Documents
    private List<DocumentInfoResponse> documents;
}