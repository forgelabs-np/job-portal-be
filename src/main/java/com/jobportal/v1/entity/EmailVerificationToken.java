package com.jobportal.v1.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_verification_tokens")
@Data
public class EmailVerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 6)
    private String otp;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String encodedPassword;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Column(nullable = false)
    private boolean used = false;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime usedAt;

    @Column
    private LocalDateTime lastResendAt;

    @Column(length = 45)
    private String ipAddress;

    @Column
    private String userAgent;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }

    public boolean canResend(int cooldownSeconds) {
        if (lastResendAt == null) return true;
        return LocalDateTime.now().isAfter(lastResendAt.plusSeconds(cooldownSeconds));
    }
    public long getSecondsRemaining(int cooldownSeconds) {
        if (lastResendAt == null) return 0;
        long elapsedSeconds = java.time.Duration.between(lastResendAt, LocalDateTime.now()).getSeconds();
        long remaining = cooldownSeconds - elapsedSeconds;
        return remaining > 0 ? remaining : 0;
    }
}