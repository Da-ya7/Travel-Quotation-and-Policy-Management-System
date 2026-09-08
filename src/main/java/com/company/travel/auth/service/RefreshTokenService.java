package com.company.travel.auth.service;

import com.company.travel.auth.entity.RefreshToken;
import com.company.travel.auth.repository.RefreshTokenRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    private static final long REFRESH_TOKEN_EXPIRATION_DAYS = 30;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository) {

        this.refreshTokenRepository = refreshTokenRepository;
    }

    public String createRefreshToken(Long userId) {

        byte[] randomBytes = new byte[64];
        secureRandom.nextBytes(randomBytes);

        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        RefreshToken refreshToken = new RefreshToken();

        refreshToken.setUserId(userId);
        refreshToken.setTokenHash(hashToken(rawToken));
        refreshToken.setCreatedAt(LocalDateTime.now());
        refreshToken.setExpiresAt(
                LocalDateTime.now()
                        .plusDays(REFRESH_TOKEN_EXPIRATION_DAYS));

        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    public RefreshToken validateRefreshToken(String rawToken) {

        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid refresh token"));

        if (refreshToken.getRevokedAt() != null) {
            throw new IllegalArgumentException(
                    "Refresh token has been revoked");
        }

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "Refresh token has expired");
        }

        return refreshToken;
    }

    private String hashToken(String token) {

        try {

            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder()
                    .encodeToString(hash);

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Unable to hash refresh token",
                    exception);
        }
    }

    public void revokeRefreshToken(RefreshToken refreshToken) {
        refreshToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);
    }
}