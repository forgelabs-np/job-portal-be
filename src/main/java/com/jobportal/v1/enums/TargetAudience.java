package com.jobportal.v1.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Target audience for announcement")
public enum TargetAudience {
    ALL,            // Both agencies and candidates
    AGENCY_ONLY,    // Only agencies
    CANDIDATE_ONLY  // Only candidates
}