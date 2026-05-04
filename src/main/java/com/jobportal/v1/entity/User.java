package com.jobportal.v1.entity;

import com.jobportal.v1.enums.RoleEnum;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "agency_name")
    private String agencyName;

    @Column(name = "agency_description", columnDefinition = "TEXT")
    private String agencyDescription;

    @ElementCollection(targetClass = RoleEnum.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<RoleEnum> roles = new HashSet<>();

    @Column(name = "is_active")
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;

    @Column(name = "login_count")
    private Integer loginCount = 0;

    @Column(name = "password_updated_at")
    private LocalDateTime passwordUpdatedAt;

    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts = 0;

    @Column(name = "account_locked_until")
    private LocalDateTime accountLockedUntil;

    @Column(name = "email_verified")
    private boolean emailVerified = false;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @Version
    private Long version;

    public void verifyEmail() {
        this.emailVerified = true;
        this.emailVerifiedAt = LocalDateTime.now();
        this.isActive = true;
    }

    public void recordLogin() {
        this.lastLoginTime = LocalDateTime.now();
        this.loginCount = (this.loginCount == null) ? 1 : this.loginCount + 1;
        this.failedLoginAttempts = 0;
        this.accountLockedUntil = null;
    }

    public void recordFailedLogin(int maxLoginAttempts, int lockDurationMinutes) {
        this.failedLoginAttempts = (this.failedLoginAttempts == null) ? 1 : this.failedLoginAttempts + 1;
        if (this.failedLoginAttempts >= maxLoginAttempts) {
            this.accountLockedUntil = LocalDateTime.now().plusMinutes(lockDurationMinutes);
        }
    }

    public boolean isAccountLocked() {
        return accountLockedUntil != null && accountLockedUntil.isAfter(LocalDateTime.now());
    }

    public void unlockAccount() {
        this.failedLoginAttempts = 0;
        this.accountLockedUntil = null;
    }

    public boolean hasRole(RoleEnum role) {
        return roles != null && roles.contains(role);
    }

    public boolean isAdmin() {
        return hasRole(RoleEnum.ADMIN);
    }

    public boolean isAgency() {
        return hasRole(RoleEnum.AGENCY);
    }
}