package com.jobportal.v1.scheduler;

import com.jobportal.v1.entity.Interview;
import com.jobportal.v1.enums.InterviewStatus;
import com.jobportal.v1.repository.InterviewRepository;
import com.jobportal.v1.service.EmailService;
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

    @Scheduled(cron = "0 0 8 * * *")  // Runs every day at 8:00 AM
    @Transactional
    public void sendDayBeforeReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.plusHours(20);   // 20 hours from now
        LocalDateTime to = now.plusHours(28);     // 28 hours from now

        List<Interview> upcoming = interviewRepository.findByScheduledAtBetweenAndStatusInAndReminderSentFalse(
                from, to,
                List.of(InterviewStatus.SCHEDULED, InterviewStatus.RESCHEDULED)
        );

        int sentCount = 0;
        for (Interview interview : upcoming) {
            try {
                emailService.sendInterviewReminder(interview);
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