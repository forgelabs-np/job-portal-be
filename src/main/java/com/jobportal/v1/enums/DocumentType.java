package com.jobportal.v1.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Document Type")
public enum DocumentType {
    PASSPORT,
    PCC,        // Police Clearance Certificate
    SLC,        // School Leaving Certificate
    VISA,
    CV,
    MOU,
    COMPANY_REGISTRATION,
    TRADE_LICENCE,
    CERTIFICATE,
    EXPERIENCE_LETTER,
    TRAINING_CERTIFICATE,
    MEDICAL_CERTIFICATE
}