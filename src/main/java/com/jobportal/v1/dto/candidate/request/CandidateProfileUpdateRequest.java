package com.jobportal.v1.dto.candidate.request;

import com.jobportal.v1.enums.MaritalStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CandidateProfileUpdateRequest {
    private String trade;
    private LocalDate dateOfBirth;
    private MaritalStatus maritalStatus;
    private String passportNumber;
    private LocalDate passportIssueDate;
    private LocalDate passportExpiryDate;
    private String documentsFolderLink;
    private String introVideoLink;
}