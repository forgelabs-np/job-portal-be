package com.jobportal.v1.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Interview Result")
public enum InterviewResult {
    PENDING,        // Not yet evaluated
    PASS,           // Candidate passed
    FAIL,           // Candidate failed
    RE_INTERVIEW    // Needs another round
}