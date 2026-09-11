package com.jobportal.v1.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Subject of the lead inquiry")
public enum LeadSubject {
    CONSULTATION,       // General consultation request
    PARTNERSHIP,        // Business partnership inquiry
    FEEDBACK,           // Feedback or suggestions
    GENERAL_INQUIRY,    // General questions
    SUPPORT             // Support or help request
}
