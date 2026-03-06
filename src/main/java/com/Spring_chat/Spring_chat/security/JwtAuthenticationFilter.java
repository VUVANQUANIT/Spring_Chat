package com.Spring_chat.Spring_chat.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Stateless JWT filter — reconstructs the {@link AuthenticatedUser} principal from JWT claims
 * on every request without any database interaction.
 *
 * <p>The JWT payload is expected to contain:
 * <ul>
 *   <li>{@code sub}   – username</li>
 *   <li>{@code uid}   – user ID (Long)</li>
 *   <li>{@code roles} – list of role names, e.g. {@code ["ROLE_ADMIN", "ROLE_USER"]}</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        Claims claims = jwtService.validateAndExtractClaims(token);
        if (claims == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String username = claims.getSubject();
        Long userId = claims.get("uid", Long.class);
        if (username == null || userId == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Collection<GrantedAuthority> authorities = extractAuthorities(claims);

        AuthenticatedUser principal = new AuthenticatedUser(userId, username, authorities);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractAuthorities(Claims claims) {
        Object rawRoles = claims.get("roles");
        if (!(rawRoles instanceof List<?> roleList)) {
            return Collections.emptyList();
        }
        return roleList.stream()
                .filter(r -> r instanceof String)
                .map(r -> (GrantedAuthority) new SimpleGrantedAuthority((String) r))
                .collect(Collectors.toList());
    }
}
