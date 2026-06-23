package com.jobportal.v1.entity;

import com.jobportal.v1.enums.InterviewResult;
import com.jobportal.v1.enums.InterviewStatus;
import com.jobportal.v1.enums.InterviewType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "interviews")
@Data
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "job_application_id", nullable = false, unique = true)
    private JobApplication jobApplication;

    @ManyToOne
    @JoinColumn(name = "candidate_id", nullable = false)
    private Candidate candidate;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(name = "timezone", nullable = false)
    private String timezone = "Asia/Kathmandu";

    @Column(name = "interview_link")
    private String interviewLink;

    @Enumerated(EnumType.STRING)
    @Column(name = "interview_type", nullable = false)
    private InterviewType interviewType;

    @Column(name = "venue")
    private String venue;

    @Column(name = "admin_notes", columnDefinition = "TEXT")
    private String adminNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InterviewStatus status = InterviewStatus.SCHEDULED;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false)
    private InterviewResult result = InterviewResult.SCHEDULED;

    @Column(name = "result_notes", columnDefinition = "TEXT")
    private String resultNotes;

    @Column(name = "result_updated_by")
    private Long resultUpdatedBy;

    @Column(name = "result_updated_at")
    private LocalDateTime resultUpdatedAt;

    @Column(name = "scheduled_by", nullable = false)
    private Long scheduledBy;

    @Column(name = "reminder_sent")
    private Boolean reminderSent = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}