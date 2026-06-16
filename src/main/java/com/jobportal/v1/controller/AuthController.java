package com.jobportal.v1.controller;

import com.jobportal.v1.dto.ApiRequest;
import com.jobportal.v1.dto.ApiResponse;
import com.jobportal.v1.dto.security.CurrentUserResponse;
import com.jobportal.v1.dto.security.request.*;
import com.jobportal.v1.dto.security.response.LoginResponse;
import com.jobportal.v1.dto.security.response.SignupResponse;
import com.jobportal.v1.dto.security.response.TokenRefreshResponse;
import com.jobportal.v1.entity.User;
import com.jobportal.v1.security.CurrentUser;
import com.jobportal.v1.security.UserPrincipal;
import com.jobportal.v1.service.AuthService;
import com.jobportal.v1.service.CurrentUserService;
import com.jobportal.v1.service.PasswordResetService;
import com.jobportal.v1.util.ResponseUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Authentication", description = "Authentication APIs")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final CurrentUserService currentUserService;

    @Operation(summary = "Admin Login", description = "Authenticate admin user")
    @PostMapping("/admin/login")
    public ResponseEntity<ApiResponse<LoginResponse>> adminLogin(
            @Valid @RequestBody ApiRequest<LoginRequest> request) {

        LoginResponse response = authService.authenticateAdmin(request.getData());
        return ResponseUtil.ok("Admin login successful", response);
    }

    @Operation(summary = "Agency Login", description = "Authenticate agency user")
    @PostMapping("/agency/login")
    public ResponseEntity<ApiResponse<LoginResponse>> agencyLogin(
            @Valid @RequestBody ApiRequest<LoginRequest> request) {

        LoginResponse response = authService.authenticateAgency(request.getData());
        return ResponseUtil.ok("Agency login successful", response);
    }

    @Operation(summary = "Candidate Login", description = "Authenticate candidate user")
    @PostMapping("/candidate/login")
    public ResponseEntity<ApiResponse<LoginResponse>> candidateLogin(
            @Valid @RequestBody ApiRequest<LoginRequest> request) {

        LoginResponse response = authService.authenticateCandidate(request.getData());
        return ResponseUtil.ok("Candidate login successful", response);
    }

    @Operation(summary = "User Registration", description = "Unified registration for Admin, Agency, and Candidate")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> registerUser(
            @Valid @RequestBody ApiRequest<SignupRequest> request) {

        authService.registerUser(request.getData());

        SignupResponse response = SignupResponse.builder()
                .email(request.getData().getEmail())
                .fullName(request.getData().getFullName())
                .message("Verification code sent to your email. Please check your inbox.")
                .build();

        return ResponseUtil.created("Registration initiated", response);
    }

    @Operation(summary = "Staff Login", description = "Authenticate staff user")
    @PostMapping("/staff/login")
    public ResponseEntity<ApiResponse<LoginResponse>> staffLogin(
            @Valid @RequestBody ApiRequest<LoginRequest> request) {

        LoginResponse response = authService.authenticateStaff(request.getData());
        return ResponseUtil.ok("Staff login successful", response);
    }

    @Operation(summary = "Verify Signup", description = "Verify email OTP to complete registration")
    @PostMapping("/verify-signup")
    public ResponseEntity<ApiResponse<String>> verifySignup(
            @Valid @RequestBody ApiRequest<VerifySignupRequest> request) {

        User user = authService.verifyAndCompleteSignup(request.getData());

        return ResponseUtil.okString(
                String.format("Registration completed successfully! Welcome %s", user.getFullName()),
                "Email verified successfully"
        );
    }

    @Operation(summary = "Resend Verification OTP", description = "Resend verification OTP (2-minute cooldown)")
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<String>> resendVerification(
            @Valid @RequestBody ApiRequest<ResendVerificationRequest> request) {

        authService.resendVerificationOtp(request.getData().getEmail());

        return ResponseUtil.okString(
                "Verification code has been resent to your email.",
                "OTP resent to " + request.getData().getEmail()
        );
    }

    @Operation(summary = "Refresh Token", description = "Get new access token using refresh token")
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refreshToken(
            @Valid @RequestBody ApiRequest<TokenRefreshRequest> request) {

        LoginResponse loginResponse = authService.refreshAccessToken(request.getData().getRefreshToken());

        TokenRefreshResponse response = new TokenRefreshResponse(
                loginResponse.getAccessToken(),
                loginResponse.getRefreshToken()
        );

        return ResponseUtil.ok("Token refreshed successfully", response);
    }

    @Operation(summary = "Logout", description = "Logout user and invalidate refresh token")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(
            @Valid @RequestBody ApiRequest<LogoutRequest> request) {

        authService.logout(request.getData().getRefreshToken());
        return ResponseUtil.okString("Logout successful");
    }

    @Operation(summary = "Change Password", description = "Change password for authenticated user")
    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @Valid @RequestBody ApiRequest<ChangePasswordRequest> request,
            @CurrentUser UserPrincipal userPrincipal) {

        authService.changePassword(request.getData(), userPrincipal.getEmail());
        return ResponseUtil.okString("Password changed successfully");
    }

    @Operation(summary = "Forgot Password", description = "Request password reset OTP")
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(
            @Valid @RequestBody ApiRequest<ForgotPasswordRequest> request,
            HttpServletRequest httpRequest) {

        passwordResetService.requestPasswordReset(
                request.getData().getEmail(),
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent")
        );

        return ResponseUtil.okString("Password reset OTP sent to your email");
    }

    @Operation(summary = "Reset Password", description = "Reset password using OTP")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(
            @Valid @RequestBody ApiRequest<ResetPasswordRequest> request,
            HttpServletRequest httpRequest) {

        passwordResetService.resetPassword(
                request.getData().getToken(),
                request.getData().getNewPassword(),
                getClientIp(httpRequest),
                httpRequest.getHeader("User-Agent")
        );

        return ResponseUtil.okString("Password reset successfully");
    }

    @Operation(summary = "Get Current User", description = "Get currently authenticated user info with role-specific profile")
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> getCurrentUser(
            @CurrentUser UserPrincipal userPrincipal) {

        CurrentUserResponse response = currentUserService.getCurrentUserProfile(userPrincipal.getId());
        return ResponseUtil.ok("Current user retrieved", response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}