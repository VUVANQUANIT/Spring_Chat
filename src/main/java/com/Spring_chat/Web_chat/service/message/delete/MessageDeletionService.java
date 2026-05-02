package com.Spring_chat.Web_chat.service.message.delete;

import com.Spring_chat.Web_chat.enums.MessageDeleteScope;

public interface MessageDeletionService {

    void deleteMessage(long messageId, MessageDeleteScope scope);
}
