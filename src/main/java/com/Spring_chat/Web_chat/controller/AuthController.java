package com.Spring_chat.Web_chat.controller;

import com.Spring_chat.Web_chat.dto.auth.LoginRequestDTO;
import com.Spring_chat.Web_chat.dto.auth.LoginResponseDTO;
import com.Spring_chat.Web_chat.dto.auth.RefreshRequestDTO;
import com.Spring_chat.Web_chat.dto.auth.RegisterRequestDTO;
import com.Spring_chat.Web_chat.security.AuthenticatedUser;
import com.Spring_chat.Web_chat.service.AuthService;
import com.Spring_chat.Web_chat.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    // ─── Register ─────────────────────────────────────────────────────────────

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public LoginResponseDTO register(@Valid @RequestBody RegisterRequestDTO dto,
                                     HttpServletRequest request) {
        return authService.register(dto, extractClientIp(request), request.getHeader("User-Agent"));
    }

    // ─── Login ────────────────────────────────────────────────────────────────

    @PostMapping("/login")
    public LoginResponseDTO login(@Valid @RequestBody LoginRequestDTO dto,
                                  HttpServletRequest request) {
        return authService.login(dto, extractClientIp(request), request.getHeader("User-Agent"));
    }

    // ─── Refresh ──────────────────────────────────────────────────────────────

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDTO> refresh(@Valid @RequestBody RefreshRequestDTO dto,
                                                    HttpServletRequest request) {
        LoginResponseDTO response = refreshTokenService.rotateRefreshTokenAndIssueAccessToken(
                dto.getRefresh_token(),
                extractClientIp(request),
                request.getHeader("User-Agent")
        );
        return ResponseEntity.ok(response);
    }

    // ─── Logout ───────────────────────────────────────────────────────────────

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal AuthenticatedUser principal) {
        authService.logout(principal.id());
    }

    private String extractClientIp(HttpServletRequest request) {
        // TODO: enable trusted proxy mode later if we use a standard reverse proxy (e.g. ForwardedHeaderFilter)
        return request.getRemoteAddr();
    }
}
