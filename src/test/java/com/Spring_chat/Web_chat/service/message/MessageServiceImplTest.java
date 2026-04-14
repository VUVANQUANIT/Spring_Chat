package com.Spring_chat.Web_chat.service.message;

import com.Spring_chat.Web_chat.dto.ApiResponse;
import com.Spring_chat.Web_chat.dto.message.MessageListResponseDTO;
import com.Spring_chat.Web_chat.dto.message.MessageRowProjection;
import com.Spring_chat.Web_chat.entity.User;
import com.Spring_chat.Web_chat.enums.MessageDeliveryStatus;
import com.Spring_chat.Web_chat.enums.MessageType;
import com.Spring_chat.Web_chat.exception.AppException;
import com.Spring_chat.Web_chat.exception.ErrorCode;
import com.Spring_chat.Web_chat.repository.ConversationParticipantRepository;
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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private ConversationParticipantRepository conversationParticipantRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private MessageServiceImpl messageService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = User.builder().id(1L).username("testuser").build();
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
            given(conversationParticipantRepository.existsByConversation_IdAndUser_Id(conversationId, currentUser.getId()))
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
                given(row.getMyStatus()).willReturn(MessageDeliveryStatus.SEEN);
                mockRows.add(row);
            }

            given(messageRepository.findMessagesByConversation(conversationId, currentUser.getId(), beforeId, requestedLimit + 1))
                    .willReturn(mockRows);

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

            given(currentUserProvider.findCurrentUserOrThrow()).willReturn(currentUser);
            given(conversationParticipantRepository.existsByConversation_IdAndUser_Id(conversationId, currentUser.getId()))
                    .willReturn(true);

            List<MessageRowProjection> mockRows = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                MessageRowProjection row = mock(MessageRowProjection.class);
                if (i < 2) {
                    given(row.getId()).willReturn((long) (49 - i));
                    given(row.getIsDeleted()).willReturn(false);
                }
                mockRows.add(row);
            }

            given(messageRepository.findMessagesByConversation(conversationId, currentUser.getId(), beforeId, requestedLimit + 1))
                    .willReturn(mockRows);

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
            given(conversationParticipantRepository.existsByConversation_IdAndUser_Id(conversationId, currentUser.getId()))
                    .willReturn(true);

            MessageRowProjection row = mock(MessageRowProjection.class);
            given(row.getId()).willReturn(1L);
            given(row.getIsDeleted()).willReturn(true); 

            List<MessageRowProjection> mockRows = new ArrayList<>();
            mockRows.add(row);

            given(messageRepository.findMessagesByConversation(anyLong(), anyLong(), org.mockito.ArgumentMatchers.isNull(), anyInt()))
                    .willReturn(mockRows);

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
            given(conversationParticipantRepository.existsByConversation_IdAndUser_Id(conversationId, currentUser.getId()))
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
            given(conversationParticipantRepository.existsByConversation_IdAndUser_Id(conversationId, currentUser.getId()))
                    .willReturn(true);

            // When
            messageService.getMessageList(beforeId, requestedLimit, conversationId);

            // Then
            // Verify that the repository is called with 100 + 1 instead of 150 + 1
            verify(messageRepository).findMessagesByConversation(conversationId, currentUser.getId(), beforeId, 101);
        }
    }
}
