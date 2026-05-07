package com.jobportal.v1.enums;

public enum ApplicationStatus {
    PENDING,      // Awaiting admin review
    REVIEWED,     // Admin has reviewed
    SHORTLISTED,  // Candidate selected
    REJECTED,     // Candidate rejected
    WITHDRAWN     // Agency withdrew application
}