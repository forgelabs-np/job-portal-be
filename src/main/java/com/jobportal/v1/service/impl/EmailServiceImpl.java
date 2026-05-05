package com.jobportal.v1.service.impl;

import com.jobportal.v1.entity.User;
import com.jobportal.v1.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Value("${app.support.email:support@jobportal.com}")
    private String supportEmail;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.security.email-verification.otp-expiration-minutes:15}")
    private int otpExpirationMinutes;

    @Override
    public void sendVerificationEmail(String email, String name, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(email);
            helper.setSubject("Verify Your Email - JobPortal");

            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("otp", formatOTP(otp));
            context.setVariable("expirationMinutes", otpExpirationMinutes);
            context.setVariable("supportEmail", supportEmail);
            context.setVariable("currentYear", LocalDateTime.now().getYear());
            context.setVariable("requestTime", LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a")));
            context.setVariable("verificationLink", frontendUrl + "/verify-email?email=" +
                    URLEncoder.encode(email, StandardCharsets.UTF_8) + "&otp=" + otp);

            String htmlContent = templateEngine.process("email/verification-email", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Verification email sent to: {}", email);

        } catch (MessagingException e) {
            log.error("Failed to send verification email: {}", e.getMessage());
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    @Override
    public void sendAgencyApprovalEmail(String to, String agencyName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("Your Agency Account Has Been Approved! - JobPortal");

            Context context = new Context();
            context.setVariable("agencyName", agencyName);
            context.setVariable("profileLink", frontendUrl + "/agency/profile");
            context.setVariable("currentYear", LocalDateTime.now().getYear());
            context.setVariable("approvalTime", LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a")));

            String htmlContent = templateEngine.process("email/approval-email", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Approval email sent to: {}", to);

        } catch (MessagingException e) {
            log.error("Failed to send approval email: {}", e.getMessage());
            throw new RuntimeException("Failed to send approval email", e);
        }
    }

    @Override
    public void sendAgencyRejectionEmail(String to, String agencyName, String rejectionReason) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("Update on Your Agency Application - JobPortal");

            Context context = new Context();
            context.setVariable("agencyName", agencyName);
            context.setVariable("rejectionReason", rejectionReason);
            context.setVariable("resubmitLink", frontendUrl + "/agency/register");
            context.setVariable("currentYear", LocalDateTime.now().getYear());
            context.setVariable("reviewTime", LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a")));

            String htmlContent = templateEngine.process("email/rejection-email", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Rejection email sent to: {}", to);

        } catch (MessagingException e) {
            log.error("Failed to send rejection email: {}", e.getMessage());
            throw new RuntimeException("Failed to send rejection email", e);
        }
    }

    @Override
    public void sendWelcomeEmail(User user) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(user.getEmail());
            helper.setSubject("Welcome to JobPortal!");
            helper.setFrom(fromEmail, "JobPortal Team");

            Context context = new Context();
            context.setVariable("name", user.getFullName());
            context.setVariable("loginLink", frontendUrl + "/login");
            context.setVariable("currentYear", LocalDateTime.now().getYear());

            String htmlContent = templateEngine.process("email/welcome-email", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Welcome email sent to: {}", user.getEmail());

        } catch (Exception e) {
            log.error("Failed to send welcome email: {}", e.getMessage());
        }
    }

    @Override
    public void sendPasswordResetEmail(User user, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(user.getEmail());
            helper.setSubject("Password Reset Request - JobPortal");

            Context context = new Context();
            context.setVariable("name", user.getFullName());
            context.setVariable("token", formatOTP(token));
            context.setVariable("resetLink", frontendUrl + "/reset-password?token=" + token);
            context.setVariable("supportEmail", supportEmail);
            context.setVariable("currentYear", LocalDateTime.now().getYear());
            context.setVariable("requestTime", LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' hh:mm a")));

            String htmlContent = templateEngine.process("email/password-reset-email", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Password reset email sent to: {}", user.getEmail());

        } catch (MessagingException e) {
            log.error("Failed to send password reset email: {}", e.getMessage());
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    private String formatOTP(String token) {
        if (token != null && token.length() == 6) {
            return String.join(" ", token.split(""));
        }
        return token;
    }
}