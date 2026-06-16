package com.jobportal.v1.service;

import com.jobportal.v1.dto.candidate.request.CandidateSignupRequest;
import com.jobportal.v1.dto.security.request.*;
import com.jobportal.v1.dto.security.response.LoginResponse;
import com.jobportal.v1.entity.User;

public interface AuthService {

    LoginResponse authenticateAdmin(LoginRequest loginRequest);

    LoginResponse authenticateAgency(LoginRequest loginRequest);

    LoginResponse authenticateCandidate(LoginRequest loginRequest);  // NEW

    void registerUser(SignupRequest signUpRequest);

    LoginResponse authenticateStaff(LoginRequest request);

    User verifyAndCompleteSignup(VerifySignupRequest verifyRequest);

    void resendVerificationOtp(String email);

    void logout(String refreshToken);

    void changePassword(ChangePasswordRequest request, String email);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    LoginResponse refreshAccessToken(String refreshToken);
}