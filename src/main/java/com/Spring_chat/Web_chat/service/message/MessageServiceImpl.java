package com.Spring_chat.Web_chat.service.message;

import com.Spring_chat.Web_chat.dto.ApiResponse;
import com.Spring_chat.Web_chat.dto.message.MessageListResponseDTO;
import com.Spring_chat.Web_chat.dto.message.MessageRowProjection;
import com.Spring_chat.Web_chat.dto.message.MessageSummaryDTO;
import com.Spring_chat.Web_chat.dto.message.SenderDTO;
import com.Spring_chat.Web_chat.entity.User;
import com.Spring_chat.Web_chat.exception.AppException;
import com.Spring_chat.Web_chat.exception.ErrorCode;
import com.Spring_chat.Web_chat.repository.ConversationParticipantRepository;
import com.Spring_chat.Web_chat.repository.MessageRepository;
import com.Spring_chat.Web_chat.service.common.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class MessageServiceImpl implements MessageService {
    private final MessageRepository messageRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final CurrentUserProvider currentUserProvider;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<MessageListResponseDTO> getMessageList(Long beforeId, Integer limit, Long conversationId) {
        User currentUser = currentUserProvider.findCurrentUserOrThrow();
        Long userId = currentUser.getId();

        log.info("User {} requesting messages for conversation {}, beforeId={}, limit={}", userId, conversationId, beforeId, limit);

        validateParticipant(conversationId, userId);

        int queryLimit = normalizeLimit(limit);

        // Fetch queryLimit + 1 to know if there's a next page
        List<MessageRowProjection> rows = messageRepository.findMessagesByConversation(
                conversationId, userId, beforeId, queryLimit + 1);

        boolean hasMore = rows.size() > queryLimit;
        if (hasMore) {
            rows.remove(rows.size() - 1);
        }

        log.debug("Found {} messages for conversation {}, hasMore={}", rows.size(), conversationId, hasMore);

        List<MessageSummaryDTO> items = rows.stream()
                .map(this::toMessageSummary)
                .toList();

        MessageListResponseDTO response = new MessageListResponseDTO();
        response.setItems(items);
        response.setHasMore(hasMore);

        return ApiResponse.ok("OK", response);
    }

    private void validateParticipant(Long conversationId, Long userId) {
        boolean isParticipant = conversationParticipantRepository
                .existsByConversation_IdAndUser_Id(conversationId, userId);
        if (!isParticipant) {
            log.warn("User {} attempted to read conversation {} without being a participant", userId, conversationId);
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không phải là thành viên của cuộc hội thoại này");
        }
    }

    private int normalizeLimit(Integer requestedLimit) {
        int effectiveLimit = (requestedLimit != null && requestedLimit > 0) ? requestedLimit : 30;
        return Math.min(effectiveLimit, 100);
    }

    private MessageSummaryDTO toMessageSummary(MessageRowProjection row) {
        MessageSummaryDTO dto = new MessageSummaryDTO();
        dto.setId(row.getId());
        dto.setConversationId(row.getConversationId());

        SenderDTO sender = new SenderDTO();
        sender.setId(row.getSenderId());
        sender.setUsername(row.getSenderUsername());
        sender.setAvatarUrl(row.getSenderAvatar());
        dto.setSender(sender);

        // Hide content if the message is marked as deleted
        if (row.getIsDeleted() != null && row.getIsDeleted()) {
            dto.setContent(null);
            dto.setDeleted(true);
        } else {
            dto.setContent(row.getContent());
            dto.setDeleted(false);
        }

        dto.setType(row.getType());
        dto.setReplyTo(row.getReplyToId());
        dto.setEdited(row.getIsEdited() != null && row.getIsEdited());
        dto.setEditedAt(row.getEditedAt());
        dto.setCreatedAt(row.getCreatedAt());
        dto.setMyStatus(row.getMyStatus());
        
        return dto;
    }
}
