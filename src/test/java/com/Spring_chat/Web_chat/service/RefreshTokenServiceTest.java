package com.Spring_chat.Web_chat.service;

import com.Spring_chat.Web_chat.ENUM.RoleName;
import com.Spring_chat.Web_chat.ENUM.UserStatus;
import com.Spring_chat.Web_chat.dto.auth.LoginResponseDTO;
import com.Spring_chat.Web_chat.entity.RefreshToken;
import com.Spring_chat.Web_chat.entity.Role;
import com.Spring_chat.Web_chat.entity.User;
import com.Spring_chat.Web_chat.repository.RefreshTokenRepository;
import com.Spring_chat.Web_chat.security.JwtService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for RefreshTokenService.
 *
 * Key scenarios:
 *  - Token rotation (happy path)
 *  - Reuse detection: revoked token triggers family revocation (theft guard)
 *  - Expired token handling
 *  - Inactive user gate during rotation
 *  - issueTokenPair for ACTIVE vs non-ACTIVE users
 *
 * NOTE: Since hashToken() is a private-static implementation detail, the helper
 * {@code sha256Base64()} below replicates the same algorithm to construct
 * the correct hash that the repository mock will be queried with.
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtService             jwtService;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    // ─── Test fixtures ────────────────────────────────────────────────────────

    private static final String IP    = "10.0.0.1";
    private static final String AGENT = "UnitTestAgent/1.0";
    private static final long   REFRESH_EXPIRY_MS = 604_800_000L;
    private static final long   ACCESS_EXPIRY_SEC = 3600L;

    private final Role userRole  = Role.builder().id(1L).name(RoleName.ROLE_USER).build();
    private final User activeUser = User.builder()
            .id(5L).username("alice").status(UserStatus.ACTIVE).roles(Set.of(userRole)).build();

    // ─── rotateRefreshTokenAndIssueAccessToken ────────────────────────────────

    @Nested
    @DisplayName("rotateRefreshTokenAndIssueAccessToken")
    class RotateRefreshToken {

        private final String rawToken  = "some-raw-refresh-token-64-bytes-long-abcdefghijklmnopqrstuvwxyz";
        private final String tokenHash = sha256Base64(rawToken.trim());

        @Test
        @DisplayName("happy path — valid, active token should be revoked and new tokens returned")
        void withValidToken_shouldRotateAndReturnNewTokenPair() {
            RefreshToken current = RefreshToken.builder()
                    .id(1L)
                    .tokenHash(tokenHash)
                    .user(activeUser)
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .revokedAt(null)
                    .build();

            given(refreshTokenRepository.findByTokenHashForUpdate(tokenHash))
                    .willReturn(Optional.of(current));
            given(jwtService.getRefreshTokenExpirationMillis()).willReturn(REFRESH_EXPIRY_MS);
            given(jwtService.getAccessTokenExpirationSeconds()).willReturn(ACCESS_EXPIRY_SEC);
            given(jwtService.generateToken(eq("alice"), eq(5L), any()))
                    .willReturn("new-access-token");

            LoginResponseDTO result = refreshTokenService
                    .rotateRefreshTokenAndIssueAccessToken(rawToken, IP, AGENT);

            assertThat(result.getAccess_token()).isEqualTo("new-access-token");
            assertThat(result.getRefresh_token()).isNotBlank()
                    .isNotEqualTo(rawToken); // new token must differ
            assertThat(result.getToken_type()).isEqualTo("Bearer");

            // Old token must be revoked
            assertThat(current.getRevokedAt()).isNotNull();
            assertThat(current.getReplacedByTokenHash()).isNotBlank();

            // New token must be persisted
            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            then(refreshTokenRepository).should(atLeast(1)).save(captor.capture());
        }

        @Test
        @DisplayName("reuse of a revoked token should revoke all family tokens (token theft detection)")
        void withRevokedToken_shouldRevokeAllFamilyAndThrow() {
            RefreshToken revoked = RefreshToken.builder()
                    .id(2L)
                    .tokenHash(tokenHash)
                    .user(activeUser)
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .revokedAt(Instant.now().minusSeconds(100)) // already revoked
                    .build();

            given(refreshTokenRepository.findByTokenHashForUpdate(tokenHash))
                    .willReturn(Optional.of(revoked));

            assertThatThrownBy(() -> refreshTokenService
                    .rotateRefreshTokenAndIssueAccessToken(rawToken, IP, AGENT))
                    .isInstanceOf(InvalidRefreshTokenException.class)
                    .hasMessageContaining("already used");

            // Family revocation must be triggered
            then(refreshTokenRepository).should()
                    .revokeAllActiveByUserId(eq(activeUser.getId()), any(), any());
        }

        @Test
        @DisplayName("expired token should be revoked and throw InvalidRefreshTokenException")
        void withExpiredToken_shouldRevokeAndThrow() {
            RefreshToken expired = RefreshToken.builder()
                    .id(3L)
                    .tokenHash(tokenHash)
                    .user(activeUser)
                    .expiresAt(Instant.now().minusSeconds(10)) // past
                    .revokedAt(null)
                    .build();

            given(refreshTokenRepository.findByTokenHashForUpdate(tokenHash))
                    .willReturn(Optional.of(expired));

            assertThatThrownBy(() -> refreshTokenService
                    .rotateRefreshTokenAndIssueAccessToken(rawToken, IP, AGENT))
                    .isInstanceOf(InvalidRefreshTokenException.class)
                    .hasMessageContaining("expired");

            // Token should have been revoked
            then(refreshTokenRepository).should().save(expired);
            assertThat(expired.getRevokedAt()).isNotNull();
        }

        @Test
        @DisplayName("token belonging to a non-ACTIVE user should revoke all tokens and throw")
        void withInactiveUserToken_shouldRevokeAllAndThrow() {
            User inactiveUser = User.builder()
                    .id(6L).username("bob").status(UserStatus.BANNED).roles(Set.of(userRole)).build();

            RefreshToken token = RefreshToken.builder()
                    .id(4L)
                    .tokenHash(tokenHash)
                    .user(inactiveUser)
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .revokedAt(null)
                    .build();

            given(refreshTokenRepository.findByTokenHashForUpdate(tokenHash))
                    .willReturn(Optional.of(token));

            assertThatThrownBy(() -> refreshTokenService
                    .rotateRefreshTokenAndIssueAccessToken(rawToken, IP, AGENT))
                    .isInstanceOf(InvalidRefreshTokenException.class);

            then(refreshTokenRepository).should()
                    .revokeAllActiveByUserId(eq(inactiveUser.getId()), any(), any());
        }

        @Test
        @DisplayName("token not found in DB should throw InvalidRefreshTokenException")
        void withUnknownToken_shouldThrow() {
            given(refreshTokenRepository.findByTokenHashForUpdate(tokenHash))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> refreshTokenService
                    .rotateRefreshTokenAndIssueAccessToken(rawToken, IP, AGENT))
                    .isInstanceOf(InvalidRefreshTokenException.class);
        }

        @Test
        @DisplayName("null raw token should throw InvalidRefreshTokenException immediately")
        void withNullToken_shouldThrow() {
            assertThatThrownBy(() -> refreshTokenService
                    .rotateRefreshTokenAndIssueAccessToken(null, IP, AGENT))
                    .isInstanceOf(InvalidRefreshTokenException.class);

            then(refreshTokenRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("blank raw token (whitespace only) should throw InvalidRefreshTokenException")
        void withBlankToken_shouldThrow() {
            assertThatThrownBy(() -> refreshTokenService
                    .rotateRefreshTokenAndIssueAccessToken("   ", IP, AGENT))
                    .isInstanceOf(InvalidRefreshTokenException.class);

            then(refreshTokenRepository).shouldHaveNoInteractions();
        }
    }

    // ─── issueTokenPair ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("issueTokenPair")
    class IssueTokenPair {

        @Test
        @DisplayName("ACTIVE user should receive a valid access token and a non-blank refresh token")
        void withActiveUser_shouldReturnTokenPair() {
            given(jwtService.getRefreshTokenExpirationMillis()).willReturn(REFRESH_EXPIRY_MS);
            given(jwtService.getAccessTokenExpirationSeconds()).willReturn(ACCESS_EXPIRY_SEC);
            given(jwtService.generateToken(eq("alice"), eq(5L), any()))
                    .willReturn("new-access-token");

            LoginResponseDTO result = refreshTokenService.issueTokenPair(activeUser, IP, AGENT);

            assertThat(result.getAccess_token()).isEqualTo("new-access-token");
            assertThat(result.getRefresh_token()).isNotBlank();
            assertThat(result.getToken_type()).isEqualTo("Bearer");

            // Refresh token must be persisted
            ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
            then(refreshTokenRepository).should().save(captor.capture());
            RefreshToken saved = captor.getValue();
            assertThat(saved.getUser()).isEqualTo(activeUser);
            assertThat(saved.getExpiresAt()).isAfter(Instant.now());
        }

        @Test
        @DisplayName("non-ACTIVE (BANNED) user should throw before any token is saved")
        void withBannedUser_shouldThrowAndNeverSave() {
            User banned = User.builder()
                    .id(7L).username("carol").status(UserStatus.BANNED).roles(Set.of(userRole)).build();

            assertThatThrownBy(() -> refreshTokenService.issueTokenPair(banned, IP, AGENT))
                    .isInstanceOf(InvalidRefreshTokenException.class);

            then(refreshTokenRepository).shouldHaveNoInteractions();
        }
    }

    // ─── revokeAllActiveTokensForUser ─────────────────────────────────────────

    @Test
    @DisplayName("revokeAllActiveTokensForUser should delegate to repository and return count")
    void revokeAll_shouldDelegateToRepositoryAndReturnCount() {
        given(refreshTokenRepository.revokeAllActiveByUserId(eq(5L), any(), any()))
                .willReturn(3);

        int count = refreshTokenService.revokeAllActiveTokensForUser(5L, Instant.now());

        assertThat(count).isEqualTo(3);
        then(refreshTokenRepository).should().revokeAllActiveByUserId(eq(5L), any(), any());
    }

    // ─── Helper: replicates private RefreshTokenService.hashToken() ───────────

    private static String sha256Base64(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
