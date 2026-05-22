package com.jobportal.v1.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
public class JwtUtils {

    private static final String USER_ID_CLAIM = "userId";
    private static final String ROLES_CLAIM = "roles";
    private static final int MIN_SECRET_LENGTH = 32;

    private final SecretKey signingKey;
    private final long jwtExpirationMs;
    private final long jwtRefreshExpirationMs;

    public JwtUtils(
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${jwt.expiration}") long jwtExpirationMs,
            @Value("${jwt.refresh.expiration}") long jwtRefreshExpirationMs) {

        if (jwtSecret == null || jwtSecret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException("JWT secret must be at least 256 bits (32 characters)");
        }

        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.jwtExpirationMs = jwtExpirationMs;
        this.jwtRefreshExpirationMs = jwtRefreshExpirationMs;
    }

    public String generateJwtToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        return generateTokenFromEmail(
                userPrincipal.getEmail(),
                userPrincipal.getId(),
                userPrincipal.getRoles()
        );
    }

    public String generateTokenFromEmail(String email, Long userId, List<String> roles) {
        return baseTokenBuilder(email, jwtExpirationMs)
                .claim(USER_ID_CLAIM, userId)
                .claim(ROLES_CLAIM, roles)
                .compact();
    }

    public String generateRefreshToken(Long userId) {
        return baseTokenBuilder(String.valueOf(userId), jwtRefreshExpirationMs)
                .compact();
    }

    public String getEmailFromJwtToken(String token) {
        return parseClaims(token).getSubject();
    }

    public Long getUserIdFromToken(String token) {
        return parseClaims(token).get(USER_ID_CLAIM, Long.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        return parseClaims(token).get(ROLES_CLAIM, List.class);
    }

    public boolean validateJwtToken(String authToken) {
        try {
            parseClaims(authToken);
            return true;
        } catch (SecurityException e) {
            log.error("Invalid JWT signature");
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token");
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired");
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported");
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty");
        }

        return false;
    }

    private JwtBuilder baseTokenBuilder(String subject, long expirationMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(signingKey, SignatureAlgorithm.HS256);
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}