package com.jobportal.v1.enums;

public enum JobStatus {
    OPEN,       // Accepting candidates
    CLOSED,     // Manually closed by admin
    COMPLETED,  // All slots filled
    CANCELLED   // Cancelled by admin
}