package com.jobportal.v1.service.impl;

import com.jobportal.v1.entity.PasswordResetLog;
import com.jobportal.v1.entity.PasswordResetToken;
import com.jobportal.v1.entity.User;
import com.jobportal.v1.exception.TokenExpiredException;
import com.jobportal.v1.exception.TokenNotFoundException;
import com.jobportal.v1.repository.PasswordResetLogRepository;
import com.jobportal.v1.repository.PasswordResetTokenRepository;
import com.jobportal.v1.repository.UserRepository;
import com.jobportal.v1.service.EmailService;
import com.jobportal.v1.service.PasswordResetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetServiceImpl.class);
    private static final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.security.password-reset.token-expiration-ms:3600000}")
    private long tokenExpirationMs;

    @Value("${app.security.password-reset.max-otp-attempts:3}")
    private int maxOtpAttempts;

    @Value("${app.security.password-reset.request-cooldown-seconds:60}")
    private int requestCooldownSeconds;

    @Value("${app.security.password-reset.max-daily-requests:5}")
    private int maxDailyRequests;

    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordResetLogRepository logRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public PasswordResetServiceImpl(PasswordResetTokenRepository tokenRepository,
                                    PasswordResetLogRepository logRepository,
                                    UserRepository userRepository,
                                    PasswordEncoder passwordEncoder,
                                    EmailService emailService) {
        this.tokenRepository = tokenRepository;
        this.logRepository = logRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public void requestPasswordReset(String email, String ipAddress, String userAgent) {
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format");
        }

        Optional<User> userOpt = userRepository.findByEmail(email.toLowerCase().trim());

        if (userOpt.isEmpty()) {
            // Silent return — don't reveal whether email exists (prevents enumeration)
            logger.warn("Password reset requested for non-existent email from IP: {}", ipAddress);
            return;
        }

        User user = userOpt.get();

        Optional<PasswordResetLog> lastRequest =
                logRepository.findTopByUserOrderByRequestedAtDesc(user);

        if (lastRequest.isPresent()) {
            long secondsSince = java.time.Duration.between(
                    lastRequest.get().getRequestedAt(),
                    LocalDateTime.now()).getSeconds();

            if (secondsSince < requestCooldownSeconds) {
                long remaining = requestCooldownSeconds - secondsSince;
                throw new RuntimeException(String.format(
                        "Please wait %d seconds before requesting another password reset.",
                        remaining));
            }
        }

        // ── Daily limit check — max requests per 24 hours
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        long requestsToday = logRepository.countByUserAndRequestedAtAfter(user, since);

        if (requestsToday >= maxDailyRequests) {
            logger.warn("Max daily password reset attempts exceeded for user: {} from IP: {}",
                    user.getEmail(), ipAddress);
            throw new RuntimeException(
                    "Too many password reset requests. " +
                            "Please try again after 24 hours or contact support.");
        }

        // flush() forces the DELETE to DB before the INSERT to avoid constraint violations
        tokenRepository.deleteByUser(user);
        tokenRepository.flush();

        // ── Generate and save new OTP
        String otp = generateOTP();

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(otp);
        resetToken.setUser(user);
        resetToken.setExpiryDate(LocalDateTime.now().plusSeconds(tokenExpirationMs / 1000));
        resetToken.setUsed(false);
        resetToken.setAttempts(0);
        tokenRepository.save(resetToken);

        // ── Log the request
        PasswordResetLog log = new PasswordResetLog();
        log.setUser(user);
        log.setRequestedAt(LocalDateTime.now());
        log.setStatus("REQUESTED");
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        logRepository.save(log);

        // ── Send email
        emailService.sendPasswordResetEmail(user, otp);

        logger.info("Password reset OTP sent for user: {} (request #{} in last 24h)",
                user.getEmail(), requestsToday + 1);
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword, String ipAddress, String userAgent) {

        // ── Validate OTP format
        if (!isValidOTP(token)) {
            throw new IllegalArgumentException("Invalid OTP format. Must be 6 digits.");
        }

        // ── Find token
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    logger.warn("Invalid password reset OTP used from IP: {}", ipAddress);
                    return new TokenNotFoundException("Invalid or expired OTP");
                });

        // ── Already used
        if (resetToken.isUsed()) {
            logger.warn("Attempt to reuse already-used password reset OTP from IP: {}", ipAddress);
            throw new TokenExpiredException("This OTP has already been used. Please request a new one.");
        }

        // ── Expired — delete and force fresh request ──────────────────────
        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            logger.warn("Expired password reset OTP used from IP: {}", ipAddress);
            tokenRepository.delete(resetToken);
            tokenRepository.flush();
            throw new TokenExpiredException(
                    "OTP has expired. Please request a new password reset.");
        }

        // ── Max attempts exceeded — delete and force fresh request
        if (resetToken.getAttempts() >= maxOtpAttempts) {
            logger.warn("Max OTP attempts exceeded for token from IP: {}", ipAddress);
            tokenRepository.delete(resetToken);
            tokenRepository.flush();
            throw new TokenExpiredException(
                    "Too many failed attempts. Please request a new password reset.");
        }

        // Increment attempts on every failed validation
        if (!isPasswordStrong(newPassword)) {
            resetToken.setAttempts(resetToken.getAttempts() + 1);
            tokenRepository.save(resetToken);
            logger.warn("Weak password attempt during reset. Attempts now: {}",
                    resetToken.getAttempts());
            throw new IllegalArgumentException(
                    "Password must be at least 8 characters and contain " +
                            "uppercase, lowercase, and a number.");
        }

        User user = resetToken.getUser();

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            resetToken.setAttempts(resetToken.getAttempts() + 1);
            tokenRepository.save(resetToken);
            throw new IllegalArgumentException(
                    "New password cannot be the same as your current password.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordUpdatedAt(LocalDateTime.now());
        user.unlockAccount(); // clears failed login attempts and lock
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        logRepository.findTopByUserOrderByRequestedAtDesc(user)
                .ifPresent(log -> {
                    log.setCompletedAt(LocalDateTime.now());
                    log.setStatus("COMPLETED");
                    logRepository.save(log);
                });

        logger.info("Password successfully reset for user: {}", user.getEmail());
    }


    @Transactional(readOnly = true)
    public boolean verifyOTP(String otp) {
        if (!isValidOTP(otp)) {
            return false;
        }

        Optional<PasswordResetToken> resetTokenOpt = tokenRepository.findByToken(otp);
        if (resetTokenOpt.isEmpty()) {
            return false;
        }

        PasswordResetToken token = resetTokenOpt.get();

        if (token.isUsed()) {
            return false;
        }

        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            return false;
        }

        if (token.getAttempts() >= maxOtpAttempts) {
            return false;
        }

        return true;
    }

    private String generateOTP() {
        // SecureRandom ensures cryptographically strong randomness
        int otp = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(otp);
    }

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    private boolean isValidOTP(String otp) {
        return otp != null && otp.matches("\\d{6}");
    }

    private boolean isPasswordStrong(String password) {
        if (password == null || password.length() < 8) return false;
        return password.matches(".*[A-Z].*")
                && password.matches(".*[a-z].*")
                && password.matches(".*[0-9].*");
    }
}