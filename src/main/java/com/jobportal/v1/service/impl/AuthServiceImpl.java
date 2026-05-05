package com.jobportal.v1.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobportal.v1.dto.security.request.*;
import com.jobportal.v1.dto.security.response.LoginResponse;
import com.jobportal.v1.entity.EmailVerificationToken;
import com.jobportal.v1.entity.RefreshToken;
import com.jobportal.v1.entity.User;
import com.jobportal.v1.enums.RoleEnum;
import com.jobportal.v1.exception.*;
import com.jobportal.v1.repository.EmailVerificationTokenRepository;
import com.jobportal.v1.repository.UserRepository;
import com.jobportal.v1.security.JwtUtils;
import com.jobportal.v1.service.AuthService;
import com.jobportal.v1.service.RefreshTokenService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;
    private final HttpServletRequest httpServletRequest;

    @Value("${app.otp.expiry-minutes}")
    private int otpExpiryMinutes;

    @Value("${app.otp.resend-cooldown-seconds}")
    private int resendCooldownSeconds;

    @Value("${app.max-login-attempts}")
    private int maxLoginAttempts;

    @Value("${app.lock-duration-minutes}")
    private int lockDurationMinutes;

    @Override
    public LoginResponse authenticateAdmin(LoginRequest loginRequest) {
        return authenticate(loginRequest, RoleEnum.ADMIN);
    }

    @Override
    public LoginResponse authenticateAgency(LoginRequest loginRequest) {
        return authenticate(loginRequest, RoleEnum.AGENCY);
    }

    private LoginResponse authenticate(LoginRequest loginRequest, RoleEnum expectedRole) {
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (user.isAccountLocked()) {
            throw new BadCredentialsException("Account is locked. Please try again later.");
        }

        if (!user.hasRole(expectedRole)) {
            throw new BadCredentialsException("Access denied. You don't have " + expectedRole + " privileges.");
        }

        if (!user.isEmailVerified()) {
            throw new BadCredentialsException("Please verify your email before logging in.");
        }

        if (expectedRole == RoleEnum.AGENCY) {
            if (user.isPending()) {
                throw new AgencyPendingException("Your account is pending admin approval. You will be notified once approved.");
            }
            if (user.isRejected()) {
                throw new AgencyRejectedException("Your account has been rejected. Reason: " + user.getRejectionReason());
            }
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            user.recordLogin();
            userRepository.save(user);

            String accessToken = jwtUtils.generateJwtToken(authentication);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

            return buildLoginResponse(user, accessToken, refreshToken.getToken());

        } catch (BadCredentialsException e) {
            user.recordFailedLogin(maxLoginAttempts, lockDurationMinutes);
            userRepository.save(user);
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    @Override
    @Transactional
    public void registerUser(SignupRequest signUpRequest) {
        // Check if user already exists
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new BadRequestException("Email is already registered!");
        }

        // Check if there's a pending verification
        emailVerificationTokenRepository.findByEmail(signUpRequest.getEmail())
                .ifPresent(token -> {
                    if (!token.isExpired() && !token.isUsed()) {
                        throw new BadRequestException("Verification already sent to your email. Please check your inbox or wait for cooldown.");
                    }
                    // Delete expired/unused token
                    emailVerificationTokenRepository.deleteByEmail(signUpRequest.getEmail());
                });

        // Generate OTP
        String otp = generateOtp();

        // Store user data in verification token
        try {
            String userDataJson = objectMapper.writeValueAsString(signUpRequest);

            EmailVerificationToken token = new EmailVerificationToken();
            token.setEmail(signUpRequest.getEmail());
            token.setOtp(otp);
            token.setUserData(userDataJson);
            token.setExpiryDate(LocalDateTime.now().plusMinutes(otpExpiryMinutes));
            token.setCreatedAt(LocalDateTime.now());
            token.setUpdatedAt(LocalDateTime.now());

            // ADD THESE LINES - Get IP and User Agent
            token.setIpAddress(getClientIp());
            token.setUserAgent(httpServletRequest.getHeader("User-Agent"));

            emailVerificationTokenRepository.save(token);

            // Send OTP email
            sendVerificationEmail(signUpRequest.getEmail(), signUpRequest.getFullName(), otp);

            log.info("Registration OTP sent to: {}", signUpRequest.getEmail());

        } catch (Exception e) {
            log.error("Error during registration: {}", e.getMessage());
            throw new RuntimeException("Failed to process registration", e);
        }
    }

    @Override
    @Transactional
    public User verifyAndCompleteSignup(VerifySignupRequest verifyRequest) {
        EmailVerificationToken token = emailVerificationTokenRepository
                .findByEmailAndOtp(verifyRequest.getEmail(), verifyRequest.getOtp())
                .orElseThrow(() -> new BadRequestException("Invalid verification code"));

        if (token.isUsed()) {
            throw new BadRequestException("Verification code has already been used");
        }

        if (token.isExpired()) {
            emailVerificationTokenRepository.delete(token);
            throw new BadRequestException("Verification code has expired. Please request a new one.");
        }

        try {
            SignupRequest signUpRequest = objectMapper.readValue(token.getUserData(), SignupRequest.class);

            User user = new User();
            user.setFullName(signUpRequest.getFullName());
            user.setEmail(signUpRequest.getEmail());
            user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));

            user.setRoles(signUpRequest.getRoleEnums());

            user.verifyEmail();

            User savedUser = userRepository.save(user);

            token.setUsed(true);
            token.setUpdatedAt(LocalDateTime.now());
            emailVerificationTokenRepository.save(token);

            log.info("User registered successfully with roles: {}", signUpRequest.getRoleEnums());
            return savedUser;

        } catch (Exception e) {
            log.error("Error completing registration: {}", e.getMessage());
            throw new RuntimeException("Failed to complete registration", e);
        }
    }

    @Override
    @Transactional
    public void resendVerificationOtp(String email) {
        EmailVerificationToken token = emailVerificationTokenRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No pending registration found for this email"));

        if (token.isUsed()) {
            throw new BadRequestException("This email is already verified");
        }

        if (!token.canResend(resendCooldownSeconds)) {
            throw new BadRequestException("Please wait before requesting another verification code");
        }

        // Generate new OTP
        String newOtp = generateOtp();
        token.setOtp(newOtp);
        token.setExpiryDate(LocalDateTime.now().plusMinutes(otpExpiryMinutes));
        token.setAttempts(token.getAttempts() + 1);
        token.setLastResendAt(LocalDateTime.now());
        token.setUpdatedAt(LocalDateTime.now());

        // ADD THESE LINES - Update IP and User Agent
        token.setIpAddress(getClientIp());
        token.setUserAgent(httpServletRequest.getHeader("User-Agent"));

        emailVerificationTokenRepository.save(token);

        // Get user data to send name in email
        try {
            SignupRequest signUpRequest = objectMapper.readValue(token.getUserData(), SignupRequest.class);
            sendVerificationEmail(email, signUpRequest.getFullName(), newOtp);
        } catch (Exception e) {
            log.error("Error resending OTP: {}", e.getMessage());
            throw new RuntimeException("Failed to resend verification code", e);
        }

        log.info("Resent verification OTP to: {}", email);
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.deleteByToken(refreshToken);
        log.info("User logged out successfully");
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // Invalidate all refresh tokens for this user
        refreshTokenService.deleteByUserId(user.getId());

        log.info("Password changed successfully for user: {}", email);
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        // Implementation similar to your existing one
        log.info("Forgot password requested for: {}", request.getEmail());
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        // Implementation similar to your existing one
        log.info("Password reset for user");
    }

    @Override
    public LoginResponse refreshAccessToken(String refreshToken) {
        RefreshToken token = refreshTokenService.findByToken(refreshToken)
                .orElseThrow(() -> new TokenNotFoundException("Invalid refresh token"));

        refreshTokenService.verifyExpiration(token);

        User user = token.getUser();

        String newAccessToken = jwtUtils.generateTokenFromEmail(user.getEmail(), user.getId(),
                user.getRoles().stream().map(Enum::name).toList());

        return buildLoginResponse(user, newAccessToken, refreshToken);
    }

    private LoginResponse buildLoginResponse(User user, String accessToken, String refreshToken) {
        return new LoginResponse(
                accessToken,
                refreshToken,
                900000L
        );
    }

    private String generateOtp() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }

    private void sendVerificationEmail(String email, String fullName, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(email);
            helper.setSubject("Verify Your Email - Agency Management System");

            String htmlContent = String.format("""
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <style>
                            body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                            .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                            .header { background-color: #4F46E5; color: white; padding: 20px; text-align: center; }
                            .content { padding: 20px; background-color: #f9fafb; }
                            .otp { font-size: 32px; font-weight: bold; color: #4F46E5; text-align: center; padding: 20px; letter-spacing: 5px; }
                            .footer { text-align: center; padding: 20px; font-size: 12px; color: #6b7280; }
                        </style>
                    </head>
                    <body>
                        <div class="container">
                            <div class="header">
                                <h2>Email Verification</h2>
                            </div>
                            <div class="content">
                                <p>Dear %s,</p>
                                <p>Thank you for registering with Agency Management System. Please use the verification code below to complete your registration:</p>
                                <div class="otp">%s</div>
                                <p>This code will expire in 10 minutes.</p>
                                <p>If you didn't request this, please ignore this email.</p>
                            </div>
                            <div class="footer">
                                <p>&copy; 2024 Agency Management System. All rights reserved.</p>
                            </div>
                        </div>
                    </body>
                    </html>
                    """, fullName, otp);

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Verification email sent to: {}", email);
        } catch (MessagingException e) {
            log.error("Failed to send verification email: {}", e.getMessage());
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    private String getClientIp() {
        String xForwardedFor = httpServletRequest.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return httpServletRequest.getRemoteAddr();
    }
}