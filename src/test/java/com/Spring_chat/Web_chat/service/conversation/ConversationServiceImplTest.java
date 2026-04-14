package com.Spring_chat.Web_chat.service.conversation;

import com.Spring_chat.Web_chat.dto.ApiResponse;
import com.Spring_chat.Web_chat.dto.conversations.ConversationDetailDTO;
import com.Spring_chat.Web_chat.dto.conversations.CreateConversationsDTO;
import com.Spring_chat.Web_chat.dto.conversations.CreateConversationsResponseDTO;
import com.Spring_chat.Web_chat.entity.Conversation;
import com.Spring_chat.Web_chat.entity.ConversationParticipant;
import com.Spring_chat.Web_chat.entity.User;
import com.Spring_chat.Web_chat.enums.ConversationType;
import com.Spring_chat.Web_chat.exception.AppException;
import com.Spring_chat.Web_chat.exception.ErrorCode;
import com.Spring_chat.Web_chat.mappers.ConversationMapper;
import com.Spring_chat.Web_chat.repository.ConversationParticipantRepository;
import com.Spring_chat.Web_chat.repository.ConversationRepository;
import com.Spring_chat.Web_chat.repository.MessageRepository;
import com.Spring_chat.Web_chat.repository.UserRepository;
import com.Spring_chat.Web_chat.security.AuthenticatedUser;
import com.Spring_chat.Web_chat.service.common.CurrentUserProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ConversationServiceImplTest {

    @Mock private ConversationRepository conversationRepository;
    @Mock private UserRepository userRepository;
    @Mock private ConversationParticipantRepository conversationParticipantRepository;
    @Mock private ConversationMapper conversationMapper;
    @Mock private com.Spring_chat.Web_chat.repository.FriendshipRepository friendshipRepository;

    private CurrentUserProvider currentUserProvider;
    private ConversationServiceImpl conversationService;

    @BeforeEach
    void setUp() {
        currentUserProvider = new CurrentUserProvider(userRepository);
        conversationService = new ConversationServiceImpl(
                conversationRepository,
                userRepository,
                currentUserProvider,
                conversationParticipantRepository,
                conversationMapper,
                friendshipRepository
        );
    }

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

    @Nested
    @DisplayName("createConversation - PRIVATE")
    class CreatePrivate {

        @Test
        @DisplayName("đã có cuộc hội thoại PRIVATE thì trả về cuộc hội thoại đó (idempotent)")
        void existingPrivateConversation_shouldReturnExisting() {
            setCurrentUser(1L, "alice");
            User alice = User.builder().id(1L).username("alice").build();
            User bob = User.builder().id(2L).username("bob").build();
            Conversation existing = Conversation.builder().id(10L).type(ConversationType.PRIVATE).build();

            CreateConversationsDTO request = new CreateConversationsDTO();
            request.setType(ConversationType.PRIVATE);
            request.setParticipantIds(new Long[]{2L});

            CreateConversationsResponseDTO responseDTO = new CreateConversationsResponseDTO();
            responseDTO.setId(10L);

            given(userRepository.findById(1L)).willReturn(Optional.of(alice));
            given(userRepository.findById(2L)).willReturn(Optional.of(bob));
            given(conversationRepository.findPrivateBetween(ConversationType.PRIVATE, 1L, 2L))
                    .willReturn(Optional.of(existing));
            given(conversationParticipantRepository.findByConversation_Id(10L))
                    .willReturn(List.of(
                            ConversationParticipant.builder().conversation(existing).user(alice).build(),
                            ConversationParticipant.builder().conversation(existing).user(bob).build()
                    ));
            given(conversationMapper.toCreateConversationsResponseDTO(any(), any()))
                    .willReturn(responseDTO);

            ApiResponse<CreateConversationsResponseDTO> response = conversationService.createConversation(request);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().getId()).isEqualTo(10L);
            then(conversationRepository).shouldHaveNoMoreInteractions();
        }

        @Test
        @DisplayName("tạo mới PRIVATE khi chưa có")
        void newPrivateConversation_shouldCreate() {
            setCurrentUser(1L, "alice");
            User alice = User.builder().id(1L).username("alice").build();
            User bob = User.builder().id(2L).username("bob").build();

            CreateConversationsDTO request = new CreateConversationsDTO();
            request.setType(ConversationType.PRIVATE);
            request.setParticipantIds(new Long[]{2L});

            CreateConversationsResponseDTO responseDTO = new CreateConversationsResponseDTO();

            given(userRepository.findById(1L)).willReturn(Optional.of(alice));
            given(userRepository.findById(2L)).willReturn(Optional.of(bob));
            given(conversationRepository.findPrivateBetween(ConversationType.PRIVATE, 1L, 2L))
                    .willReturn(Optional.empty());
            given(conversationMapper.toCreateConversationsResponseDTO(any(), any()))
                    .willReturn(responseDTO);

            conversationService.createConversation(request);

            ArgumentCaptor<List<ConversationParticipant>> participantsCaptor = ArgumentCaptor.forClass(List.class);
            then(conversationParticipantRepository).should().saveAll(participantsCaptor.capture());
            List<ConversationParticipant> participants = participantsCaptor.getValue();
            assertThat(participants).hasSize(2);
            assertThat(participants).extracting(p -> p.getUser().getId())
                    .containsExactlyInAnyOrder(1L, 2L);
        }

        @Test
        @DisplayName("PRIVATE có nhiều hơn 1 participant -> BUSINESS_RULE_VIOLATED")
        void privateWithMoreThanOneParticipant_shouldThrow() {
            setCurrentUser(1L, "alice");
            User alice = User.builder().id(1L).username("alice").build();

            CreateConversationsDTO request = new CreateConversationsDTO();
            request.setType(ConversationType.PRIVATE);
            request.setParticipantIds(new Long[]{2L, 3L});

            given(userRepository.findById(1L)).willReturn(Optional.of(alice));

            assertThatThrownBy(() -> conversationService.createConversation(request))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATED);
        }
    }

    @Nested
    @DisplayName("createConversation - GROUP")
    class CreateGroup {

        @Test
        @DisplayName("GROUP thiếu tiêu đề -> BUSINESS_RULE_VIOLATED")
        void groupMissingTitle_shouldThrow() {
            setCurrentUser(1L, "alice");
            User alice = User.builder().id(1L).username("alice").build();

            CreateConversationsDTO request = new CreateConversationsDTO();
            request.setType(ConversationType.GROUP);
            request.setParticipantIds(new Long[]{2L, 3L});
            request.setTitle("   ");

            given(userRepository.findById(1L)).willReturn(Optional.of(alice));

            assertThatThrownBy(() -> conversationService.createConversation(request))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATED);
        }

        @Test
        @DisplayName("GROUP có userId không tồn tại -> RESOURCE_NOT_FOUND")
        void groupWithMissingUser_shouldThrow() {
            setCurrentUser(1L, "alice");
            User alice = User.builder().id(1L).username("alice").build();
            User bob = User.builder().id(2L).username("bob").build();

            CreateConversationsDTO request = new CreateConversationsDTO();
            request.setType(ConversationType.GROUP);
            request.setParticipantIds(new Long[]{2L, 3L});
            request.setTitle("Team A");

            given(userRepository.findById(1L)).willReturn(Optional.of(alice));
            given(userRepository.findAllById(anyIterable()))
                    .willReturn(List.of(bob));

            assertThatThrownBy(() -> conversationService.createConversation(request))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        }

        @Test
        @DisplayName("GROUP hợp lệ -> tạo conversation và trim title/avatar")
        void validGroup_shouldCreateAndTrimFields() {
            setCurrentUser(1L, "alice");
            User alice = User.builder().id(1L).username("alice").build();
            User bob = User.builder().id(2L).username("bob").build();
            User carol = User.builder().id(3L).username("carol").build();

            CreateConversationsDTO request = new CreateConversationsDTO();
            request.setType(ConversationType.GROUP);
            request.setParticipantIds(new Long[]{2L, 3L});
            request.setTitle("  Nhóm A  ");
            request.setAvatarUrl("  https://cdn.example.com/a.png  ");

            given(userRepository.findById(1L)).willReturn(Optional.of(alice));
            given(userRepository.findAllById(anyIterable()))
                    .willReturn(List.of(bob, carol));
            given(conversationMapper.toCreateConversationsResponseDTO(any(), any()))
                    .willReturn(new CreateConversationsResponseDTO());

            conversationService.createConversation(request);

            ArgumentCaptor<Conversation> conversationCaptor = ArgumentCaptor.forClass(Conversation.class);
            then(conversationRepository).should().save(conversationCaptor.capture());
            Conversation saved = conversationCaptor.getValue();
            assertThat(saved.getTitle()).isEqualTo("Nhóm A");
            assertThat(saved.getAvatarUrl()).isEqualTo("https://cdn.example.com/a.png");
            assertThat(saved.getOwner()).isEqualTo(alice);

            ArgumentCaptor<List<ConversationParticipant>> participantsCaptor = ArgumentCaptor.forClass(List.class);
            then(conversationParticipantRepository).should().saveAll(participantsCaptor.capture());
            List<ConversationParticipant> participants = participantsCaptor.getValue();
            assertThat(participants).hasSize(3);
        }
    }

    @Nested
    @DisplayName("getConversationDetail")
    class GetConversationDetail {

        @Test
        @DisplayName("participant hợp lệ -> trả chi tiết conversation")
        void participantShouldReceiveConversationDetail() {
            setCurrentUser(1L, "alice");
            User alice = User.builder().id(1L).username("alice").build();
            User owner = User.builder().id(1L).username("alice").build();
            Conversation conversation = Conversation.builder()
                    .id(55L)
                    .type(ConversationType.GROUP)
                    .title("Dev Team")
                    .owner(owner)
                    .build();
            List<ConversationParticipant> participants = List.of(
                    ConversationParticipant.builder().conversation(conversation).user(alice).build()
            );
            ConversationDetailDTO detailDTO = new ConversationDetailDTO();
            detailDTO.setId(55L);

            given(userRepository.findById(1L)).willReturn(Optional.of(alice));
            given(conversationRepository.findById(55L)).willReturn(Optional.of(conversation));
            given(conversationParticipantRepository.existsByConversation_IdAndUser_Id(55L, 1L)).willReturn(true);
            given(conversationParticipantRepository.findAllByConversation_IdOrderByJoinedAtAsc(55L)).willReturn(participants);
            given(conversationMapper.toConversationDetailDTO(conversation, participants)).willReturn(detailDTO);

            ApiResponse<ConversationDetailDTO> response = conversationService.getConversationDetail(55L);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().getId()).isEqualTo(55L);
        }

        @Test
        @DisplayName("conversation không tồn tại -> RESOURCE_NOT_FOUND")
        void missingConversationShouldThrow() {
            setCurrentUser(1L, "alice");
            User alice = User.builder().id(1L).username("alice").build();

            given(userRepository.findById(1L)).willReturn(Optional.of(alice));
            given(conversationRepository.findById(99L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> conversationService.getConversationDetail(99L))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
        }

        @Test
        @DisplayName("không phải participant -> FORBIDDEN")
        void nonParticipantShouldThrowForbidden() {
            setCurrentUser(1L, "alice");
            User alice = User.builder().id(1L).username("alice").build();
            Conversation conversation = Conversation.builder()
                    .id(55L)
                    .type(ConversationType.GROUP)
                    .build();

            given(userRepository.findById(1L)).willReturn(Optional.of(alice));
            given(conversationRepository.findById(55L)).willReturn(Optional.of(conversation));
            given(conversationParticipantRepository.existsByConversation_IdAndUser_Id(55L, 1L)).willReturn(false);

            assertThatThrownBy(() -> conversationService.getConversationDetail(55L))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("updateConversation")
    class UpdateConversation {
        // ... (existing tests)
    }

    @Nested
    @DisplayName("addUserToConversation")
    class AddUserToConversation {

        @Test
        @DisplayName("thêm thành viên thành công bởi owner")
        void ownerShouldAddUserSuccessfully() {
            setCurrentUser(1L, "alice");
            User alice = User.builder().id(1L).username("alice").build();
            User bob = User.builder().id(2L).username("bob").build();
            Conversation conversation = Conversation.builder()
                    .id(5L)
                    .type(ConversationType.GROUP)
                    .owner(alice)
                    .build();

            com.Spring_chat.Web_chat.dto.conversations.ListUserDTO request = new com.Spring_chat.Web_chat.dto.conversations.ListUserDTO();
            request.setUserIds(new Long[]{2L});

            given(userRepository.findById(1L)).willReturn(Optional.of(alice));
            given(conversationRepository.findById(5L)).willReturn(Optional.of(conversation));
            given(userRepository.findById(2L)).willReturn(Optional.of(bob));
            given(friendshipRepository.findBetweenUsers(1L, 2L)).willReturn(Optional.empty());
            given(conversationParticipantRepository.findByConversation_IdAndUser(5L, bob)).willReturn(null);

            ApiResponse<com.Spring_chat.Web_chat.dto.conversations.ListUserDTO> response =
                    conversationService.addUserToConversation(5L, request);

            assertThat(response.isSuccess()).isTrue();
            then(conversationParticipantRepository).should().save(any(ConversationParticipant.class));
        }

        @Test
        @DisplayName("không phải owner -> FORBIDDEN")
        void nonOwnerShouldThrowForbidden() {
            setCurrentUser(2L, "bob");
            User alice = User.builder().id(1L).username("alice").build();
            User bob = User.builder().id(2L).username("bob").build();
            Conversation conversation = Conversation.builder()
                    .id(5L)
                    .type(ConversationType.GROUP)
                    .owner(alice)
                    .build();

            given(userRepository.findById(2L)).willReturn(Optional.of(bob));
            given(conversationRepository.findById(5L)).willReturn(Optional.of(conversation));

            assertThatThrownBy(() -> conversationService.addUserToConversation(5L, new com.Spring_chat.Web_chat.dto.conversations.ListUserDTO()))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.FORBIDDEN);
        }

        @Test
        @DisplayName("thêm người đã block -> CANNOT_INVATE_BLOCK")
        void addingBlockedUserShouldThrow() {
            setCurrentUser(1L, "alice");
            User alice = User.builder().id(1L).username("alice").build();
            User bob = User.builder().id(2L).username("bob").build();
            Conversation conversation = Conversation.builder()
                    .id(5L)
                    .type(ConversationType.GROUP)
                    .owner(alice)
                    .build();

            com.Spring_chat.Web_chat.dto.conversations.ListUserDTO request = new com.Spring_chat.Web_chat.dto.conversations.ListUserDTO();
            request.setUserIds(new Long[]{2L});

            given(userRepository.findById(1L)).willReturn(Optional.of(alice));
            given(conversationRepository.findById(5L)).willReturn(Optional.of(conversation));
            given(userRepository.findById(2L)).willReturn(Optional.of(bob));

            com.Spring_chat.Web_chat.entity.Friendship friendship = new com.Spring_chat.Web_chat.entity.Friendship();
            friendship.setStatus(com.Spring_chat.Web_chat.enums.FriendshipStatus.BLOCKED);

            given(friendshipRepository.findBetweenUsers(1L, 2L)).willReturn(Optional.of(friendship));

            assertThatThrownBy(() -> conversationService.addUserToConversation(5L, request))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.CANNOT_INVATE_BLOCK);
        }

        @Test
        @DisplayName("thêm người đã là thành viên -> không làm gì (idempotent)")
        void addingExistingParticipantShouldDoNothing() {
            setCurrentUser(1L, "alice");
            User alice = User.builder().id(1L).username("alice").build();
            User bob = User.builder().id(2L).username("bob").build();
            Conversation conversation = Conversation.builder()
                    .id(5L)
                    .type(ConversationType.GROUP)
                    .owner(alice)
                    .build();

            com.Spring_chat.Web_chat.dto.conversations.ListUserDTO request = new com.Spring_chat.Web_chat.dto.conversations.ListUserDTO();
            request.setUserIds(new Long[]{2L});

            given(userRepository.findById(1L)).willReturn(Optional.of(alice));
            given(conversationRepository.findById(5L)).willReturn(Optional.of(conversation));
            given(userRepository.findById(2L)).willReturn(Optional.of(bob));
            given(friendshipRepository.findBetweenUsers(1L, 2L)).willReturn(Optional.empty());
            given(conversationParticipantRepository.findByConversation_IdAndUser(5L, bob))
                    .willReturn(ConversationParticipant.builder().build());

            conversationService.addUserToConversation(5L, request);

            then(conversationParticipantRepository).should(org.mockito.Mockito.never()).save(any());
        }
    }

    @Nested
    @DisplayName("removeParticipantFromConversation")
    class RemoveParticipant {

        @Test
        @DisplayName("user tự rời nhóm thành công")
        void userShouldLeaveSuccessfully() {
            setCurrentUser(2L, "bob");
            User alice = User.builder().id(1L).username("alice").build();
            User bob = User.builder().id(2L).username("bob").build();
            Conversation conversation = Conversation.builder()
                    .id(5L)
                    .type(ConversationType.GROUP)
                    .owner(alice)
                    .build();

            ConversationParticipant participant = ConversationParticipant.builder()
                    .id(100L).conversation(conversation).user(bob).build();

            given(userRepository.findById(2L)).willReturn(Optional.of(bob));
            given(conversationRepository.findById(5L)).willReturn(Optional.of(conversation));
            given(conversationParticipantRepository.findByConversation_IdAndUser_Id(5L, 2L))
                    .willReturn(Optional.of(participant));

            ApiResponse<Void> response = conversationService.removeParticipantFromConversation(5L, 2L);

            assertThat(response.isSuccess()).isTrue();
            assertThat(participant.getLeftAt()).isNotNull();
            then(conversationParticipantRepository).should().save(participant);
        }

        @Test
        @DisplayName("owner kick thành viên thành công")
        void ownerShouldKickMemberSuccessfully() {
            setCurrentUser(1L, "alice");
            User alice = User.builder().id(1L).username("alice").build();
            User bob = User.builder().id(2L).username("bob").build();
            Conversation conversation = Conversation.builder()
                    .id(5L)
                    .type(ConversationType.GROUP)
                    .owner(alice)
                    .build();

            ConversationParticipant participant = ConversationParticipant.builder()
                    .id(100L).conversation(conversation).user(bob).build();

            given(userRepository.findById(1L)).willReturn(Optional.of(alice));
            given(conversationRepository.findById(5L)).willReturn(Optional.of(conversation));
            given(conversationParticipantRepository.findByConversation_IdAndUser_Id(5L, 2L))
                    .willReturn(Optional.of(participant));

            ApiResponse<Void> response = conversationService.removeParticipantFromConversation(5L, 2L);

            assertThat(response.isSuccess()).isTrue();
            assertThat(participant.getLeftAt()).isNotNull();
        }

        @Test
        @DisplayName("owner rời nhóm -> tự động chuyển quyền owner cho người tiếp theo")
        void ownerLeave_shouldTransferOwnership() {
            setCurrentUser(1L, "alice");
            User alice = User.builder().id(1L).username("alice").build();
            User bob = User.builder().id(2L).username("bob").build();
            Conversation conversation = Conversation.builder()
                    .id(5L)
                    .type(ConversationType.GROUP)
                    .owner(alice)
                    .build();

            ConversationParticipant alicePart = ConversationParticipant.builder()
                    .id(10L).conversation(conversation).user(alice).build();
            ConversationParticipant bobPart = ConversationParticipant.builder()
                    .id(11L).conversation(conversation).user(bob).build();

            given(userRepository.findById(1L)).willReturn(Optional.of(alice));
            given(conversationRepository.findById(5L)).willReturn(Optional.of(conversation));
            given(conversationParticipantRepository.findByConversation_IdAndUser_Id(5L, 1L))
                    .willReturn(Optional.of(alicePart));
            // Cập nhật stub: loại trừ người rời đi
            given(conversationParticipantRepository.findFirstByConversation_IdAndUser_IdNotAndLeftAtIsNullOrderByJoinedAtAsc(5L, 1L))
                    .willReturn(Optional.of(bobPart));

            conversationService.removeParticipantFromConversation(5L, 1L);

            assertThat(conversation.getOwner()).isEqualTo(bob);
            then(conversationRepository).should().save(conversation);
        }

        @Test
        @DisplayName("không phải owner kick người khác -> FORBIDDEN")
        void nonOwnerKick_shouldThrowForbidden() {
            setCurrentUser(2L, "bob");
            User alice = User.builder().id(1L).username("alice").build();
            User bob = User.builder().id(2L).username("bob").build();
            User carol = User.builder().id(3L).username("carol").build();
            Conversation conversation = Conversation.builder()
                    .id(5L)
                    .type(ConversationType.GROUP)
                    .owner(alice)
                    .build();

            ConversationParticipant carolPart = ConversationParticipant.builder()
                    .id(102L).conversation(conversation).user(carol).build();

            given(userRepository.findById(2L)).willReturn(Optional.of(bob));
            given(conversationRepository.findById(5L)).willReturn(Optional.of(conversation));
            given(conversationParticipantRepository.findByConversation_IdAndUser_Id(5L, 3L))
                    .willReturn(Optional.of(carolPart));

            assertThatThrownBy(() -> conversationService.removeParticipantFromConversation(5L, 3L))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.FORBIDDEN);
        }
    }
}
