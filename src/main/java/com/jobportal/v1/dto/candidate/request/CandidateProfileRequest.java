package com.jobportal.v1.dto.candidate.request;

import com.jobportal.v1.enums.DocumentType;
import com.jobportal.v1.enums.MaritalStatus;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class CandidateProfileRequest {
    private Long id;

    private String firstName;
    private String lastName;
    private String trade;
    private LocalDate dateOfBirth;
    private MaritalStatus maritalStatus;
    private String passportNumber;
    private LocalDate passportIssueDate;
    private LocalDate passportExpiryDate;
    private String documentsFolderLink;
    private String introVideoLink;

//    private List<DocumentInfo> documents;
//
//    @Data
//    public static class DocumentInfo {
//        private DocumentType documentType;
//        private String documentName;
//        private String notes;
//    }
}