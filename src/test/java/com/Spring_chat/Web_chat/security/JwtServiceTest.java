package com.Spring_chat.Web_chat.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Pure unit tests for JwtService — no Spring context, no DB.
 * Verifies token generation, claim embedding and validation logic.
 */
class JwtServiceTest {

    // ─── Constants ────────────────────────────────────────────────────────────

    private static final String SECRET =
            "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF";
    private static final long EXPIRATION_MS      = 3_600_000L; // 1h
    private static final long REFRESH_EXPIRY_MS  = 604_800_000L; // 7d

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtConfig config = new JwtConfig();
        config.setSecret(SECRET);
        config.setExpiration(EXPIRATION_MS);
        config.setRefreshExpiration(REFRESH_EXPIRY_MS);
        jwtService = new JwtService(config);
    }

    // ─── generateToken ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("generateToken")
    class GenerateToken {

        @Test
        @DisplayName("should return a non-blank JWT string")
        void shouldReturnNonBlankToken() {
            String token = jwtService.generateToken("alice", 1L, Set.of("ROLE_USER"));
            assertThat(token).isNotBlank();
        }

        @Test
        @DisplayName("should embed correct username as subject and userId as 'uid' claim")
        void shouldEmbedSubjectAndUserId() {
            String token = jwtService.generateToken("alice", 42L, Set.of("ROLE_USER"));
            Claims claims = jwtService.validateAndExtractClaims(token);

            assertThat(claims).isNotNull();
            assertThat(claims.getSubject()).isEqualTo("alice");
            assertThat(claims.get("uid", Long.class)).isEqualTo(42L);
        }

        @Test
        @DisplayName("should embed all provided roles in the 'roles' claim")
        void shouldEmbedAllRoles() {
            Set<String> roles = Set.of("ROLE_USER", "ROLE_ADMIN");
            String token = jwtService.generateToken("alice", 1L, roles);
            Claims claims = jwtService.validateAndExtractClaims(token);

            assertThat(claims).isNotNull();
            @SuppressWarnings("unchecked")
            List<String> embeddedRoles = (List<String>) claims.get("roles");
            assertThat(embeddedRoles).containsExactlyInAnyOrderElementsOf(roles);
        }

        @Test
        @DisplayName("should produce different tokens for different users")
        void shouldProduceDifferentTokensForDifferentUsers() {
            String tokenA = jwtService.generateToken("alice", 1L, Set.of("ROLE_USER"));
            String tokenB = jwtService.generateToken("bob",   2L, Set.of("ROLE_USER"));
            assertThat(tokenA).isNotEqualTo(tokenB);
        }
    }

    // ─── validateAndExtractClaims ─────────────────────────────────────────────

    @Nested
    @DisplayName("validateAndExtractClaims")
    class ValidateAndExtractClaims {

        @Test
        @DisplayName("with a valid token should return the full Claims payload")
        void withValidToken_shouldReturnClaims() {
            String token = jwtService.generateToken("bob", 99L, Set.of("ROLE_USER"));
            Claims claims = jwtService.validateAndExtractClaims(token);

            assertThat(claims).isNotNull();
            assertThat(claims.getSubject()).isEqualTo("bob");
            assertThat(claims.get("uid", Long.class)).isEqualTo(99L);
        }

        @Test
        @DisplayName("with a tampered signature should return null")
        void withTamperedSignature_shouldReturnNull() {
            String token   = jwtService.generateToken("alice", 1L, Set.of("ROLE_USER"));
            String tampered = token.substring(0, token.length() - 4) + "XXXX";

            assertThat(jwtService.validateAndExtractClaims(tampered)).isNull();
        }

        @Test
        @DisplayName("with an already-expired token should return null")
        void withExpiredToken_shouldReturnNull() {
            JwtConfig shortLived = new JwtConfig();
            shortLived.setSecret(SECRET);
            shortLived.setExpiration(-1000L); // expiration in the past
            shortLived.setRefreshExpiration(REFRESH_EXPIRY_MS);
            JwtService expiredService = new JwtService(shortLived);

            String expiredToken = expiredService.generateToken("alice", 1L, Set.of("ROLE_USER"));

            assertThat(jwtService.validateAndExtractClaims(expiredToken)).isNull();
        }

        @Test
        @DisplayName("with a random non-JWT string should return null")
        void withRandomString_shouldReturnNull() {
            assertThat(jwtService.validateAndExtractClaims("not.a.jwt.at.all")).isNull();
        }

        @Test
        @DisplayName("with an empty string should return null")
        void withEmptyString_shouldReturnNull() {
            assertThat(jwtService.validateAndExtractClaims("")).isNull();
        }
    }

    // ─── Expiry helpers ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getAccessTokenExpirationSeconds should return config value / 1000")
    void getAccessTokenExpirationSeconds_shouldMatchConfig() {
        assertThat(jwtService.getAccessTokenExpirationSeconds())
                .isEqualTo(EXPIRATION_MS / 1000);
    }

    @Test
    @DisplayName("getRefreshTokenExpirationMillis should return raw config value")
    void getRefreshTokenExpirationMillis_shouldMatchConfig() {
        assertThat(jwtService.getRefreshTokenExpirationMillis())
                .isEqualTo(REFRESH_EXPIRY_MS);
    }
}
