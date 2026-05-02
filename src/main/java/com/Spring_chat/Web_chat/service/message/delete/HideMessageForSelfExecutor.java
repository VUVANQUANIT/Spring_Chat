package com.Spring_chat.Web_chat.service.message.delete;

import com.Spring_chat.Web_chat.entity.Message;
import com.Spring_chat.Web_chat.entity.MessageHidden;
import com.Spring_chat.Web_chat.entity.User;
import com.Spring_chat.Web_chat.enums.MessageDeleteScope;
import com.Spring_chat.Web_chat.repository.MessageHiddenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HideMessageForSelfExecutor implements MessageDeleteScopeExecutor {

    private final MessageHiddenRepository messageHiddenRepository;

    @Override
    public MessageDeleteScope supportedScope() {
        return MessageDeleteScope.ME;
    }

    @Override
    public void execute(Message message, User actor) {
        if (messageHiddenRepository.existsByMessageIdAndUserId(message.getId(), actor.getId())) {
            return;
        }
        messageHiddenRepository.save(MessageHidden.builder()
                .message(message)
                .user(actor)
                .build());
    }
}
