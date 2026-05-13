package com.jobportal.v1.service;

import com.jobportal.v1.dto.security.CurrentUserResponse;

public interface CurrentUserService {
    CurrentUserResponse getCurrentUserProfile(Long userId);
}