package com.jobportal.v1.dto.interview.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.jobportal.v1.enums.InterviewResult;
import com.jobportal.v1.enums.InterviewType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InterviewResponse {
    private Long id;
    private Long jobApplicationId;
    private String jobTitle;
    private Long candidateId;
    private String candidateName;
    private String candidateType;
    private Long agencyId;
    private String agencyName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime scheduledAt;
    private String timezone;
    private String interviewLink;
    private InterviewType interviewType;
    private String venue;
    private String adminNotes;

    private InterviewResult result;

    private String resultNotes;
    private Long resultUpdatedBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime resultUpdatedAt;

    private Long scheduledBy;
    private Boolean reminderSent;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}