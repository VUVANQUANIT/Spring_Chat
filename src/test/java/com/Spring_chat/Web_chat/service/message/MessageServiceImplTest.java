package com.Spring_chat.Web_chat.service.message;

import com.Spring_chat.Web_chat.dto.ApiResponse;
import com.Spring_chat.Web_chat.dto.message.MessageListResponseDTO;
import com.Spring_chat.Web_chat.dto.message.MessageRowProjection;
import com.Spring_chat.Web_chat.dto.message.ReadReceiptRequestDTO;
import com.Spring_chat.Web_chat.dto.message.ReadReceiptResponseDTO;
import com.Spring_chat.Web_chat.dto.message.SendMessageRequestDTO;
import com.Spring_chat.Web_chat.dto.message.SendMessageResponseDTO;
import com.Spring_chat.Web_chat.entity.Conversation;
import com.Spring_chat.Web_chat.entity.ConversationParticipant;
import com.Spring_chat.Web_chat.entity.Message;
import com.Spring_chat.Web_chat.entity.User;
import com.Spring_chat.Web_chat.enums.ConversationStatus;
import com.Spring_chat.Web_chat.enums.ConversationType;
import com.Spring_chat.Web_chat.enums.MessageDeliveryStatus;
import com.Spring_chat.Web_chat.enums.MessageType;
import com.Spring_chat.Web_chat.exception.AppException;
import com.Spring_chat.Web_chat.exception.ErrorCode;
import com.Spring_chat.Web_chat.repository.ConversationParticipantRepository;
import com.Spring_chat.Web_chat.repository.ConversationRepository;
import com.Spring_chat.Web_chat.repository.MessageDeliveryStatusRepo;
import com.Spring_chat.Web_chat.repository.MessageRepository;
import com.Spring_chat.Web_chat.service.common.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.any;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MessageServiceImplTest {


    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ConversationParticipantRepository conversationParticipantRepository;

    @Mock
    private MessageDeliveryStatusRepo messageDeliveryStatusRepo;

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private MessageServiceImpl messageService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = User.builder().id(1L).username("testuser").build();
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
    }

    @Nested
    @DisplayName("getMessageList")
    class GetMessageList {

        @Test
        @DisplayName("Lấy danh sách tin nhắn thành công (không có cờ hasMore)")
        void getMessageList_Success_NoMoreItems() {
            // Given
            Long conversationId = 100L;
            Integer requestedLimit = 10;
            Long beforeId = null;

            given(currentUserProvider.findCurrentUserOrThrow()).willReturn(currentUser);
            given(conversationParticipantRepository.existsByConversation_IdAndUser_IdAndLeftAtIsNull(conversationId, currentUser.getId()))
                    .willReturn(true);

            List<MessageRowProjection> mockRows = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                MessageRowProjection row = mock(MessageRowProjection.class);
                given(row.getId()).willReturn((long) (i + 1));
                given(row.getContent()).willReturn("Hello " + i);
                given(row.getIsDeleted()).willReturn(false);
                given(row.getIsEdited()).willReturn(false);
                given(row.getType()).willReturn(MessageType.TEXT);
                given(row.getCreatedAt()).willReturn(Instant.now());
                given(row.getSenderId()).willReturn(2L);
                given(row.getSenderUsername()).willReturn("sender2");
                given(row.getSenderAvatar()).willReturn("avatar2");
                given(row.getMyStatus()).willReturn(MessageDeliveryStatus.SEEN);
                mockRows.add(row);
            }

            given(messageRepository.findMessagesByConversation(conversationId, currentUser.getId(), null, beforeId, org.springframework.data.domain.PageRequest.of(0, requestedLimit + 1)))
                    .willReturn(mockRows);
            given(messageDeliveryStatusRepo.findAllByMessage_IdIn(org.mockito.ArgumentMatchers.anyList()))
                    .willReturn(new ArrayList<>());

            // When
            ApiResponse<MessageListResponseDTO> response = messageService.getMessageList(beforeId, requestedLimit, conversationId);

            // Then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isNotNull();
            assertThat(response.getData().isHasMore()).isFalse();
            assertThat(response.getData().getItems()).hasSize(5);
            assertThat(response.getData().getItems().get(0).getContent()).isEqualTo("Hello 0");
            assertThat(response.getData().getItems().get(0).getMyStatus()).isEqualTo(MessageDeliveryStatus.SEEN);
        }

        @Test
        @DisplayName("Lấy danh sách tin nhắn có phân trang (cờ hasMore = true)")
        void getMessageList_Success_WithHasMore() {
            // Given
            Long conversationId = 100L;
            Integer requestedLimit = 2;
            Long beforeId = 50L;
            Instant beforeCreatedAt = Instant.now().minusSeconds(60);

            given(currentUserProvider.findCurrentUserOrThrow()).willReturn(currentUser);
            given(conversationParticipantRepository.existsByConversation_IdAndUser_IdAndLeftAtIsNull(conversationId, currentUser.getId()))
                    .willReturn(true);
            given(messageRepository.findCreatedAtById(beforeId)).willReturn(beforeCreatedAt);

            List<MessageRowProjection> mockRows = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                MessageRowProjection row = mock(MessageRowProjection.class);
                if (i < 2) { // only first 2 rows are mapped
                    given(row.getId()).willReturn((long) (49 - i));
                    given(row.getContent()).willReturn("Msg " + i);
                    given(row.getIsDeleted()).willReturn(false);
                    given(row.getIsEdited()).willReturn(false);
                    given(row.getType()).willReturn(MessageType.TEXT);
                    given(row.getCreatedAt()).willReturn(Instant.now());
                    given(row.getSenderId()).willReturn(2L);
                    given(row.getSenderUsername()).willReturn("sender2");
                    given(row.getSenderAvatar()).willReturn("avatar2");
                    given(row.getMyStatus()).willReturn(MessageDeliveryStatus.DELIVERED);
                } else {
                    // Third row is only used to determine hasMore=true (getId() is called for messageIds list)
                    given(row.getId()).willReturn(47L);
                }
                mockRows.add(row);
            }


            given(messageRepository.findMessagesByConversation(conversationId, currentUser.getId(), beforeCreatedAt, beforeId, org.springframework.data.domain.PageRequest.of(0, requestedLimit + 1)))
                    .willReturn(mockRows);
            given(messageDeliveryStatusRepo.findAllByMessage_IdIn(org.mockito.ArgumentMatchers.anyList()))
                    .willReturn(new ArrayList<>());

            // When
            ApiResponse<MessageListResponseDTO> response = messageService.getMessageList(beforeId, requestedLimit, conversationId);

            // Then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isNotNull();
            assertThat(response.getData().isHasMore()).isTrue();
            assertThat(response.getData().getItems()).hasSize(2);
        }

        @Test
        @DisplayName("Tin nhắn bị xóa sẽ hiển thị content = null và isDeleted = true")
        void getMessageList_Success_DeletedMessage() {
            // Given
            Long conversationId = 100L;

            given(currentUserProvider.findCurrentUserOrThrow()).willReturn(currentUser);
            given(conversationParticipantRepository.existsByConversation_IdAndUser_IdAndLeftAtIsNull(conversationId, currentUser.getId()))
                    .willReturn(true);

            MessageRowProjection row = mock(MessageRowProjection.class);
            given(row.getId()).willReturn(1L);
            given(row.getIsDeleted()).willReturn(true); 
            // Add other necessary stubs for toMessageSummary
            given(row.getSenderId()).willReturn(2L);
            given(row.getSenderUsername()).willReturn("sender2");
            given(row.getSenderAvatar()).willReturn("avatar2");
            given(row.getType()).willReturn(MessageType.TEXT);
            given(row.getCreatedAt()).willReturn(Instant.now());
            given(row.getMyStatus()).willReturn(MessageDeliveryStatus.SENT);

            List<MessageRowProjection> mockRows = new ArrayList<>();
            mockRows.add(row);

            given(messageRepository.findMessagesByConversation(anyLong(), anyLong(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)))
                    .willReturn(mockRows);
            given(messageDeliveryStatusRepo.findAllByMessage_IdIn(org.mockito.ArgumentMatchers.anyList()))
                    .willReturn(new ArrayList<>());

            // When
            ApiResponse<MessageListResponseDTO> response = messageService.getMessageList(null, 30, conversationId);

            // Then
            assertThat(response.getData().getItems()).hasSize(1);
            assertThat(response.getData().getItems().get(0).getContent()).isNull();
            assertThat(response.getData().getItems().get(0).isDeleted()).isTrue();
        }


        @Test
        @DisplayName("Báo lỗi FORBIDDEN nếu user không tham gia cuộc hội thoại")
        void getMessageList_ThrowsForbidden_WhenNotParticipant() {
            // Given
            Long conversationId = 100L;

            given(currentUserProvider.findCurrentUserOrThrow()).willReturn(currentUser);
            given(conversationParticipantRepository.existsByConversation_IdAndUser_IdAndLeftAtIsNull(conversationId, currentUser.getId()))
                    .willReturn(false);

            // When & Then
            assertThatThrownBy(() -> messageService.getMessageList(null, 30, conversationId))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining("Bạn không phải là thành viên")
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.FORBIDDEN);
        }

        @Test
        @DisplayName("Giới hạn số lượng tin nhắn tối đa (clamped limit = 100)")
        void getMessageList_ClampsLimitTo100() {
            // Given
            Long conversationId = 100L;
            Integer requestedLimit = 150; // Quá giới hạn
            Long beforeId = null;

            given(currentUserProvider.findCurrentUserOrThrow()).willReturn(currentUser);
            given(conversationParticipantRepository.existsByConversation_IdAndUser_IdAndLeftAtIsNull(conversationId, currentUser.getId()))
                    .willReturn(true);
            given(messageRepository.findMessagesByConversation(anyLong(), anyLong(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)))
                    .willReturn(new ArrayList<>());
            given(messageDeliveryStatusRepo.findAllByMessage_IdIn(org.mockito.ArgumentMatchers.anyList()))
                    .willReturn(new ArrayList<>());

            // When
            messageService.getMessageList(beforeId, requestedLimit, conversationId);

            // Then
            // Verify that the repository is called with 100 + 1 instead of 150 + 1
            verify(messageRepository).findMessagesByConversation(conversationId, currentUser.getId(), null, beforeId, org.springframework.data.domain.PageRequest.of(0, 101));
        }

    }

    @Nested
    @DisplayName("sendMessage")
    class SendMessage {
        @Test
        @DisplayName("Gửi TEXT thành công và tạo MessageStatus SENT cho active participants")
        void sendMessage_TextSuccess() {
            Long conversationId = 10L;
            Conversation conversation = Conversation.builder()
                    .id(conversationId)
                    .type(ConversationType.GROUP)
                    .status(ConversationStatus.ACTIVE)
                    .build();

            User user2 = User.builder().id(2L).username("u2").build();
            ConversationParticipant p1 = ConversationParticipant.builder().conversation(conversation).user(currentUser).build();
            ConversationParticipant p2 = ConversationParticipant.builder().conversation(conversation).user(user2).build();

            SendMessageRequestDTO request = new SendMessageRequestDTO();
            request.setType(MessageType.TEXT);
            request.setContent("  hello  ");

            Message saved = Message.builder()
                    .id(100L)
                    .conversation(conversation)
                    .sender(currentUser)
                    .content("hello")
                    .type(MessageType.TEXT)
                    .isDeleted(false)
                    .isEdited(false)
                    .createdAt(Instant.now())
                    .build();

            given(currentUserProvider.findCurrentUserOrThrow()).willReturn(currentUser);
            given(conversationRepository.findById(conversationId)).willReturn(Optional.of(conversation));
            given(conversationParticipantRepository.findByConversation_IdAndUser_IdAndLeftAtIsNull(conversationId, currentUser.getId()))
                    .willReturn(Optional.of(p1));
            given(messageRepository.save(org.mockito.ArgumentMatchers.any(Message.class))).willReturn(saved);
            given(conversationParticipantRepository.findAllByConversation_IdAndLeftAtIsNull(conversationId))
                    .willReturn(List.of(p1, p2));

            ApiResponse<SendMessageResponseDTO> response = messageService.sendMessage(conversationId, request);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().getId()).isEqualTo(100L);
            assertThat(response.getData().getContent()).isEqualTo("hello");
            verify(messageDeliveryStatusRepo, times(1)).saveAll(org.mockito.ArgumentMatchers.anyList());
        }

        @Test
        @DisplayName("Không phải active participant thì FORBIDDEN")
        void sendMessage_ForbiddenWhenNotActiveParticipant() {
            Long conversationId = 10L;
            Conversation conversation = Conversation.builder()
                    .id(conversationId)
                    .status(ConversationStatus.ACTIVE)
                    .build();
            SendMessageRequestDTO request = new SendMessageRequestDTO();
            request.setContent("hello");

            given(currentUserProvider.findCurrentUserOrThrow()).willReturn(currentUser);
            given(conversationRepository.findById(conversationId)).willReturn(Optional.of(conversation));
            given(conversationParticipantRepository.findByConversation_IdAndUser_IdAndLeftAtIsNull(conversationId, currentUser.getId()))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> messageService.sendMessage(conversationId, request))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.FORBIDDEN);
        }

        @Test
        @DisplayName("Conversation INACTIVE thì BUSINESS_RULE_VIOLATED")
        void sendMessage_RejectInactiveConversation() {
            Long conversationId = 10L;
            Conversation conversation = Conversation.builder()
                    .id(conversationId)
                    .status(ConversationStatus.INACTIVE)
                    .build();
            SendMessageRequestDTO request = new SendMessageRequestDTO();
            request.setContent("hello");

            given(currentUserProvider.findCurrentUserOrThrow()).willReturn(currentUser);
            given(conversationRepository.findById(conversationId)).willReturn(Optional.of(conversation));

            assertThatThrownBy(() -> messageService.sendMessage(conversationId, request))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATED);
        }

        @Test
        @DisplayName("Idempotency: request trùng trong 30s trả message cũ")
        void sendMessage_DuplicateReturnsExisting() {
            Long conversationId = 10L;
            String clientMessageId = "retry-1";
            Conversation conversation = Conversation.builder()
                    .id(conversationId)
                    .status(ConversationStatus.ACTIVE)
                    .build();
            ConversationParticipant p1 = ConversationParticipant.builder().conversation(conversation).user(currentUser).build();

            SendMessageRequestDTO request = new SendMessageRequestDTO();
            request.setType(MessageType.TEXT);
            request.setContent("hello");
            request.setClientMessageId(clientMessageId);

            Message existing = Message.builder()
                    .id(200L)
                    .conversation(conversation)
                    .sender(currentUser)
                    .content("hello")
                    .type(MessageType.TEXT)
                    .isDeleted(false)
                    .isEdited(false)
                    .createdAt(Instant.now())
                    .clientMessageId(clientMessageId)
                    .build();

            given(currentUserProvider.findCurrentUserOrThrow()).willReturn(currentUser);
            given(conversationRepository.findById(conversationId)).willReturn(Optional.of(conversation));
            given(conversationParticipantRepository.findByConversation_IdAndUser_IdAndLeftAtIsNull(conversationId, currentUser.getId()))
                    .willReturn(Optional.of(p1));
            given(valueOperations.setIfAbsent(anyString(), org.mockito.ArgumentMatchers.eq("PENDING"), org.mockito.ArgumentMatchers.any()))
                    .willReturn(false);
            given(messageRepository
                    .findFirstByConversation_IdAndSender_IdAndClientMessageIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                            org.mockito.ArgumentMatchers.eq(conversationId),
                            org.mockito.ArgumentMatchers.eq(currentUser.getId()),
                            org.mockito.ArgumentMatchers.eq(clientMessageId),
                            org.mockito.ArgumentMatchers.any(Instant.class)))
                    .willReturn(Optional.of(existing));

            ApiResponse<SendMessageResponseDTO> response = messageService.sendMessage(conversationId, request);

            assertThat(response.getData().getId()).isEqualTo(200L);
            verify(messageRepository, never()).save(org.mockito.ArgumentMatchers.any(Message.class));
        }

        @Test
        @DisplayName("IMAGE phải là URL http/https hợp lệ")
        void sendMessage_RejectInvalidImageUrl() {
            Long conversationId = 10L;
            Conversation conversation = Conversation.builder()
                    .id(conversationId)
                    .status(ConversationStatus.ACTIVE)
                    .build();
            ConversationParticipant p1 = ConversationParticipant.builder().conversation(conversation).user(currentUser).build();
            SendMessageRequestDTO request = new SendMessageRequestDTO();
            request.setType(MessageType.IMAGE);
            request.setContent("not-a-url");

            given(currentUserProvider.findCurrentUserOrThrow()).willReturn(currentUser);
            given(conversationRepository.findById(conversationId)).willReturn(Optional.of(conversation));
            given(conversationParticipantRepository.findByConversation_IdAndUser_IdAndLeftAtIsNull(conversationId, currentUser.getId()))
                    .willReturn(Optional.of(p1));

            assertThatThrownBy(() -> messageService.sendMessage(conversationId, request))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATED);
        }
    }

    @Nested
    @DisplayName("markAsRead")
    class MarkAsRead {
        @Test
        @DisplayName("Đánh dấu đã đọc thành công")
        void markAsRead_Success() {
            Long conversationId = 10L;
            Long messageId = 99L;
            Conversation conversation = Conversation.builder()
                    .id(conversationId)
                    .status(ConversationStatus.ACTIVE)
                    .build();
            Message message = Message.builder()
                    .id(messageId)
                    .conversation(conversation)
                    .sender(currentUser)
                    .content("hello")
                    .type(MessageType.TEXT)
                    .build();
            ConversationParticipant participant = ConversationParticipant.builder()
                    .conversation(conversation)
                    .user(currentUser)
                    .build();
            ReadReceiptRequestDTO request = new ReadReceiptRequestDTO();
            request.setLastReadMessageId(messageId);

            given(currentUserProvider.findCurrentUserOrThrow()).willReturn(currentUser);
            given(conversationParticipantRepository.findByConversation_IdAndUser_IdAndLeftAtIsNull(conversationId, currentUser.getId()))
                    .willReturn(Optional.of(participant));
            given(messageRepository.findById(messageId)).willReturn(Optional.of(message));
            given(messageDeliveryStatusRepo.countUnreadMessages(currentUser.getId(), conversationId)).willReturn(0L);

            ApiResponse<ReadReceiptResponseDTO> response = messageService.markAsRead(conversationId, request);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData().getConversationId()).isEqualTo(conversationId);
            assertThat(response.getData().getLastReadMessageId()).isEqualTo(messageId);
            assertThat(response.getData().getUnreadCount()).isZero();
            assertThat(participant.getLastReadMessage()).isEqualTo(message);
            verify(conversationParticipantRepository).save(participant);
            verify(messageDeliveryStatusRepo).updateStatusToSeenForUserAndConversation(
                    org.mockito.ArgumentMatchers.eq(currentUser.getId()),
                    org.mockito.ArgumentMatchers.eq(conversationId),
                    org.mockito.ArgumentMatchers.eq(messageId),
                    org.mockito.ArgumentMatchers.any(Instant.class)
            );
        }

        @Test
        @DisplayName("Tin nhắn không thuộc conversation -> BUSINESS_RULE_VIOLATED")
        void markAsRead_MessageNotInConversation() {
            Long conversationId = 10L;
            ReadReceiptRequestDTO request = new ReadReceiptRequestDTO();
            request.setLastReadMessageId(99L);

            Conversation otherConversation = Conversation.builder().id(20L).build();
            Message otherMessage = Message.builder()
                    .id(99L)
                    .conversation(otherConversation)
                    .sender(currentUser)
                    .content("other")
                    .type(MessageType.TEXT)
                    .build();

            given(currentUserProvider.findCurrentUserOrThrow()).willReturn(currentUser);
            given(conversationParticipantRepository.findByConversation_IdAndUser_IdAndLeftAtIsNull(conversationId, currentUser.getId()))
                    .willReturn(Optional.of(ConversationParticipant.builder().conversation(Conversation.builder().id(conversationId).build()).user(currentUser).build()));
            given(messageRepository.findById(99L)).willReturn(Optional.of(otherMessage));

            assertThatThrownBy(() -> messageService.markAsRead(conversationId, request))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.BUSINESS_RULE_VIOLATED);
        }

        @Test
        @DisplayName("Không phải active participant -> FORBIDDEN")
        void markAsRead_ForbiddenWhenNotParticipant() {
            Long conversationId = 10L;
            ReadReceiptRequestDTO request = new ReadReceiptRequestDTO();
            request.setLastReadMessageId(99L);

            given(currentUserProvider.findCurrentUserOrThrow()).willReturn(currentUser);
            given(conversationParticipantRepository.findByConversation_IdAndUser_IdAndLeftAtIsNull(conversationId, currentUser.getId()))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> messageService.markAsRead(conversationId, request))
                    .isInstanceOf(AppException.class)
                    .extracting(e -> ((AppException) e).getErrorCode())
                    .isEqualTo(ErrorCode.FORBIDDEN);

            verify(messageRepository, never()).findById(any());
        }
    }
}
