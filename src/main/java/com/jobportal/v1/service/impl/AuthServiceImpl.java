package com.jobportal.v1.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jobportal.v1.dto.security.request.*;
import com.jobportal.v1.dto.security.response.LoginResponse;
import com.jobportal.v1.entity.*;
import com.jobportal.v1.enums.*;
import com.jobportal.v1.exception.*;
import com.jobportal.v1.repository.*;
import com.jobportal.v1.security.JwtUtils;
import com.jobportal.v1.service.AuthService;
import com.jobportal.v1.service.EmailService;
import com.jobportal.v1.service.PasswordResetService;
import com.jobportal.v1.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final CandidateRepository candidateRepository;
    private final CandidateStatusRepository candidateStatusRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final EmailService emailService;
    private final PasswordResetService passwordResetService;
    private final HttpServletRequest httpServletRequest;

    @Value("${app.security.registration.otp-expiry-minutes}")
    private int otpExpiryMinutes;

    @Value("${app.security.registration.otp-resend-cooldown-seconds}")
    private int resendCooldownSeconds;

    @Value("${app.security.max-login-attempts}")
    private int maxLoginAttempts;

    @Value("${app.security.registration.max-otp-attempts:5}")
    private int maxResendAttempts;

    @Value("${app.security.account-lock-duration-minutes}")
    private int lockDurationMinutes;

    @Value("${app.test.mode:false}")
    private boolean testMode;

    @Value("${app.test.static-otp:123456}")
    private String staticOtp;

    @Override
    public LoginResponse authenticateAdmin(LoginRequest loginRequest) {
        return authenticate(loginRequest, RoleEnum.ADMIN);
    }

    @Override
    public LoginResponse authenticateAgency(LoginRequest loginRequest) {
        return authenticate(loginRequest, RoleEnum.AGENCY);
    }

    @Override
    public LoginResponse authenticateCandidate(LoginRequest loginRequest) {
        return authenticate(loginRequest, RoleEnum.CANDIDATE);
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
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new BadRequestException("Email is already registered!");
        }

        emailVerificationTokenRepository.findByEmail(signUpRequest.getEmail())
                .ifPresent(token -> {
                    boolean isActive = !token.isExpired()
                            && !token.isUsed()
                            && token.getAttempts() < maxResendAttempts;
                    if (isActive) {
                        throw new BadRequestException(
                                "Verification already sent to your email. " +
                                        "Please check your inbox or use resend.");
                    }
                    emailVerificationTokenRepository.deleteByEmail(signUpRequest.getEmail());
                    emailVerificationTokenRepository.flush();
                });

        String otp = generateOtp();

        try {
            EmailVerificationToken token = new EmailVerificationToken();
            token.setEmail(signUpRequest.getEmail());
            token.setOtp(otp);

            token.setFullName(signUpRequest.getFullName());
            token.setEncodedPassword(passwordEncoder.encode(signUpRequest.getPassword()));

            RoleEnum role = signUpRequest.getRoleEnums().iterator().next();
            token.setRole(role.name());

            token.setExpiryDate(LocalDateTime.now().plusMinutes(otpExpiryMinutes));
            token.setCreatedAt(LocalDateTime.now());
            token.setUpdatedAt(LocalDateTime.now());
            token.setIpAddress(getClientIp());
            token.setUserAgent(httpServletRequest.getHeader("User-Agent"));

            emailVerificationTokenRepository.save(token);

            emailService.sendVerificationEmail(signUpRequest.getEmail(), signUpRequest.getFullName(), otp);

            log.info("Registration OTP sent to: {}", signUpRequest.getEmail());
            if (testMode) {
                log.info("TEST MODE - OTP for {} is: {}", signUpRequest.getEmail(), otp);
            }

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
            User user = new User();
            user.setFullName(token.getFullName());
            user.setEmail(token.getEmail());
            user.setPassword(token.getEncodedPassword());

            Set<RoleEnum> roles = new HashSet<>();
            roles.add(RoleEnum.valueOf(token.getRole()));
            user.setRoles(roles);

            user.setEmailVerified(true);
            user.setEmailVerifiedAt(LocalDateTime.now());
            user.setActive(true);

            if (user.isAdmin() || user.isCandidate()) {
                user.setApprovalStatus(ApprovalStatus.APPROVED);
            } else if (user.isAgency()) {
                user.setApprovalStatus(ApprovalStatus.PENDING);
            }

            User savedUser = userRepository.save(user);

            if (savedUser.isCandidate()) {
                boolean candidateExists = candidateRepository.existsByUserId(savedUser.getId());

                if (!candidateExists) {
                    Candidate candidate = new Candidate();
                    candidate.setUser(savedUser);
                    candidate.setAgency(null);
                    candidate.setFirstName(getFirstNameFromFullName(token.getFullName()));
                    candidate.setLastName(getLastNameFromFullName(token.getFullName()));
                    candidate.setCandidateType(CandidateType.SELF_REGISTERED);
                    candidate.setCreatedByType(CreatedByType.CANDIDATE);
                    candidate.setCreatedBy(savedUser.getId());
                    candidate.setIsEnabled(true);
                    candidateRepository.save(candidate);

                    CandidateStatus status = new CandidateStatus();
                    status.setCandidate(candidate);
                    candidateStatusRepository.save(status);

                    log.info("Candidate profile auto-created for user: {}", savedUser.getEmail());
                }
            }

            token.setUsed(true);
            token.setUsedAt(LocalDateTime.now());
            token.setUpdatedAt(LocalDateTime.now());
            emailVerificationTokenRepository.save(token);

            emailService.sendWelcomeEmail(savedUser);

            log.info("User registered successfully with role: {}", token.getRole());
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
        if (token.isExpired()) {
            emailVerificationTokenRepository.delete(token);
            throw new BadRequestException(
                    "Verification code has expired. Please register again.");
        }
        if (token.getAttempts() >= maxResendAttempts) {
            throw new BadRequestException("Maximum OTP requests exceeded. Please register again.");
        }

        if (!token.canResend(resendCooldownSeconds)) {
            long secondsRemaining = token.getSecondsRemaining(resendCooldownSeconds);
            throw new BadRequestException(String.format("Please wait %d seconds before requesting another verification code", secondsRemaining));
        }

        String newOtp = generateOtp();
        token.setOtp(newOtp);
        token.setExpiryDate(LocalDateTime.now().plusMinutes(otpExpiryMinutes));
        token.setAttempts(token.getAttempts() + 1);
        token.setLastResendAt(LocalDateTime.now());
        token.setUpdatedAt(LocalDateTime.now());
        token.setIpAddress(getClientIp());
        token.setUserAgent(httpServletRequest.getHeader("User-Agent"));

        emailVerificationTokenRepository.save(token);

        emailService.sendVerificationEmail(email, token.getFullName(), newOtp);

        if (testMode) {
            log.info("TEST MODE - Resent OTP for {} is: {}", email, newOtp);
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

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        refreshTokenService.deleteByUserId(user.getId());

        log.info("Password changed successfully for user: {}", email);
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        log.info("Forgot password requested for: {}", request.getEmail());
        String ipAddress = getClientIp();
        String userAgent = httpServletRequest.getHeader("User-Agent");
        passwordResetService.requestPasswordReset(request.getEmail(), ipAddress, userAgent);
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        log.info("Password reset with token");
        String ipAddress = getClientIp();
        String userAgent = httpServletRequest.getHeader("User-Agent");
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword(), ipAddress, userAgent);
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
        return new LoginResponse(accessToken, refreshToken, 900000L);
    }

    private String generateOtp() {
        if (testMode) {
            return staticOtp;
        }
        SecureRandom random = new SecureRandom();
        return String.format("%06d", random.nextInt(1000000));
    }

    private String getClientIp() {
        String xForwardedFor = httpServletRequest.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return httpServletRequest.getRemoteAddr();
    }

    private String getFirstNameFromFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "";
        String[] parts = fullName.trim().split(" ");
        return parts[0];
    }

    private String getLastNameFromFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "";
        String[] parts = fullName.trim().split(" ");
        if (parts.length > 1) {
            return String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
        }
        return "";
    }
}