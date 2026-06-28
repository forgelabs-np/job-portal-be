package com.jobportal.v1.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Interview Status/Result (Combined)")
public enum InterviewResult {
    SCHEDULED,      // Interview is scheduled (not yet happened)
    PENDING,        // Interview happened, awaiting result evaluation
    PASS,           // Candidate passed the interview
    FAIL,           // Candidate failed the interview
    RE_INTERVIEW    // Needs another round
}