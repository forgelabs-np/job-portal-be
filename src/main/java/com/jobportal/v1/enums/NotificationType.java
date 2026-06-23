package com.jobportal.v1.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Notification Type")
public enum NotificationType {
    // Application notifications
    NEW_APPLICATION,          // New job application submitted
    APPLICATION_STATUS_CHANGE, // Application status updated

    // Interview notifications
    INTERVIEW_SCHEDULED,      // Interview booked
    INTERVIEW_REMINDER,       // Day before reminder
    INTERVIEW_RESULT,         // Interview result published
    INTERVIEW_CANCELLED,      // Interview cancelled

    // Document notifications
    DOCUMENT_APPROVED,        // Document approved
    DOCUMENT_REJECTED,        // Document rejected

    // Profile notifications
    PROFILE_APPROVED,         // Agency profile approved
    PROFILE_REJECTED,         // Agency profile rejected

    // System notifications
    SYSTEM_ALERT              // General system alerts
}