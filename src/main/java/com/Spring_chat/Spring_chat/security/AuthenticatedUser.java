package com.Spring_chat.Spring_chat.security;

import org.springframework.security.core.GrantedAuthority;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;

/**
 * Lightweight immutable principal populated from JWT claims — no database round-trip per request.
 * Available in controllers via {@code @AuthenticationPrincipal AuthenticatedUser principal}.
 */
public record AuthenticatedUser(
        Long id,
        String username,
        Collection<? extends GrantedAuthority> authorities
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public boolean hasRole(String role) {
        return authorities.stream()
                .anyMatch(a -> a.getAuthority().equals(role));
    }

    public boolean hasPermission(String permission) {
        return authorities.stream()
                .anyMatch(a -> a.getAuthority().equals(permission));
    }
}
