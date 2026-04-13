package com.Spring_chat.Web_chat.service;

import com.Spring_chat.Web_chat.enums.UserStatus;
import com.Spring_chat.Web_chat.dto.auth.LoginResponseDTO;
import com.Spring_chat.Web_chat.entity.RefreshToken;
import com.Spring_chat.Web_chat.entity.User;
import com.Spring_chat.Web_chat.repository.RefreshTokenRepository;
import com.Spring_chat.Web_chat.security.JwtService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int REFRESH_TOKEN_BYTES = 64;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Transactional
    public LoginResponseDTO rotateRefreshTokenAndIssueAccessToken(String rawRefreshToken,
                                                                  String clientIp,
                                                                  String userAgent) {
        String normalizedToken = normalizeToken(rawRefreshToken);
        String tokenHash = hashToken(normalizedToken);
        Instant now = Instant.now();

        RefreshToken currentToken = refreshTokenRepository.findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token is invalid"));

        if (currentToken.isRevoked()) {
            revokeAllActiveTokensForUser(currentToken.getUser().getId(), now);
            throw new InvalidRefreshTokenException("Refresh token was already used");
        }

        if (currentToken.isExpired(now)) {
            currentToken.setRevokedAt(now);
            refreshTokenRepository.save(currentToken);
            throw new InvalidRefreshTokenException("Refresh token is expired");
        }

        User user = currentToken.getUser();
        if (user.getStatus() != UserStatus.ACTIVE) {
            revokeAllActiveTokensForUser(user.getId(), now);
            throw new InvalidRefreshTokenException("User is not allowed to refresh token");
        }

        String nextRawRefreshToken = generateRawRefreshToken();
        String nextRefreshTokenHash = hashToken(nextRawRefreshToken);

        RefreshToken nextToken = RefreshToken.builder()
                .user(user)
                .tokenHash(nextRefreshTokenHash)
                .expiresAt(now.plusMillis(jwtService.getRefreshTokenExpirationMillis()))
                .createdByIp(limitLength(clientIp, 45))
                .userAgent(limitLength(userAgent, 255))
                .build();
        refreshTokenRepository.save(nextToken);

        currentToken.setRevokedAt(now);
        currentToken.setReplacedByTokenHash(nextRefreshTokenHash);
        refreshTokenRepository.save(currentToken);

        Set<String> roleNames = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(java.util.stream.Collectors.toSet());
        String accessToken = jwtService.generateToken(user.getUsername(), user.getId(), roleNames);
        return new LoginResponseDTO(
                accessToken,
                nextRawRefreshToken,
                String.valueOf(jwtService.getAccessTokenExpirationSeconds())
        );
    }

    @Transactional
    public LoginResponseDTO issueTokenPair(User user, String clientIp, String userAgent) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new InvalidRefreshTokenException("User is not active");
        }
        Instant now = Instant.now();
        String rawRefreshToken = generateRawRefreshToken();
        String refreshTokenHash = hashToken(rawRefreshToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(refreshTokenHash)
                .expiresAt(now.plusMillis(jwtService.getRefreshTokenExpirationMillis()))
                .createdByIp(limitLength(clientIp, 45))
                .userAgent(limitLength(userAgent, 255))
                .build();
        refreshTokenRepository.save(refreshToken);

        Set<String> roleNames = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(java.util.stream.Collectors.toSet());
        String accessToken = jwtService.generateToken(user.getUsername(), user.getId(), roleNames);
        return new LoginResponseDTO(
                accessToken,
                rawRefreshToken,
                String.valueOf(jwtService.getAccessTokenExpirationSeconds())
        );
    }

    @Transactional
    public int revokeAllActiveTokensForUser(Long userId, Instant now) {
        return refreshTokenRepository.revokeAllActiveByUserId(userId, now, now);
    }

    private static String generateRawRefreshToken() {
        byte[] randomBytes = new byte[REFRESH_TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private static String normalizeToken(String rawToken) {
        if (rawToken == null) {
            throw new InvalidRefreshTokenException("Refresh token is required");
        }
        String normalized = rawToken.trim();
        if (normalized.isEmpty()) {
            throw new InvalidRefreshTokenException("Refresh token is required");
        }
        return normalized;
    }

    private static String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }

    private static String limitLength(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
