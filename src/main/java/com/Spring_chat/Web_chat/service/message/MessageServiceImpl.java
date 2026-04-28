package com.Spring_chat.Web_chat.service.message;

import com.Spring_chat.Web_chat.dto.ApiResponse;
import com.Spring_chat.Web_chat.dto.message.DeliveryStatusesDTO;
import com.Spring_chat.Web_chat.dto.message.MessageListResponseDTO;
import com.Spring_chat.Web_chat.dto.message.MessageRowProjection;
import com.Spring_chat.Web_chat.dto.message.MessageSummaryDTO;
import com.Spring_chat.Web_chat.dto.message.ReadReceiptRequestDTO;
import com.Spring_chat.Web_chat.dto.message.ReadReceiptResponseDTO;
import com.Spring_chat.Web_chat.dto.message.SendMessageRequestDTO;
import com.Spring_chat.Web_chat.dto.message.SendMessageResponseDTO;
import com.Spring_chat.Web_chat.dto.message.SenderDTO;
import com.Spring_chat.Web_chat.entity.Conversation;
import com.Spring_chat.Web_chat.entity.ConversationParticipant;
import com.Spring_chat.Web_chat.entity.Message;
import com.Spring_chat.Web_chat.entity.MessageStatus;
import com.Spring_chat.Web_chat.entity.User;
import com.Spring_chat.Web_chat.enums.ConversationStatus;
import com.Spring_chat.Web_chat.enums.MessageDeliveryStatus;
import com.Spring_chat.Web_chat.enums.MessageType;
import com.Spring_chat.Web_chat.exception.AppException;
import com.Spring_chat.Web_chat.exception.ErrorCode;
import com.Spring_chat.Web_chat.repository.ConversationParticipantRepository;
import com.Spring_chat.Web_chat.repository.ConversationRepository;
import com.Spring_chat.Web_chat.repository.MessageDeliveryStatusRepo;
import com.Spring_chat.Web_chat.repository.MessageRepository;
import com.Spring_chat.Web_chat.service.common.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import java.time.Duration;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class MessageServiceImpl implements MessageService {
    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final MessageDeliveryStatusRepo messageDeliveryStatusRepo;
    private final CurrentUserProvider currentUserProvider;
    private final StringRedisTemplate stringRedisTemplate;

    private static final Duration IDEMPOTENCY_TTL = Duration.ofSeconds(30);
    private static final String IDEMPOTENCY_PREFIX = "chat:idempotency:send-message:";

    // Simple cache to avoid redundant existsBy... queries (optimization)
    // Key: userId:conversationId, Value: Boolean indicating participant exists
    private final Cache<String, Boolean> participantCache = Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .maximumSize(10000)
            .build();

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<MessageListResponseDTO> getMessageList(Long beforeId, Integer limit, Long conversationId) {
        User currentUser = currentUserProvider.findCurrentUserOrThrow();
        Long userId = currentUser.getId();

        log.info("User {} requesting messages for conversation {}, beforeId={}, limit={}", userId, conversationId, beforeId, limit);

        validateParticipant(conversationId, userId);

        int queryLimit = normalizeLimit(limit);

        Instant beforeCreatedAt = null;
        if (beforeId != null) {
            beforeCreatedAt = messageRepository.findCreatedAtById(beforeId);
            if (beforeCreatedAt == null) {
                log.warn("beforeId {} not found for conversation {}", beforeId, conversationId);
            }
        }

        // Fetch queryLimit + 1 to know if there's a next page using PageRequest
        List<MessageRowProjection> rows = messageRepository.findMessagesByConversation(
                conversationId, userId, beforeCreatedAt, beforeId, PageRequest.of(0, queryLimit + 1));

        boolean hasMore = rows.size() > queryLimit;
        if (hasMore) {
            rows = new ArrayList<>(rows);
            rows.remove(rows.size() - 1);
        }

        List<Long> messageIds = rows.stream().map(MessageRowProjection::getId).toList();
        
        // Fetch all delivery statuses for these messages
        List<MessageStatus> allStatuses = messageDeliveryStatusRepo.findAllByMessage_IdIn(messageIds);
        
        // Group by message ID
        Map<Long, List<DeliveryStatusesDTO>> statusMap = allStatuses.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getMessage().getId(),
                        Collectors.mapping(s -> {
                            DeliveryStatusesDTO d = new DeliveryStatusesDTO();
                            d.setUserId(s.getUser().getId());
                            d.setStatus(s.getStatus());
                            d.setUpdatedAt(s.getUpdatedAt());
                            return d;
                        }, Collectors.toList())
                ));

        log.debug("Found {} messages for conversation {}, hasMore={}", rows.size(), conversationId, hasMore);

        List<MessageSummaryDTO> items = rows.stream()
                .map(row -> toMessageSummary(row, statusMap.getOrDefault(row.getId(), Collections.emptyList())))
                .toList();

        MessageListResponseDTO response = new MessageListResponseDTO();
        response.setItems(items);
        response.setHasMore(hasMore);

        return ApiResponse.ok("OK", response);
    }

    @Override
    @Transactional
    public ApiResponse<SendMessageResponseDTO> sendMessage(Long conversationId, SendMessageRequestDTO request) {
        User currentUser = currentUserProvider.findCurrentUserOrThrow();
        Long senderId = currentUser.getId();

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Conversation không tồn tại"));
        if (conversation.getStatus() != ConversationStatus.ACTIVE) {
            throw new AppException(ErrorCode.BUSINESS_RULE_VIOLATED, "Cuộc hội thoại không còn hoạt động");
        }

        conversationParticipantRepository
                .findByConversation_IdAndUser_IdAndLeftAtIsNull(conversationId, senderId)
                .orElseThrow(() -> new AppException(
                        ErrorCode.FORBIDDEN,
                        "Bạn không phải participant hợp lệ hoặc đã rời khỏi cuộc hội thoại"
                ));

        MessageType messageType = request.getType() == null ? MessageType.TEXT : request.getType();
        String normalizedContent = normalizeContent(request.getContent());
        validateMessageContent(messageType, normalizedContent);

        String normalizedClientMessageId = normalizeClientMessageId(request.getClientMessageId());
        String idempotencyKey = buildIdempotencyKey(conversationId, senderId, normalizedClientMessageId);
        boolean lockAcquired = true;
        if (idempotencyKey != null) {
            lockAcquired = tryAcquireIdempotencyKey(idempotencyKey);
            if (!lockAcquired) {
                Message existingMessage = findRecentDuplicate(conversationId, senderId, normalizedClientMessageId);
                if (existingMessage != null) {
                    return ApiResponse.created("Message sent", toSendMessageResponse(existingMessage));
                }
                throw new AppException(
                        ErrorCode.BUSINESS_RULE_VIOLATED,
                        "Yêu cầu gửi tin nhắn đang được xử lý, vui lòng thử lại"
                );
            }
        }

        try {
            Message replyTo = null;
            if (request.getReplyToId() != null) {
                replyTo = messageRepository.findByIdAndConversation_Id(request.getReplyToId(), conversationId)
                        .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Tin nhắn reply không tồn tại"));
            }

            Message message = Message.builder()
                    .conversation(conversation)
                    .sender(currentUser)
                    .content(normalizedContent)
                    .type(messageType)
                    .replyTo(replyTo)
                    .clientMessageId(normalizedClientMessageId)
                    .build();
            Message savedMessage = messageRepository.save(message);

            List<ConversationParticipant> activeParticipants =
                    conversationParticipantRepository.findAllByConversation_IdAndLeftAtIsNull(conversationId);

            List<MessageStatus> initialStatuses = activeParticipants.stream()
                    .map(participant -> MessageStatus.builder()
                            .message(savedMessage)
                            .user(participant.getUser())
                            .status(MessageDeliveryStatus.SENT)
                            .build())
                    .toList();
            messageDeliveryStatusRepo.saveAll(initialStatuses);

            participantCache.put(senderId + ":" + conversationId, true);
            persistIdempotencyResult(idempotencyKey, savedMessage.getId());
            return ApiResponse.created("Message sent", toSendMessageResponse(savedMessage));
        } catch (RuntimeException ex) {
            releaseIdempotencyKey(idempotencyKey);
            throw ex;
        }
    }

    @Override
    @Transactional
    public ApiResponse<ReadReceiptResponseDTO> markAsRead(Long conversationId, ReadReceiptRequestDTO request) {
        User currentUser = currentUserProvider.findCurrentUserOrThrow();
        Long userId = currentUser.getId();

        ConversationParticipant participant = conversationParticipantRepository
                .findByConversation_IdAndUser_IdAndLeftAtIsNull(conversationId, userId)
                .orElseThrow(() -> new AppException(
                        ErrorCode.FORBIDDEN,
                        "Bạn không có quyền hoặc đã rời nhóm"
                ));

        Message lastReadMessage = messageRepository.findById(request.getLastReadMessageId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy tin nhắn"));

        if (!lastReadMessage.getConversation().getId().equals(conversationId)) {
            throw new AppException(ErrorCode.BUSINESS_RULE_VIOLATED, "Tin nhắn không thuộc cuộc hội thoại này");
        }

        participant.setLastReadMessage(lastReadMessage);
        conversationParticipantRepository.save(participant);

        messageDeliveryStatusRepo.updateStatusToSeenForUserAndConversation(
                userId,
                conversationId,
                lastReadMessage.getId(),
                Instant.now()
        );

        int unreadCount = Math.toIntExact(messageDeliveryStatusRepo.countUnreadMessages(userId, conversationId));
        ReadReceiptResponseDTO response = ReadReceiptResponseDTO.builder()
                .conversationId(conversationId)
                .lastReadMessageId(lastReadMessage.getId())
                .unreadCount(unreadCount)
                .build();

        participantCache.put(userId + ":" + conversationId, true);
        return ApiResponse.ok("Read receipt updated", response);
    }

    @Override
    public void invalidateParticipantCache(Long conversationId, Long userId) {
        if (conversationId == null || userId == null) {
            return;
        }
        participantCache.invalidate(userId + ":" + conversationId);
    }


    private void validateParticipant(Long conversationId, Long userId) {
        String cacheKey = userId + ":" + conversationId;
        Boolean isParticipant = participantCache.getIfPresent(cacheKey);
        
        if (isParticipant != null && isParticipant) {
            return; // Cache hit and valid
        }

        isParticipant = conversationParticipantRepository
                .existsByConversation_IdAndUser_IdAndLeftAtIsNull(conversationId, userId);
        if (!isParticipant) {
            log.warn("User {} attempted to read conversation {} without being a participant", userId, conversationId);
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không phải là thành viên của cuộc hội thoại này");
        }
        
        participantCache.put(cacheKey, true);
    }

    private int normalizeLimit(Integer requestedLimit) {
        int effectiveLimit = (requestedLimit != null && requestedLimit > 0) ? requestedLimit : 30;
        return Math.min(effectiveLimit, 100);
    }

    private String normalizeClientMessageId(String clientMessageId) {
        if (clientMessageId == null) {
            return null;
        }
        String trimmed = clientMessageId.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed;
    }

    private String buildIdempotencyKey(Long conversationId, Long senderId, String clientMessageId) {
        if (clientMessageId == null) {
            return null;
        }
        return IDEMPOTENCY_PREFIX + conversationId + ":" + senderId + ":" + clientMessageId;
    }

    private boolean tryAcquireIdempotencyKey(String idempotencyKey) {
        ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();
        Boolean acquired = valueOps.setIfAbsent(idempotencyKey, "PENDING", IDEMPOTENCY_TTL);
        return Boolean.TRUE.equals(acquired);
    }

    private void persistIdempotencyResult(String idempotencyKey, Long messageId) {
        if (idempotencyKey == null || messageId == null) {
            return;
        }
        stringRedisTemplate.opsForValue().set(idempotencyKey, String.valueOf(messageId), IDEMPOTENCY_TTL);
    }

    private void releaseIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null) {
            return;
        }
        stringRedisTemplate.delete(idempotencyKey);
    }

    private Message findRecentDuplicate(Long conversationId, Long senderId, String clientMessageId) {
        return messageRepository
                .findFirstByConversation_IdAndSender_IdAndClientMessageIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                        conversationId,
                        senderId,
                        clientMessageId,
                        Instant.now().minusSeconds(IDEMPOTENCY_TTL.toSeconds())
                )
                .orElse(null);
    }

    private String normalizeContent(String content) {
        return content == null ? null : content.trim();
    }

    private void validateMessageContent(MessageType type, String content) {
        if (type != MessageType.TEXT && type != MessageType.IMAGE) {
            throw new AppException(ErrorCode.BUSINESS_RULE_VIOLATED, "Chỉ hỗ trợ gửi TEXT hoặc IMAGE");
        }
        if (type == MessageType.TEXT) {
            if (content == null || content.isBlank()) {
                throw new AppException(ErrorCode.BUSINESS_RULE_VIOLATED, "Nội dung text không được để trống");
            }
            return;
        }
        if (!isValidHttpUrl(content)) {
            throw new AppException(ErrorCode.BUSINESS_RULE_VIOLATED, "Tin nhắn IMAGE phải có URL hợp lệ");
        }
    }

    private boolean isValidHttpUrl(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && uri.getHost() != null;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private SendMessageResponseDTO toSendMessageResponse(Message message) {
        SendMessageResponseDTO dto = new SendMessageResponseDTO();
        dto.setId(message.getId());
        dto.setConversationId(message.getConversation().getId());

        SenderDTO senderDTO = new SenderDTO();
        senderDTO.setId(message.getSender().getId());
        senderDTO.setUsername(message.getSender().getUsername());
        senderDTO.setAvatarUrl(message.getSender().getAvatarUrl());
        dto.setSender(senderDTO);

        dto.setContent(Boolean.TRUE.equals(message.getIsDeleted()) ? null : message.getContent());
        dto.setType(message.getType());
        dto.setReplyTo(message.getReplyTo() == null ? null : message.getReplyTo().getId());
        dto.setDeleted(Boolean.TRUE.equals(message.getIsDeleted()));
        dto.setEdited(Boolean.TRUE.equals(message.getIsEdited()));
        dto.setCreatedAt(message.getCreatedAt());
        dto.setClientMessageId(message.getClientMessageId());
        return dto;
    }

    private MessageSummaryDTO toMessageSummary(MessageRowProjection row, List<DeliveryStatusesDTO> deliveryStatuses) {
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
        dto.setDeliveryStatuses(deliveryStatuses);
        
        return dto;
    }

}
