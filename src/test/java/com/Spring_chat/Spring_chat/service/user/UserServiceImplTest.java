package com.Spring_chat.Spring_chat.service.user;

import com.Spring_chat.Spring_chat.dto.ApiResponse;
import com.Spring_chat.Spring_chat.dto.user.MyProfileUserDTO;
import com.Spring_chat.Spring_chat.dto.user.ProfileUserDTO;
import com.Spring_chat.Spring_chat.dto.user.UpdateMyProfileRequestDTO;
import com.Spring_chat.Spring_chat.entity.User;
import com.Spring_chat.Spring_chat.exception.AppException;
import com.Spring_chat.Spring_chat.exception.ErrorCode;
import com.Spring_chat.Spring_chat.mappers.UserMapper;
import com.Spring_chat.Spring_chat.repository.UserRepository;
import com.Spring_chat.Spring_chat.security.AuthenticatedUser;
import com.Spring_chat.Spring_chat.service.common.CurrentUserProvider;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for UserServiceImpl.
 *
 * Covers:
 *  - getUserProfile: happy path, null id, not found
 *  - getMyProfile:   reads userId from SecurityContext (not from caller)
 *  - updateMyProfile: partial updates, trim, null-field no-op
 *
 * CurrentUserProvider là real instance (không mock) dùng chung userRepository mock,
 * SecurityContext được set thủ công → test đi qua đúng code path thật của findCurrentUserOrThrow().
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper     userMapper;

    // Real instance (không @Mock) — dùng userRepository mock bên trong
    private CurrentUserProvider currentUserProvider;
    private UserServiceImpl     userService;

    @BeforeEach
    void setUpService() {
        currentUserProvider = new CurrentUserProvider(userRepository);
        userService = new UserServiceImpl(userRepository, userMapper, currentUserProvider);
    }

    // ─── SecurityContext helpers ──────────────────────────────────────────────

    private void setCurrentUser(Long userId, String username) {
        AuthenticatedUser principal =
                new AuthenticatedUser(userId, username, Collections.emptyList());
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ─── getUserProfile ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("getUserProfile")
    class GetUserProfile {

        @Test
        @DisplayName("happy path — existing user id should return ProfileUserDTO wrapped in ApiResponse")
        void withExistingId_shouldReturnPublicProfile() {
            User user = User.builder().id(2L).username("bob").build();
            ProfileUserDTO dto = new ProfileUserDTO();
            dto.setFullName("Bob Smith");
            dto.setAvatarUrl("https://example.com/bob.jpg");

            given(userRepository.findById(2L)).willReturn(Optional.of(user));
            given(userMapper.userToUserDTO(user)).willReturn(dto);

            ApiResponse<ProfileUserDTO> response = userService.getUserProfile(2L);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isEqualTo(dto);
            assertThat(response.getData().getFullName()).isEqualTo("Bob Smith");
        }

        @Test
        @DisplayName("null id should throw MISSING_PARAMETER before querying the DB")
        void withNullId_shouldThrowMissingParameter() {
            assertThatThrownBy(() -> userService.getUserProfile(null))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.MISSING_PARAMETER);

            then(userRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("non-existent user id should throw RESOURCE_NOT_FOUND")
        void withNotFoundId_shouldThrowResourceNotFound() {
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserProfile(999L))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    // ─── getMyProfile ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getMyProfile")
    class GetMyProfile {

        @Test
        @DisplayName("should read userId from SecurityContext — not accept it as a parameter")
        void shouldReturnPrivateProfileOfCurrentUser() {
            setCurrentUser(1L, "alice");
            User alice = User.builder().id(1L).username("alice").email("alice@mail.test").build();
            MyProfileUserDTO dto = new MyProfileUserDTO();
            dto.setUsername("alice");
            dto.setEmail("alice@mail.test");

            given(userRepository.findById(1L)).willReturn(Optional.of(alice));
            given(userMapper.userToMyUserDTO(alice)).willReturn(dto);

            ApiResponse<MyProfileUserDTO> response = userService.getMyProfile();

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().getUsername()).isEqualTo("alice");
            assertThat(response.getData().getEmail()).isEqualTo("alice@mail.test");
        }

        @Test
        @DisplayName("if userId in token no longer exists in DB, should throw RESOURCE_NOT_FOUND")
        void whenTokenUserDeletedFromDb_shouldThrowResourceNotFound() {
            setCurrentUser(42L, "ghost");
            given(userRepository.findById(42L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getMyProfile())
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        }
    }

    // ─── updateMyProfile ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateMyProfile")
    class UpdateMyProfile {

        private User alice;

        @BeforeEach
        void setUp() {
            setCurrentUser(1L, "alice");
            alice = User.builder()
                    .id(1L).username("alice").email("alice@mail.test")
                    .fullName("Alice Old").avatarUrl("https://old.url/avatar.jpg")
                    .build();
        }

        @Test
        @DisplayName("providing fullName should update fullName and keep existing avatarUrl")
        void withFullNameOnly_shouldUpdateFullName() {
            UpdateMyProfileRequestDTO request = new UpdateMyProfileRequestDTO();
            request.setFullName("Alice New");

            MyProfileUserDTO expectedDto = new MyProfileUserDTO();
            expectedDto.setFullName("Alice New");

            given(userRepository.findById(1L)).willReturn(Optional.of(alice));
            given(userRepository.save(alice)).willReturn(alice);
            given(userMapper.userToMyUserDTO(alice)).willReturn(expectedDto);

            ApiResponse<MyProfileUserDTO> response = userService.updateMyProfile(request);

            assertThat(alice.getFullName()).isEqualTo("Alice New");
            assertThat(alice.getAvatarUrl()).isEqualTo("https://old.url/avatar.jpg"); // unchanged
            assertThat(response.getData()).isEqualTo(expectedDto);
        }

        @Test
        @DisplayName("providing avatarUrl should update avatarUrl and keep existing fullName")
        void withAvatarUrlOnly_shouldUpdateAvatarUrl() {
            UpdateMyProfileRequestDTO request = new UpdateMyProfileRequestDTO();
            request.setAvatarUrl("https://cdn.example.com/new.jpg");

            MyProfileUserDTO expectedDto = new MyProfileUserDTO();
            expectedDto.setAvatarUrl("https://cdn.example.com/new.jpg");

            given(userRepository.findById(1L)).willReturn(Optional.of(alice));
            given(userRepository.save(alice)).willReturn(alice);
            given(userMapper.userToMyUserDTO(alice)).willReturn(expectedDto);

            userService.updateMyProfile(request);

            assertThat(alice.getAvatarUrl()).isEqualTo("https://cdn.example.com/new.jpg");
            assertThat(alice.getFullName()).isEqualTo("Alice Old"); // unchanged
        }

        @Test
        @DisplayName("null fields in request should not overwrite existing values (partial update)")
        void withAllNullFields_shouldNotModifyUser() {
            UpdateMyProfileRequestDTO request = new UpdateMyProfileRequestDTO();
            // both fields left null

            MyProfileUserDTO expectedDto = new MyProfileUserDTO();
            expectedDto.setFullName("Alice Old");

            given(userRepository.findById(1L)).willReturn(Optional.of(alice));
            given(userRepository.save(alice)).willReturn(alice);
            given(userMapper.userToMyUserDTO(alice)).willReturn(expectedDto);

            userService.updateMyProfile(request);

            assertThat(alice.getFullName()).isEqualTo("Alice Old");
            assertThat(alice.getAvatarUrl()).isEqualTo("https://old.url/avatar.jpg");
        }

        @Test
        @DisplayName("fullName with surrounding whitespace should be trimmed before saving")
        void withFullNameHavingWhitespace_shouldTrimBeforeSave() {
            UpdateMyProfileRequestDTO request = new UpdateMyProfileRequestDTO();
            request.setFullName("  Alice Trimmed  ");

            given(userRepository.findById(1L)).willReturn(Optional.of(alice));
            given(userRepository.save(any(User.class))).willReturn(alice);
            given(userMapper.userToMyUserDTO(any())).willReturn(new MyProfileUserDTO());

            userService.updateMyProfile(request);

            ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
            then(userRepository).should().save(saved.capture());
            assertThat(saved.getValue().getFullName()).isEqualTo("Alice Trimmed");
        }

        @Test
        @DisplayName("avatarUrl with surrounding whitespace should be trimmed before saving")
        void withAvatarUrlHavingWhitespace_shouldTrimBeforeSave() {
            UpdateMyProfileRequestDTO request = new UpdateMyProfileRequestDTO();
            request.setAvatarUrl("  https://cdn.example.com/avatar.png  ");

            given(userRepository.findById(1L)).willReturn(Optional.of(alice));
            given(userRepository.save(any(User.class))).willReturn(alice);
            given(userMapper.userToMyUserDTO(any())).willReturn(new MyProfileUserDTO());

            userService.updateMyProfile(request);

            ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
            then(userRepository).should().save(saved.capture());
            assertThat(saved.getValue().getAvatarUrl())
                    .isEqualTo("https://cdn.example.com/avatar.png");
        }

        @Test
        @DisplayName("should return the updated DTO with the correct success message")
        void shouldReturnSuccessMessageInApiResponse() {
            UpdateMyProfileRequestDTO request = new UpdateMyProfileRequestDTO();
            request.setFullName("Alice Success");

            given(userRepository.findById(1L)).willReturn(Optional.of(alice));
            given(userRepository.save(alice)).willReturn(alice);
            given(userMapper.userToMyUserDTO(alice)).willReturn(new MyProfileUserDTO());

            ApiResponse<MyProfileUserDTO> response = userService.updateMyProfile(request);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getMessage()).isEqualTo("Profile updated successfully");
        }
    }
}
