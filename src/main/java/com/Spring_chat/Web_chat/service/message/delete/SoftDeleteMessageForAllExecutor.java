package com.Spring_chat.Web_chat.service.message.delete;

import com.Spring_chat.Web_chat.entity.Message;
import com.Spring_chat.Web_chat.entity.User;
import com.Spring_chat.Web_chat.enums.MessageDeleteScope;
import com.Spring_chat.Web_chat.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class SoftDeleteMessageForAllExecutor implements MessageDeleteScopeExecutor {

    private final MessageRepository messageRepository;
    private final MessageDeleteForAllAuthorization deleteForAllAuthorization;
    private final Clock clock;

    @Override
    public MessageDeleteScope supportedScope() {
        return MessageDeleteScope.ALL;
    }

    @Override
    public void execute(Message message, User actor) {
        if (Boolean.TRUE.equals(message.getIsDeleted())) {
            return;
        }
        deleteForAllAuthorization.assertMayDeleteForAll(message, actor);
        Instant now = Instant.now(clock);
        message.setIsDeleted(true);
        message.setDeletedAt(now);
        message.setDeletedBy(actor);
        messageRepository.save(message);
    }
}
