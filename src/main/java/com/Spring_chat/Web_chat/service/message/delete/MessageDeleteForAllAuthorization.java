package com.Spring_chat.Web_chat.service.message.delete;

import com.Spring_chat.Web_chat.entity.Message;
import com.Spring_chat.Web_chat.entity.User;

/**
 * Who may perform a global (ALL) delete — replace with staff rules, audit hooks, etc.
 */
public interface MessageDeleteForAllAuthorization {

    void assertMayDeleteForAll(Message message, User actor);
}
