package com.Spring_chat.Web_chat.service.conversation;

import com.Spring_chat.Web_chat.dto.ApiResponse;
import com.Spring_chat.Web_chat.dto.conversations.*;
import com.Spring_chat.Web_chat.entity.Conversation;
import com.Spring_chat.Web_chat.entity.ConversationParticipant;
import com.Spring_chat.Web_chat.entity.Friendship;
import com.Spring_chat.Web_chat.entity.User;
import com.Spring_chat.Web_chat.enums.ConversationType;
import com.Spring_chat.Web_chat.enums.FriendshipStatus;
import com.Spring_chat.Web_chat.enums.MessageType;
import com.Spring_chat.Web_chat.exception.AppException;
import com.Spring_chat.Web_chat.exception.ErrorCode;
import com.Spring_chat.Web_chat.mappers.ConversationMapper;
import com.Spring_chat.Web_chat.repository.*;
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
    private final FriendshipRepository friendshipRepository;

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

    @Override
    @Transactional
    public ApiResponse<UpdateConversationDTO> updateConversation(Long id, UpdateConversationDTO updateConversationDTO) {
        User currentUser = currentUserProvider.findCurrentUserOrThrow();
        Conversation conversation = conversationRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Conversation not found"));

        if (conversation.getType() == ConversationType.PRIVATE) {
            throw new AppException(ErrorCode.BUSINESS_RULE_VIOLATED, "Cuộc hội thoại PRIVATE không có tiêu đề để sửa");
        }

        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> role.getName().equals(com.Spring_chat.Web_chat.enums.RoleName.ROLE_ADMIN));

        boolean isOwner = conversation.getOwner() != null && conversation.getOwner().getId().equals(currentUser.getId());

        if (!isOwner && !isAdmin) {
            throw new AppException(ErrorCode.FORBIDDEN, "Chỉ chủ nhóm hoặc Admin mới được phép chỉnh sửa");
        }

        if (updateConversationDTO.getTitle() != null && !updateConversationDTO.getTitle().trim().isEmpty()) {
            conversation.setTitle(updateConversationDTO.getTitle().trim());
        }
        if (updateConversationDTO.getAvatarUrl() != null) {
            conversation.setAvatarUrl(updateConversationDTO.getAvatarUrl().trim());
        }

        conversationRepository.save(conversation);

        UpdateConversationDTO response = UpdateConversationDTO.builder()
                .id(conversation.getId())
                .title(conversation.getTitle())
                .avatarUrl(conversation.getAvatarUrl())
                .build();

        return ApiResponse.ok("Conversation updated", response);
    }

    @Override
    @Transactional
    public ApiResponse<ListUserDTO> addUserToConversation(Long conversationId, ListUserDTO listUserDTO) {
        User currentUser = currentUserProvider.findCurrentUserOrThrow();
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy cuộc hội thoại"));

        if (conversation.getType() == ConversationType.PRIVATE) {
            throw new AppException(ErrorCode.BUSINESS_RULE_VIOLATED, "Không thể thêm thành viên vào cuộc hội thoại PRIVATE");
        }

        boolean isOwner = conversation.getOwner() != null && conversation.getOwner().getId().equals(currentUser.getId());
        if (!isOwner) {
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không phải là chủ của group nên không thể thêm");
        }

        if (listUserDTO.getUserIds() == null || listUserDTO.getUserIds().length == 0) {
            throw new AppException(ErrorCode.MISSING_PARAMETER, "Không có dữ liệu của người thêm vào");
        }

        for (Long userId : listUserDTO.getUserIds()) {
            addParticipantToConversation(conversation, userId, currentUser);
        }

        return ApiResponse.ok("Thêm thành công người dùng vào cuộc hội thoại", listUserDTO);
    }

    @Override
    @Transactional
    public ApiResponse<Void> removeParticipantFromConversation(Long conversationId, Long userId) {
        User currentUser = currentUserProvider.findCurrentUserOrThrow();
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy cuộc hội thoại"));

        if (conversation.getType() == ConversationType.PRIVATE) {
            throw new AppException(ErrorCode.BUSINESS_RULE_VIOLATED, "Không thể xóa thành viên khỏi cuộc hội thoại PRIVATE");
        }

        ConversationParticipant targetParticipant = conversationParticipantRepository
                .findByConversation_IdAndUser_Id(conversationId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Người dùng không phải thành viên của nhóm"));

        if (targetParticipant.getLeftAt() != null) {
             return ApiResponse.ok("User already left", null); // Idempotent: already left
        }

        boolean isSelf = currentUser.getId().equals(userId);
        boolean isOwner = conversation.getOwner() != null && conversation.getOwner().getId().equals(currentUser.getId());

        if (!isSelf && !isOwner) {
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền kick thành viên này");
        }

        // Thực hiện rời nhóm/kick
        targetParticipant.setLeftAt(Instant.now());
        conversationParticipantRepository.save(targetParticipant);

        // Nếu chủ nhóm rời đi -> chuyển quyền owner
        if (isSelf && isOwner) {
            transferOwnership(conversation, userId);
        }

        return ApiResponse.ok("Participant removed", null);
    }

    private void transferOwnership(Conversation conversation, Long leavingOwnerId) {
        // Tìm người còn lại gia nhập sớm nhất, loại trừ người đang rời đi
        Optional<ConversationParticipant> nextOwnerParticipant = conversationParticipantRepository
                .findFirstByConversation_IdAndUser_IdNotAndLeftAtIsNullOrderByJoinedAtAsc(conversation.getId(), leavingOwnerId);

        if (nextOwnerParticipant.isPresent()) {
            conversation.setOwner(nextOwnerParticipant.get().getUser());
            conversationRepository.save(conversation);
        } else {
            // Không còn ai trong nhóm -> Xóa owner
            conversation.setOwner(null);
            conversationRepository.save(conversation);
        }
    }

    private void addParticipantToConversation(Conversation conversation, Long participantId, User currentUser) {
        if (participantId.equals(currentUser.getId())) {
            return; // Chủ nhóm đã ở trong nhóm rồi
        }

        User participant = userRepository.findById(participantId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy người dùng ID: " + participantId));

        // Kiểm tra block hai chiều (chuẩn production: dùng findBetweenUsers)
        Optional<Friendship> friendship = friendshipRepository.findBetweenUsers(currentUser.getId(), participantId);
        if (friendship.isPresent() && friendship.get().getStatus() == FriendshipStatus.BLOCKED) {
            throw new AppException(ErrorCode.CANNOT_INVATE_BLOCK, "Không thể mời do tồn tại quan hệ block giữa hai người");
        }

        ConversationParticipant conversationParticipant = conversationParticipantRepository
                .findByConversation_IdAndUser(conversation.getId(), participant);

        if (conversationParticipant == null) {
            ConversationParticipant newParticipant = ConversationParticipant.builder()
                    .conversation(conversation)
                    .user(participant)
                    .joinedAt(Instant.now())
                    .build();
            conversationParticipantRepository.save(newParticipant);
        } else if (conversationParticipant.getLeftAt() != null) {
            // Re-join if they left before
            conversationParticipant.setLeftAt(null);
            conversationParticipantRepository.save(conversationParticipant);
        }
        // If already in group (joinedAt != null && leftAt == null), do nothing (idempotent)
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
