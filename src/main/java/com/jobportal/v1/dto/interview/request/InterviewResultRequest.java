package com.jobportal.v1.dto.interview.request;

import com.jobportal.v1.enums.InterviewResult;
import lombok.Data;

@Data
public class InterviewResultRequest {
    private InterviewResult result;
    private String resultNotes;
}