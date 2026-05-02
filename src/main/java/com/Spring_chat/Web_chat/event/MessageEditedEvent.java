package com.Spring_chat.Web_chat.event;

import java.time.Instant;

/**
 * Published after a message is successfully edited and the transaction commits.
 * Listeners can relay to WebSocket topics without racing ahead of DB visibility.
 */
public record MessageEditedEvent(
        long messageId,
        long conversationId,
        String content,
        boolean isEdited,
        Instant editedAt,
        long editedByUserId,
        String editedByUsername
) {}
