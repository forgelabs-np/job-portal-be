package com.jobportal.v1.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Interview Status")
public enum InterviewStatus {
    SCHEDULED,      // Interview is booked
    RESCHEDULED,    // Date/time changed after initial schedule
    COMPLETED,      // Interview happened
    CANCELLED,      // Called off
    NO_SHOW         // Candidate didn't appear
}