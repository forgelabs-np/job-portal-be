package com.jobportal.v1.scheduler;

import com.jobportal.v1.entity.Candidate;
import com.jobportal.v1.entity.Interview;
import com.jobportal.v1.enums.InterviewResult;
import com.jobportal.v1.enums.NotificationType;
import com.jobportal.v1.repository.InterviewRepository;
import com.jobportal.v1.service.EmailService;
import com.jobportal.v1.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class InterviewReminderScheduler {

    private final InterviewRepository interviewRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void sendDayBeforeReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.plusHours(20);
        LocalDateTime to = now.plusHours(28);

        List<Interview> upcoming = interviewRepository.findByScheduledAtBetweenAndResultInAndReminderSentFalse(
                from, to,
                List.of(InterviewResult.SCHEDULED, InterviewResult.RE_INTERVIEW)
        );

        int sentCount = 0;
        for (Interview interview : upcoming) {
            try {
                emailService.sendInterviewReminder(interview);

                Candidate candidate = interview.getCandidate();
                if (candidate.getUser() != null) {
                    notificationService.sendNotification(
                            candidate.getUser().getId(),
                            NotificationType.INTERVIEW_REMINDER,
                            "Interview Tomorrow!",
                            "You have an interview tomorrow for " +
                                    interview.getJobApplication().getJobDemand().getTitle() +
                                    " at " + interview.getScheduledAt(),
                            "/candidate/interviews/" + interview.getId()
                    );
                }

                interview.setReminderSent(true);
                interviewRepository.save(interview);
                sentCount++;
                log.info("Reminder sent for interview: {}", interview.getId());
            } catch (Exception e) {
                log.error("Failed to send reminder for interview: {}", interview.getId(), e);
            }
        }

        log.info("Interview reminder job completed. Sent: {}", sentCount);
    }
}