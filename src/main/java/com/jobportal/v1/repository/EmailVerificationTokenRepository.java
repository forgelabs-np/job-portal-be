package com.jobportal.v1.repository;

import com.jobportal.v1.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByEmail(String email);

    Optional<EmailVerificationToken> findByEmailAndOtp(String email, String otp);

    // Add this method
    Optional<EmailVerificationToken> findByEmailAndUsedFalse(String email);

    @Modifying
    @Transactional
    void deleteByEmail(String email);

    @Modifying
    @Transactional
    @Query("UPDATE EmailVerificationToken e SET e.used = true, e.usedAt = CURRENT_TIMESTAMP WHERE e.email = :email")
    void markAsUsed(@Param("email") String email);

    @Modifying
    @Transactional
    @Query("DELETE FROM EmailVerificationToken e WHERE e.expiryDate < :cutoff")
    int deleteExpiredTokens(@Param("cutoff") LocalDateTime cutoff);
}