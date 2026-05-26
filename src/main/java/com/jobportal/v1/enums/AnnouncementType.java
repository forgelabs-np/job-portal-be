package com.jobportal.v1.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Type of announcement")
public enum AnnouncementType {
    GENERAL,        // General information
    JOB_ALERT,      // New job posting or urgent hiring
    SYSTEM_UPDATE,  // System maintenance or feature update
    POLICY_CHANGE,  // Policy or rule change
    EVENT           // Upcoming event or webinar
}