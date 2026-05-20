package com.jobportal.v1.service.impl;

import com.jobportal.v1.entity.*;
import com.jobportal.v1.enums.InterviewType;
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

    @Override
    public void sendInterviewScheduledEmail(Interview interview) {
        try {
            Candidate candidate = interview.getCandidate();
            JobApplication application = interview.getJobApplication();
            JobDemand job = application.getJobDemand();

            String recipientEmail;
            String recipientName;

            if (candidate.isSelfRegistered()) {
                recipientEmail = candidate.getUser().getEmail();
                recipientName = candidate.getFullName();
            } else {
                recipientEmail = candidate.getAgency().getEmail();
                recipientName = candidate.getAgency().getFullName() + " (Agency for " + candidate.getFullName() + ")";
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(recipientEmail);
            helper.setSubject("Interview Scheduled - " + job.getTitle());

            Context context = new Context();
            context.setVariable("recipientName", recipientName);
            context.setVariable("jobTitle", job.getTitle());
            context.setVariable("candidateName", candidate.getFullName());
            context.setVariable("scheduledAt", interview.getScheduledAt());
            context.setVariable("timezone", interview.getTimezone());
            context.setVariable("interviewType", interview.getInterviewType().name());
            context.setVariable("interviewLink", interview.getInterviewLink());
            context.setVariable("venue", interview.getVenue());
            context.setVariable("adminNotes", interview.getAdminNotes());
            context.setVariable("dashboardLink", frontendUrl + "/candidate/applications");
            context.setVariable("supportEmail", supportEmail);
            context.setVariable("currentYear", LocalDateTime.now().getYear());

            String htmlContent = templateEngine.process("email/interview-scheduled", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Interview scheduled email sent to: {}", recipientEmail);

        } catch (MessagingException e) {
            log.error("Failed to send interview scheduled email: {}", e.getMessage());
            throw new RuntimeException("Failed to send interview scheduled email", e);
        }
    }

    @Override
    public void sendInterviewReminder(Interview interview) {
        try {
            Candidate candidate = interview.getCandidate();
            JobApplication application = interview.getJobApplication();
            JobDemand job = application.getJobDemand();

            String recipientEmail;
            String recipientName;

            if (candidate.isSelfRegistered()) {
                recipientEmail = candidate.getUser().getEmail();
                recipientName = candidate.getFullName();
            } else {
                recipientEmail = candidate.getAgency().getEmail();
                recipientName = candidate.getAgency().getFullName() + " (Agency for " + candidate.getFullName() + ")";
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(recipientEmail);
            helper.setSubject("Reminder: Interview Tomorrow - " + job.getTitle());

            Context context = new Context();
            context.setVariable("recipientName", recipientName);
            context.setVariable("jobTitle", job.getTitle());
            context.setVariable("scheduledAt", interview.getScheduledAt());
            context.setVariable("timezone", interview.getTimezone());
            context.setVariable("interviewType", interview.getInterviewType().name());
            context.setVariable("interviewLink", interview.getInterviewLink());
            context.setVariable("dashboardLink", frontendUrl + "/candidate/applications");
            context.setVariable("currentYear", LocalDateTime.now().getYear());

            String htmlContent = templateEngine.process("email/interview-reminder", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Interview reminder email sent to: {}", recipientEmail);

        } catch (MessagingException e) {
            log.error("Failed to send interview reminder email: {}", e.getMessage());
            // Don't throw - reminder failure shouldn't break the flow
        }
    }

    @Override
    public void sendInterviewRescheduledEmail(Interview interview) {
        try {
            Candidate candidate = interview.getCandidate();
            JobApplication application = interview.getJobApplication();
            JobDemand job = application.getJobDemand();

            String recipientEmail;
            String recipientName;

            if (candidate.isSelfRegistered()) {
                recipientEmail = candidate.getUser().getEmail();
                recipientName = candidate.getFullName();
            } else {
                recipientEmail = candidate.getAgency().getEmail();
                recipientName = candidate.getAgency().getFullName() + " (Agency for " + candidate.getFullName() + ")";
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(recipientEmail);
            helper.setSubject("Interview Rescheduled - " + job.getTitle());

            Context context = new Context();
            context.setVariable("recipientName", recipientName);
            context.setVariable("jobTitle", job.getTitle());
            context.setVariable("scheduledAt", interview.getScheduledAt());
            context.setVariable("timezone", interview.getTimezone());
            context.setVariable("interviewLink", interview.getInterviewLink());
            context.setVariable("dashboardLink", frontendUrl + "/candidate/applications");
            context.setVariable("supportEmail", supportEmail);
            context.setVariable("currentYear", LocalDateTime.now().getYear());

            String htmlContent = templateEngine.process("email/interview-rescheduled", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Interview rescheduled email sent to: {}", recipientEmail);

        } catch (MessagingException e) {
            log.error("Failed to send interview rescheduled email: {}", e.getMessage());
            throw new RuntimeException("Failed to send interview rescheduled email", e);
        }
    }

    @Override
    public void sendInterviewCancelledEmail(Interview interview) {
        try {
            Candidate candidate = interview.getCandidate();
            JobApplication application = interview.getJobApplication();
            JobDemand job = application.getJobDemand();

            String recipientEmail;
            String recipientName;

            if (candidate.isSelfRegistered()) {
                recipientEmail = candidate.getUser().getEmail();
                recipientName = candidate.getFullName();
            } else {
                recipientEmail = candidate.getAgency().getEmail();
                recipientName = candidate.getAgency().getFullName() + " (Agency for " + candidate.getFullName() + ")";
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(recipientEmail);
            helper.setSubject("Interview Cancelled - " + job.getTitle());

            Context context = new Context();
            context.setVariable("recipientName", recipientName);
            context.setVariable("jobTitle", job.getTitle());
            context.setVariable("scheduledAt", interview.getScheduledAt());
            context.setVariable("dashboardLink", frontendUrl + "/candidate/applications");
            context.setVariable("supportEmail", supportEmail);
            context.setVariable("currentYear", LocalDateTime.now().getYear());

            String htmlContent = templateEngine.process("email/interview-cancelled", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Interview cancelled email sent to: {}", recipientEmail);

        } catch (MessagingException e) {
            log.error("Failed to send interview cancelled email: {}", e.getMessage());
            throw new RuntimeException("Failed to send interview cancelled email", e);
        }
    }

    private String formatOTP(String token) {
        if (token != null && token.length() == 6) {
            return String.join(" ", token.split(""));
        }
        return token;
    }
}