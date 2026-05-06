package com.jobportal.v1.service.impl;

import com.jobportal.v1.entity.RefreshToken;
import com.jobportal.v1.entity.User;
import com.jobportal.v1.exception.TokenExpiredException;
import com.jobportal.v1.repository.RefreshTokenRepository;
import com.jobportal.v1.repository.UserRepository;
import com.jobportal.v1.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

    @Value("${jwt.refresh.expiration}")
    private Long refreshTokenDurationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Override
    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        // Delete any existing refresh token for this user
        refreshTokenRepository.deleteByUserId(userId);
        refreshTokenRepository.flush();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        refreshToken.setToken(UUID.randomUUID().toString());

        RefreshToken savedToken = refreshTokenRepository.save(refreshToken);
        log.info("Created new refresh token for user ID: {}", userId);
        return savedToken;
    }

    @Override
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            log.warn("Refresh token expired for user ID: {}", token.getUser().getId());
            throw new TokenExpiredException("Refresh token was expired. Please make a new sign-in request");
        }
        return token;
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
        log.info("Deleted refresh token for user ID: {}", userId);
    }

    @Override
    @Transactional
    public void deleteByToken(String token) {
        refreshTokenRepository.deleteByToken(token);
        log.info("Deleted refresh token: {}", token);
    }

    @Override
    public boolean validateRefreshToken(String token) {
        return refreshTokenRepository.existsByToken(token);
    }
}