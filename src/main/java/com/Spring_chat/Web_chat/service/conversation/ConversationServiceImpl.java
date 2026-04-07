package com.Spring_chat.Web_chat.service.conversation;

import com.Spring_chat.Web_chat.dto.ApiResponse;
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
import com.Spring_chat.Web_chat.repository.UserRepository;
import com.Spring_chat.Web_chat.service.common.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationServiceImpl implements ConversationService {
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final ConversationMapper conversationMapper;

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
}
