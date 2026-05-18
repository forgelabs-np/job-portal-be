package com.jobportal.v1.enums;

public enum OnboardingStage {
    PROFILE,     // candidate needs to fill profile fields
    DOCUMENTS,   // profile complete, needs to upload passport
    COMPLETE     // fully onboarded, show dashboard
}