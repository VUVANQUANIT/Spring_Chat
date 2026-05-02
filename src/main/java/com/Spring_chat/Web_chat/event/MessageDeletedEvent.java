package com.Spring_chat.Web_chat.event;

import java.time.Instant;

/**
 * Published after a global (scope=ALL) soft-delete commits. scope=ME does not emit this event.
 */
public record MessageDeletedEvent(
        long messageId,
        long conversationId,
        Instant deletedAt,
        long deletedByUserId
) {}
