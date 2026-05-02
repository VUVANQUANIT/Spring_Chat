package com.Spring_chat.Web_chat.service.message.edit;

import com.Spring_chat.Web_chat.entity.Message;
import com.Spring_chat.Web_chat.entity.User;

/**
 * Pluggable edit rules (OCP: swap or compose implementations for moderator overrides, etc.).
 */
public interface MessageEditValidator {

    void assertEditable(Message message, User editor, String normalizedNewContent);
}
