package com.Spring_chat.Web_chat.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {

    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_USER_ID = "uid";

    private final JwtConfig jwtConfig;

    private SecretKey getSingingKey() {
        byte[] keyBytes = jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Issues a signed access token embedding the user's ID and role names.
     * Roles are stored as a {@code List<String>} claim so the filter can restore
     * {@link org.springframework.security.core.GrantedAuthority} objects without a DB hit.
     */
    public String generateToken(String username, Long userId, Collection<String> roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtConfig.getExpiration());
        return Jwts.builder()
                .subject(username)
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_ROLES, List.copyOf(roles))
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSingingKey())
                .compact();
    }

    /**
     * Validates the token signature and expiry, then returns the full {@link Claims} payload.
     * Returns {@code null} on any failure (expired, tampered, malformed).
     */
    public Claims validateAndExtractClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSingingKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return null;
        }
    }

    public long getAccessTokenExpirationSeconds() {
        return jwtConfig.getExpiration() / 1000;
    }

    public long getRefreshTokenExpirationMillis() {
        return jwtConfig.getRefreshExpiration();
    }
}
