package com.jobportal.v1.service;

import com.jobportal.v1.entity.Interview;
import com.jobportal.v1.entity.User;

public interface EmailService {
    void sendPasswordResetEmail(User user, String token);

    void sendAgencyApprovalEmail(String to, String agencyName);

    void sendAgencyRejectionEmail(String to, String agencyName, String rejectionReason);

    void sendWelcomeEmail(User user);

    void sendVerificationEmail(String email, String name, String otp);

    void sendInterviewScheduledEmail(Interview interview);

    void sendInterviewReminder(Interview interview);

    void sendInterviewRescheduledEmail(Interview interview);

    void sendInterviewCancelledEmail(Interview interview);
}