package com.jobportal.v1.dto.candidate.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jobportal.v1.enums.MaritalStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CandidateResponse {
    private Long id;
    private Long agencyId;
    private String agencyName;
    private Long userId;
    private String userEmail;
    private String candidateType;
    private String createdByType;

    private String firstName;
    private String lastName;
    private String fullName;
    private String trade;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;
    private Integer age;
    private MaritalStatus maritalStatus;

    private String passportNumber;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate passportIssueDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate passportExpiryDate;
    private Boolean isPassportValid;

    private String documentsFolderLink;
    private String introVideoLink;
    private Boolean isEnabled;
    private Boolean profileComplete;
    private String onboardingStage;

    private List<CandidateDocumentResponse> documents;
    private StatusResponse statuses;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}