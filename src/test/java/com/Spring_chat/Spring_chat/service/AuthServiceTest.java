package com.Spring_chat.Spring_chat.service;

import com.Spring_chat.Spring_chat.ENUM.RoleName;
import com.Spring_chat.Spring_chat.ENUM.UserStatus;
import com.Spring_chat.Spring_chat.dto.auth.LoginRequestDTO;
import com.Spring_chat.Spring_chat.dto.auth.LoginResponseDTO;
import com.Spring_chat.Spring_chat.dto.auth.RegisterRequestDTO;
import com.Spring_chat.Spring_chat.entity.Role;
import com.Spring_chat.Spring_chat.entity.User;
import com.Spring_chat.Spring_chat.exception.AppException;
import com.Spring_chat.Spring_chat.exception.ErrorCode;
import com.Spring_chat.Spring_chat.repository.RoleRepository;
import com.Spring_chat.Spring_chat.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for AuthService.
 *
 * Business rules verified:
 *  - register: uniqueness check, role seeding, password hashing, token issuance
 *  - login: credential check, account status gate (BANNED / INACTIVE)
 *  - logout: delegates revocation to RefreshTokenService
 *
 * No Spring context, no DB — all dependencies are mocked with Mockito.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository       userRepository;
    @Mock private RoleRepository       roleRepository;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private RefreshTokenService  refreshTokenService;

    @InjectMocks
    private AuthService authService;

    // ─── Test fixtures ────────────────────────────────────────────────────────

    private static final String IP       = "127.0.0.1";
    private static final String AGENT    = "UnitTestAgent/1.0";
    private static final String PASSWORD = "Aa!123456";
    private static final String HASHED   = "$2a$10$abc.hashedpassword.here";

    private final Role defaultRole =
            Role.builder().id(1L).name(RoleName.ROLE_USER).build();

    private final LoginResponseDTO dummyPair =
            new LoginResponseDTO("access-token", "refresh-token", "3600");

    // ─── register ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("register")
    class Register {

        private RegisterRequestDTO dto;

        @BeforeEach
        void setUp() {
            dto = new RegisterRequestDTO("alice", "alice@mail.test", "Alice Full Name", PASSWORD, PASSWORD);
        }

        @Test
        @DisplayName("happy path — unique credentials should save user with ACTIVE status and return token pair")
        void withUniqueCredentials_shouldSaveUserAndReturnTokens() {
            given(userRepository.existsByUsername("alice")).willReturn(false);
            given(userRepository.existsByEmail("alice@mail.test")).willReturn(false);
            given(roleRepository.findByName(RoleName.ROLE_USER)).willReturn(Optional.of(defaultRole));
            given(passwordEncoder.encode(PASSWORD)).willReturn(HASHED);
            given(refreshTokenService.issueTokenPair(any(User.class), eq(IP), eq(AGENT)))
                    .willReturn(dummyPair);

            LoginResponseDTO result = authService.register(dto, IP, AGENT);

            assertThat(result).isEqualTo(dummyPair);

            ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
            then(userRepository).should().save(saved.capture());
            User u = saved.getValue();
            assertThat(u.getUsername()).isEqualTo("alice");
            assertThat(u.getEmail()).isEqualTo("alice@mail.test");
            assertThat(u.getPasswordHash()).isEqualTo(HASHED);
            assertThat(u.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(u.getRoles()).contains(defaultRole);
        }

        @Test
        @DisplayName("duplicate username should throw USERNAME_ALREADY_EXISTS before touching the DB")
        void withDuplicateUsername_shouldThrowAndNeverSave() {
            given(userRepository.existsByUsername("alice")).willReturn(true);

            assertThatThrownBy(() -> authService.register(dto, IP, AGENT))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.USERNAME_ALREADY_EXISTS);

            then(userRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("duplicate email should throw EMAIL_ALREADY_EXISTS before touching the DB")
        void withDuplicateEmail_shouldThrowAndNeverSave() {
            given(userRepository.existsByUsername("alice")).willReturn(false);
            given(userRepository.existsByEmail("alice@mail.test")).willReturn(true);

            assertThatThrownBy(() -> authService.register(dto, IP, AGENT))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);

            then(userRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("missing ROLE_USER seed should throw IllegalStateException with descriptive message")
        void whenRoleUserNotSeeded_shouldThrowIllegalState() {
            given(userRepository.existsByUsername("alice")).willReturn(false);
            given(userRepository.existsByEmail("alice@mail.test")).willReturn(false);
            given(roleRepository.findByName(RoleName.ROLE_USER)).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.register(dto, IP, AGENT))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ROLE_USER");
        }

        @Test
        @DisplayName("raw password must never be stored — only hashed value is saved")
        void passwordShouldBeHashed_notStoredInPlainText() {
            given(userRepository.existsByUsername("alice")).willReturn(false);
            given(userRepository.existsByEmail("alice@mail.test")).willReturn(false);
            given(roleRepository.findByName(RoleName.ROLE_USER)).willReturn(Optional.of(defaultRole));
            given(passwordEncoder.encode(PASSWORD)).willReturn(HASHED);
            given(refreshTokenService.issueTokenPair(any(), any(), any())).willReturn(dummyPair);

            authService.register(dto, IP, AGENT);

            ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
            then(userRepository).should().save(saved.capture());
            assertThat(saved.getValue().getPasswordHash())
                    .doesNotContain(PASSWORD)
                    .isEqualTo(HASHED);
        }
    }

    // ─── login ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("login")
    class Login {

        private LoginRequestDTO dto;
        private User activeUser;

        @BeforeEach
        void setUp() {
            dto = new LoginRequestDTO("alice", PASSWORD);
            activeUser = User.builder()
                    .id(1L)
                    .username("alice")
                    .passwordHash(HASHED)
                    .status(UserStatus.ACTIVE)
                    .roles(Set.of(defaultRole))
                    .build();
        }

        @Test
        @DisplayName("happy path — correct credentials with ACTIVE account should return token pair")
        void withValidCredentials_shouldReturnTokenPair() {
            given(userRepository.findByUsername("alice")).willReturn(Optional.of(activeUser));
            given(passwordEncoder.matches(PASSWORD, HASHED)).willReturn(true);
            given(refreshTokenService.issueTokenPair(activeUser, IP, AGENT)).willReturn(dummyPair);

            LoginResponseDTO result = authService.login(dto, IP, AGENT);

            assertThat(result).isEqualTo(dummyPair);
        }

        @Test
        @DisplayName("unknown username should throw BadCredentialsException")
        void withUnknownUsername_shouldThrowBadCredentials() {
            given(userRepository.findByUsername("alice")).willReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(dto, IP, AGENT))
                    .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        @DisplayName("correct username but wrong password should throw BadCredentialsException")
        void withWrongPassword_shouldThrowBadCredentials() {
            given(userRepository.findByUsername("alice")).willReturn(Optional.of(activeUser));
            given(passwordEncoder.matches(PASSWORD, HASHED)).willReturn(false);

            assertThatThrownBy(() -> authService.login(dto, IP, AGENT))
                    .isInstanceOf(BadCredentialsException.class);
        }

        @Test
        @DisplayName("BANNED account should throw ACCOUNT_BANNED after password check passes")
        void withBannedAccount_shouldThrowAccountBanned() {
            User banned = User.builder()
                    .username("alice").passwordHash(HASHED).status(UserStatus.BANNED).build();
            given(userRepository.findByUsername("alice")).willReturn(Optional.of(banned));
            given(passwordEncoder.matches(PASSWORD, HASHED)).willReturn(true);

            assertThatThrownBy(() -> authService.login(dto, IP, AGENT))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ACCOUNT_BANNED);
        }

        @Test
        @DisplayName("INACTIVE account should throw ACCOUNT_DISABLED after password check passes")
        void withInactiveAccount_shouldThrowAccountDisabled() {
            User inactive = User.builder()
                    .username("alice").passwordHash(HASHED).status(UserStatus.INACTIVE).build();
            given(userRepository.findByUsername("alice")).willReturn(Optional.of(inactive));
            given(passwordEncoder.matches(PASSWORD, HASHED)).willReturn(true);

            assertThatThrownBy(() -> authService.login(dto, IP, AGENT))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.ACCOUNT_DISABLED);
        }
    }

    // ─── logout ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("logout")
    class Logout {

        @Test
        @DisplayName("should delegate token revocation to RefreshTokenService with current timestamp")
        void shouldRevokeAllActiveTokensViaRefreshService() {
            given(refreshTokenService.revokeAllActiveTokensForUser(eq(1L), any())).willReturn(3);

            authService.logout(1L);

            then(refreshTokenService).should().revokeAllActiveTokensForUser(eq(1L), any());
        }

        @Test
        @DisplayName("should not throw even when user has no active refresh tokens (0 revoked)")
        void withNoActiveTokens_shouldNotThrow() {
            given(refreshTokenService.revokeAllActiveTokensForUser(eq(99L), any())).willReturn(0);

            assertThatCode(() -> authService.logout(99L)).doesNotThrowAnyException();
        }
    }
}
