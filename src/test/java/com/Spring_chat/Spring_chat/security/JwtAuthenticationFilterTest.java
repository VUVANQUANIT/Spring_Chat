package com.Spring_chat.Spring_chat.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for JwtAuthenticationFilter.
 *
 * Tests the filter's behaviour in isolation:
 *  - correct cases (valid token → SecurityContext populated)
 *  - incorrect/missing token → request passes through unauthenticated
 *  - idempotency when auth is already set
 *
 * No Spring context is started; MockHttpServletRequest/Response replace real HTTP objects.
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ─── No auth header ───────────────────────────────────────────────────────

    @Test
    @DisplayName("request without Authorization header should pass through without setting auth")
    void withoutAuthorizationHeader_shouldPassThrough() throws Exception {
        MockHttpServletRequest  request  = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        then(jwtService).shouldHaveNoInteractions();
        then(chain).should().doFilter(request, response);
    }

    @Test
    @DisplayName("request with non-Bearer Authorization header should pass through without auth")
    void withNonBearerHeader_shouldPassThrough() throws Exception {
        MockHttpServletRequest  request  = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        then(jwtService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("request with blank bearer token should pass through without auth")
    void withBlankBearerToken_shouldPassThrough() throws Exception {
        MockHttpServletRequest  request  = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer   ");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        then(jwtService).shouldHaveNoInteractions();
    }

    // ─── Invalid / expired token ──────────────────────────────────────────────

    @Test
    @DisplayName("request with invalid JWT should pass through without setting auth")
    void withInvalidToken_shouldPassThrough() throws Exception {
        given(jwtService.validateAndExtractClaims("bad-token")).willReturn(null);

        MockHttpServletRequest  request  = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        then(chain).should().doFilter(request, response);
    }

    // ─── Valid token ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("request with a valid token should populate SecurityContext with AuthenticatedUser")
    void withValidToken_shouldSetAuthenticationInContext() throws Exception {
        Claims claims = mock(Claims.class);
        given(claims.getSubject()).willReturn("alice");
        given(claims.get("uid", Long.class)).willReturn(7L);
        given(claims.get("roles")).willReturn(List.of("ROLE_USER"));
        given(jwtService.validateAndExtractClaims("valid-token")).willReturn(claims);

        MockHttpServletRequest  request  = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isInstanceOf(AuthenticatedUser.class);

        AuthenticatedUser principal = (AuthenticatedUser) auth.getPrincipal();
        assertThat(principal.id()).isEqualTo(7L);
        assertThat(principal.username()).isEqualTo("alice");
        assertThat(auth.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");

        then(chain).should().doFilter(request, response);
    }

    @Test
    @DisplayName("token with missing uid claim should pass through without auth")
    void withTokenMissingUid_shouldPassThrough() throws Exception {
        Claims claims = mock(Claims.class);
        given(claims.getSubject()).willReturn("alice");
        given(claims.get("uid", Long.class)).willReturn(null); // uid missing
        given(jwtService.validateAndExtractClaims("partial-token")).willReturn(claims);

        MockHttpServletRequest  request  = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer partial-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    // ─── Idempotency ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("when SecurityContext already has an authentication, filter should not call JwtService")
    void whenAuthAlreadyPresent_shouldSkipValidation() throws Exception {
        Authentication existingAuth = mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        MockHttpServletRequest  request  = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer some-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        then(jwtService).shouldHaveNoInteractions();
        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isSameAs(existingAuth);
    }
}
