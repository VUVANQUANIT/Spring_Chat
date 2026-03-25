package com.Spring_chat.Spring_chat.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure Java unit tests for RefreshToken entity helper methods.
 * No Spring context, no Mockito — just the domain object behaviour.
 */
class RefreshTokenTest {

    // ─── isRevoked ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isRevoked")
    class IsRevoked {

        @Test
        @DisplayName("returns false when revokedAt is null (token is still active)")
        void whenRevokedAtIsNull_shouldReturnFalse() {
            RefreshToken token = RefreshToken.builder().revokedAt(null).build();
            assertThat(token.isRevoked()).isFalse();
        }

        @Test
        @DisplayName("returns true when revokedAt is set")
        void whenRevokedAtIsSet_shouldReturnTrue() {
            RefreshToken token = RefreshToken.builder()
                    .revokedAt(Instant.now())
                    .build();
            assertThat(token.isRevoked()).isTrue();
        }
    }

    // ─── isExpired ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isExpired")
    class IsExpired {

        @Test
        @DisplayName("returns false when expiresAt is in the future")
        void whenExpiresAtIsInFuture_shouldReturnFalse() {
            RefreshToken token = RefreshToken.builder()
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();
            assertThat(token.isExpired(Instant.now())).isFalse();
        }

        @Test
        @DisplayName("returns true when expiresAt is in the past")
        void whenExpiresAtIsInPast_shouldReturnTrue() {
            RefreshToken token = RefreshToken.builder()
                    .expiresAt(Instant.now().minusSeconds(1))
                    .build();
            assertThat(token.isExpired(Instant.now())).isTrue();
        }

        @Test
        @DisplayName("returns false when expiresAt is null (treated as non-expiring)")
        void whenExpiresAtIsNull_shouldReturnFalse() {
            RefreshToken token = RefreshToken.builder().expiresAt(null).build();
            assertThat(token.isExpired(Instant.now())).isFalse();
        }
    }
}
