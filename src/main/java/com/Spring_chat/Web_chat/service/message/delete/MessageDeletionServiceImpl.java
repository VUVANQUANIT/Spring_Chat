package com.Spring_chat.Web_chat.service.message.delete;

import com.Spring_chat.Web_chat.entity.Conversation;
import com.Spring_chat.Web_chat.entity.Message;
import com.Spring_chat.Web_chat.entity.User;
import com.Spring_chat.Web_chat.enums.ConversationStatus;
import com.Spring_chat.Web_chat.enums.MessageDeleteScope;
import com.Spring_chat.Web_chat.event.MessageDeletedEvent;
import com.Spring_chat.Web_chat.exception.AppException;
import com.Spring_chat.Web_chat.exception.ErrorCode;
import com.Spring_chat.Web_chat.repository.ConversationParticipantRepository;
import com.Spring_chat.Web_chat.repository.MessageRepository;
import com.Spring_chat.Web_chat.service.common.CurrentUserProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MessageDeletionServiceImpl implements MessageDeletionService {

    private final MessageRepository messageRepository;
    private final ConversationParticipantRepository conversationParticipantRepository;
    private final CurrentUserProvider currentUserProvider;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final Map<MessageDeleteScope, MessageDeleteScopeExecutor> executorsByScope;

    public MessageDeletionServiceImpl(
            List<MessageDeleteScopeExecutor> executors,
            MessageRepository messageRepository,
            ConversationParticipantRepository conversationParticipantRepository,
            CurrentUserProvider currentUserProvider,
            ApplicationEventPublisher applicationEventPublisher) {
        this.messageRepository = messageRepository;
        this.conversationParticipantRepository = conversationParticipantRepository;
        this.currentUserProvider = currentUserProvider;
        this.applicationEventPublisher = applicationEventPublisher;
        this.executorsByScope = executors.stream()
                .collect(Collectors.toUnmodifiableMap(
                        MessageDeleteScopeExecutor::supportedScope,
                        Function.identity(),
                        (a, b) -> {
                            throw new IllegalStateException("Duplicate MessageDeleteScopeExecutor for scope "
                                    + a.supportedScope());
                        }));
    }

    @Override
    @Transactional
    public void deleteMessage(long messageId, MessageDeleteScope scope) {
        User actor = currentUserProvider.findCurrentUserOrThrow();
        Message message = messageRepository.findDetailedById(messageId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Tin nhắn không tồn tại"));

        Conversation conversation = message.getConversation();
        if (conversation.getStatus() != ConversationStatus.ACTIVE) {
            throw new AppException(ErrorCode.BUSINESS_RULE_VIOLATED, "Cuộc hội thoại không còn hoạt động");
        }

        Long conversationId = conversation.getId();
        conversationParticipantRepository
                .findByConversation_IdAndUser_IdAndLeftAtIsNull(conversationId, actor.getId())
                .orElseThrow(() -> new AppException(
                        ErrorCode.FORBIDDEN,
                        "Bạn không phải participant hợp lệ hoặc đã rời khỏi cuộc hội thoại"));

        MessageDeleteScopeExecutor executor = executorsByScope.get(scope);
        if (executor == null) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, "Không hỗ trợ scope xóa");
        }

        boolean wasGloballyDeleted = Boolean.TRUE.equals(message.getIsDeleted());
        executor.execute(message, actor);

        if (scope == MessageDeleteScope.ALL
                && !wasGloballyDeleted
                && Boolean.TRUE.equals(message.getIsDeleted())) {
            applicationEventPublisher.publishEvent(new MessageDeletedEvent(
                    message.getId(),
                    conversationId,
                    message.getDeletedAt(),
                    actor.getId()));
        }

        log.debug("deleteMessage messageId={} scope={} actorId={}", messageId, scope, actor.getId());
    }
}
