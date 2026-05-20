package com.jobportal.v1.dto.interview.request;

import com.jobportal.v1.enums.InterviewStatus;
import com.jobportal.v1.enums.InterviewType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InterviewUpdateRequest {
    private LocalDateTime scheduledAt;
    private String timezone;
    private String interviewLink;
    private InterviewType interviewType;
    private String venue;
    private String adminNotes;
    private InterviewStatus status;
}