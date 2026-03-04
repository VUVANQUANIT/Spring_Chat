package com.Spring_chat.Spring_chat.service;

import com.Spring_chat.Spring_chat.ENUM.RoleName;
import com.Spring_chat.Spring_chat.ENUM.UserStatus;
import com.Spring_chat.Spring_chat.dto.auth.LoginRequestDTO;
import com.Spring_chat.Spring_chat.dto.auth.LoginResponseDTO;
import com.Spring_chat.Spring_chat.dto.auth.RegisterRequestDTO;
import com.Spring_chat.Spring_chat.entity.Role;
import com.Spring_chat.Spring_chat.entity.User;
import com.Spring_chat.Spring_chat.exception.AppException;
import com.Spring_chat.Spring_chat.repository.RoleRepository;
import com.Spring_chat.Spring_chat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    // ─── Register ─────────────────────────────────────────────────────────────

    @Transactional
    public LoginResponseDTO register(RegisterRequestDTO dto, String clientIp, String userAgent) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new AppException(HttpStatus.CONFLICT, "Tên đăng nhập đã được sử dụng");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new AppException(HttpStatus.CONFLICT, "Email đã được sử dụng");
        }

        Role defaultRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException(
                        "ROLE_USER not found — ensure DataInitializer has run successfully"));

        User user = User.builder()
                .username(dto.getUsername())
                .email(dto.getEmail())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .fullName(dto.getUsername())
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(defaultRole)))
                .build();

        userRepository.save(user);
        log.info("Registered new user: {}", user.getUsername());

        return refreshTokenService.issueTokenPair(user, clientIp, userAgent);
    }

    // ─── Login ────────────────────────────────────────────────────────────────

    @Transactional
    public LoginResponseDTO login(LoginRequestDTO dto, String clientIp, String userAgent) {
        User user = userRepository.findByUsername(dto.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        if (user.getStatus() == UserStatus.BANNED) {
            throw new AppException(HttpStatus.FORBIDDEN, "Tài khoản của bạn đã bị cấm");
        }
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new AppException(HttpStatus.FORBIDDEN, "Tài khoản chưa được kích hoạt");
        }

        log.debug("User logged in: {}", user.getUsername());
        return refreshTokenService.issueTokenPair(user, clientIp, userAgent);
    }

    // ─── Logout ───────────────────────────────────────────────────────────────

    @Transactional
    public void logout(Long userId) {
        int revoked = refreshTokenService.revokeAllActiveTokensForUser(userId, java.time.Instant.now());
        log.debug("Revoked {} refresh token(s) for userId={}", revoked, userId);
    }
}
