package com.Spring_chat.Web_chat.service.conversation;

import com.Spring_chat.Web_chat.dto.ApiResponse;
import com.Spring_chat.Web_chat.dto.conversations.*;
import com.Spring_chat.Web_chat.entity.Conversation;
import com.Spring_chat.Web_chat.entity.ConversationParticipant;
import com.Spring_chat.Web_chat.entity.User;
import com.Spring_chat.Web_chat.enums.ConversationType;
import com.Spring_chat.Web_chat.enums.MessageType;
import com.Spring_chat.Web_chat.exception.AppException;
import com.Spring_chat.Web_chat.exception.ErrorCode;
import com.Spring_chat.Web_chat.mappers.ConversationMapper;
import com.Spring_chat.Web_chat.repository.ConversationParticipantRepository;
import com.Spring_chat.Web_chat.repository.ConversationRepository;
import com.Spring_chat.Web_chat.repository.MessageRepository;
import com.Spring_chat.Web_chat.repository.UserRepository;
import com.Spring_chat.Web_chat.service.common.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationServiceImpl implements ConversationService {
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final ConversationMapper conversationMapper;
    private final MessageRepository messageRepository;

    @Override
    @Transactional
    public ApiResponse<CreateConversationsResponseDTO> createConversation(CreateConversationsDTO createConversationsDTO) {
        User currentUser = currentUserProvider.findCurrentUserOrThrow();
        if (createConversationsDTO == null || createConversationsDTO.getType() == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Loại hội thoại không hợp lệ");
        }
        if (createConversationsDTO.getParticipantIds() == null || createConversationsDTO.getParticipantIds().length == 0) {
            throw new AppException(ErrorCode.VALIDATION_FAILED, "Danh sách người tham gia là bắt buộc");
        }

        Set<Long> participantIds = new LinkedHashSet<>(Arrays.asList(createConversationsDTO.getParticipantIds()));
        participantIds.remove(currentUser.getId());

        if (createConversationsDTO.getType() == ConversationType.PRIVATE) {
            if (participantIds.size() != 1) {
                throw new AppException(ErrorCode.BUSINESS_RULE_VIOLATED, "Cuộc hội thoại PRIVATE chỉ được đúng 1 người tham gia");
            }
            Long otherUserId = participantIds.iterator().next();
            User otherUser = currentUserProvider.findUserOrThrow(otherUserId);

            Conversation existing = conversationRepository
                    .findPrivateBetween(ConversationType.PRIVATE, currentUser.getId(), otherUserId)
                    .orElse(null);
            if (existing != null) {
                List<ConversationParticipant> participants =
                        conversationParticipantRepository.findByConversation_Id(existing.getId());
                CreateConversationsResponseDTO responseDTO =
                        conversationMapper.toCreateConversationsResponseDTO(existing, participants);
                return ApiResponse.created("Conversation created", responseDTO);
            }

            Conversation conversation = Conversation.builder()
                    .type(ConversationType.PRIVATE)
                    .build();
            conversationRepository.save(conversation);

            List<ConversationParticipant> participants = new ArrayList<>();
            participants.add(ConversationParticipant.builder()
                    .conversation(conversation)
                    .user(currentUser)
                    .build());
            participants.add(ConversationParticipant.builder()
                    .conversation(conversation)
                    .user(otherUser)
                    .build());
            conversationParticipantRepository.saveAll(participants);

            CreateConversationsResponseDTO responseDTO =
                    conversationMapper.toCreateConversationsResponseDTO(conversation, participants);
            return ApiResponse.created("Conversation created", responseDTO);
        }

        if (createConversationsDTO.getType() == ConversationType.GROUP) {
            if (participantIds.size() < 2) {
                throw new AppException(ErrorCode.BUSINESS_RULE_VIOLATED, "Cuộc hội thoại GROUP phải có ít nhất 2 người tham gia");
            }
            String title = createConversationsDTO.getTitle();
            if (title == null || title.trim().isEmpty()) {
                throw new AppException(ErrorCode.BUSINESS_RULE_VIOLATED, "Cuộc hội thoại GROUP bắt buộc phải có tiêu đề");
            }
            String avatarUrl = createConversationsDTO.getAvatarUrl();

            List<User> users = userRepository.findAllById(participantIds);
            if (users.size() != participantIds.size()) {
                throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Có người dùng không tồn tại");
            }

            Conversation conversation = Conversation.builder()
                    .type(ConversationType.GROUP)
                    .title(title.trim())
                    .avatarUrl(avatarUrl == null ? null : avatarUrl.trim())
                    .owner(currentUser)
                    .build();
            conversationRepository.save(conversation);

            List<ConversationParticipant> participants = new ArrayList<>();
            participants.add(ConversationParticipant.builder()
                    .conversation(conversation)
                    .user(currentUser)
                    .build());
            for (User user : users) {
                participants.add(ConversationParticipant.builder()
                        .conversation(conversation)
                        .user(user)
                        .build());
            }
            conversationParticipantRepository.saveAll(participants);

            CreateConversationsResponseDTO responseDTO =
                    conversationMapper.toCreateConversationsResponseDTO(conversation, participants);
            return ApiResponse.created("Conversation created", responseDTO);
        }

        throw new AppException(ErrorCode.VALIDATION_FAILED, "Loại hội thoại không hợp lệ");
    }

    @Override
    public ApiResponse<ConversationListDTO> getUserConversation(Pageable pageable, String cursor) {
        User currentUser = currentUserProvider.findCurrentUserOrThrow();

        int limit = Math.min(pageable.getPageSize(), 50);

        List<ConversationRowProjection> rows = conversationParticipantRepository
                .findUserConversations(currentUser.getId(), cursor, limit + 1);

        boolean hasMore = rows.size() > limit;
        List<ConversationRowProjection> page = hasMore ? rows.subList(0, limit) : rows;

        List<ConversationSummaryDTO> items = page.stream()
                .map(this::toSummaryDTO)
                .collect(Collectors.toList());

        Instant nextCursor = null;
        if (hasMore && !page.isEmpty()) {
            ConversationRowProjection last = page.get(page.size() - 1);
            nextCursor = last.getLastMessageCreatedAt() != null
                    ? last.getLastMessageCreatedAt()
                    : last.getConversationCreatedAt();
        }

        ConversationListDTO result = new ConversationListDTO();
        result.setItems(items);
        result.setNextCursor(nextCursor);
        result.setHasMore(hasMore);

        return ApiResponse.ok("OK", result);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<ConversationDetailDTO> getConversationDetail(Long conversationId) {
        User currentUser = currentUserProvider.findCurrentUserOrThrow();
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Conversation not found"));

        if (!conversationParticipantRepository.existsByConversation_IdAndUser_Id(conversationId, currentUser.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN, "You are not a participant of this conversation");
        }

        List<ConversationParticipant> participants =
                conversationParticipantRepository.findAllByConversation_IdOrderByJoinedAtAsc(conversationId);

        ConversationDetailDTO detailDTO = conversationMapper.toConversationDetailDTO(conversation, participants);
        return ApiResponse.ok("OK", detailDTO);
    }

    private ConversationSummaryDTO toSummaryDTO(ConversationRowProjection row) {
        ConversationSummaryDTO dto = new ConversationSummaryDTO();
        dto.setId(row.getId());
        dto.setType(ConversationType.valueOf(row.getType()));
        dto.setTitle(row.getTitle());
        dto.setAvatarUrl(row.getAvatarUrl());
        dto.setCreatedAt(row.getConversationCreatedAt());

        if (row.getLastMessageId() != null) {
            LastMessageDTO lastMsg = new LastMessageDTO();
            lastMsg.setId(row.getLastMessageId());
            lastMsg.setContent(row.getLastMessageContent());
            lastMsg.setType(row.getLastMessageType() != null
                    ? MessageType.valueOf(row.getLastMessageType()) : null);
            lastMsg.setSenderId(row.getLastMessageSenderId());
            lastMsg.setSenderUsername(row.getSenderUsername());
            lastMsg.setCreatedAt(row.getLastMessageCreatedAt());
            lastMsg.setDeleted(Boolean.TRUE.equals(row.getLastMessageIsDeleted()));
            dto.setLastMessage(lastMsg);
        }

        dto.setUnreadCount(row.getUnreadCount() != null ? row.getUnreadCount().intValue() : 0);

        if (row.getOtherUserId() != null) {
            OtherParticipantDTO other = new OtherParticipantDTO();
            other.setUserId(row.getOtherUserId());
            other.setUsername(row.getOtherUsername());
            other.setAvatarUrl(row.getOtherAvatarUrl());
            other.setOnline(Boolean.TRUE.equals(row.getIsOnline()));
            dto.setOtherParticipant(other);
        }

        return dto;
    }
}
