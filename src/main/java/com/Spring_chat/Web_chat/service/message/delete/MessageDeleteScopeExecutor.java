package com.Spring_chat.Web_chat.service.message.delete;

import com.Spring_chat.Web_chat.entity.Message;
import com.Spring_chat.Web_chat.entity.User;
import com.Spring_chat.Web_chat.enums.MessageDeleteScope;

/**
 * Strategy per delete scope (Open/Closed: add new scopes by registering another executor bean).
 */
public interface MessageDeleteScopeExecutor {

    MessageDeleteScope supportedScope();

    void execute(Message message, User actor);
}
