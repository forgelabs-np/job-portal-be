package com.jobportal.v1.service;

import com.jobportal.v1.entity.User;

public interface EmailService {
    void sendPasswordResetEmail(User user, String token);
    void sendWelcomeEmail(User user);
    void sendVerificationEmail(String email, String name, String otp);
}