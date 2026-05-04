package com.jobportal.v1.controller;

import com.jobportal.v1.dto.ApiRequest;
import com.jobportal.v1.dto.ApiResponse;
import com.jobportal.v1.dto.security.request.*;
import com.jobportal.v1.dto.security.response.LoginResponse;
import com.jobportal.v1.dto.security.response.SignupResponse;
import com.jobportal.v1.dto.security.response.TokenRefreshResponse;
import com.jobportal.v1.entity.User;
import com.jobportal.v1.security.CurrentUser;
import com.jobportal.v1.security.UserPrincipal;
import com.jobportal.v1.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "Authentication", description = "Authentication APIs for Admin and Agency")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Admin Login", description = "Authenticate admin user")
    @PostMapping("/admin/login")
    public ResponseEntity<ApiResponse<LoginResponse>> adminLogin(
            @Valid @RequestBody ApiRequest<LoginRequest> request) {

        LoginResponse response = authService.authenticateAdmin(request.getData());
        return ResponseEntity.ok(ApiResponse.success("Admin login successful", response));
    }

    @Operation(summary = "Agency Login", description = "Authenticate agency user")
    @PostMapping("/agency/login")
    public ResponseEntity<ApiResponse<LoginResponse>> agencyLogin(
            @Valid @RequestBody ApiRequest<LoginRequest> request) {

        LoginResponse response = authService.authenticateAgency(request.getData());
        return ResponseEntity.ok(ApiResponse.success("Agency login successful", response));
    }

    @Operation(summary = "User Registration", description = "Register a new user (sends verification OTP)")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> registerUser(
            @Valid @RequestBody ApiRequest<SignupRequest> request) {

        authService.registerUser(request.getData());

        SignupResponse response = SignupResponse.builder()
                .email(request.getData().getEmail())
                .fullName(request.getData().getFullName())
                .message("Verification code sent to your email. Please check your inbox.")
                .build();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration initiated", response));
    }

    @Operation(summary = "Verify Signup", description = "Verify email OTP to complete registration")
    @PostMapping("/verify-signup")
    public ResponseEntity<ApiResponse<String>> verifySignup(
            @Valid @RequestBody ApiRequest<VerifySignupRequest> request) {

        User user = authService.verifyAndCompleteSignup(request.getData());

        return ResponseEntity.ok(ApiResponse.success(
                String.format("Registration completed successfully! Welcome %s", user.getFullName()),
                "Email verified successfully"
        ));
    }

    @Operation(summary = "Resend Verification OTP", description = "Resend verification OTP (2-minute cooldown)")
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<String>> resendVerification(
            @RequestParam String email) {

        authService.resendVerificationOtp(email);

        return ResponseEntity.ok(ApiResponse.success(
                "Verification code has been resent to your email.",
                "OTP resent to " + email
        ));
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

        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    @Operation(summary = "Logout", description = "Logout user and invalidate refresh token")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(
            @RequestParam("refreshToken") String refreshToken) {

        authService.logout(refreshToken);
        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }

    @Operation(summary = "Change Password", description = "Change password for authenticated user")
    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<String>> changePassword(
            @Valid @RequestBody ApiRequest<ChangePasswordRequest> request,
            @CurrentUser UserPrincipal userPrincipal) {

        authService.changePassword(request.getData(), userPrincipal.getEmail());
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    @Operation(summary = "Forgot Password", description = "Request password reset")
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(
            @Valid @RequestBody ApiRequest<ForgotPasswordRequest> request) {

        authService.forgotPassword(request.getData());
        return ResponseEntity.ok(ApiResponse.success("Password reset link sent to your email", null));
    }

    @Operation(summary = "Reset Password", description = "Reset password using token")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(
            @Valid @RequestBody ApiRequest<ResetPasswordRequest> request) {

        authService.resetPassword(request.getData());
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully", null));
    }

    @Operation(summary = "Get Current User", description = "Get currently authenticated user info")
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserPrincipal>> getCurrentUser(
            @CurrentUser UserPrincipal userPrincipal) {

        return ResponseEntity.ok(ApiResponse.success("Current user retrieved", userPrincipal));
    }
}