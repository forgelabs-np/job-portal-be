package com.jobportal.v1.service;

import com.jobportal.v1.entity.RefreshToken;

import java.util.Optional;

public interface RefreshTokenService {
    Optional<RefreshToken> findByToken(String token);
    RefreshToken createRefreshToken(Long userId);
    RefreshToken verifyExpiration(RefreshToken token);
    void deleteByUserId(Long userId);
    void deleteByToken(String token);
    boolean validateRefreshToken(String token);
}