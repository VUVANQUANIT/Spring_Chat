package com.Spring_chat.Web_chat.dto.conversations;

import java.time.Instant;

/**
 * Spring Data JPA projection cho native query lấy danh sách conversations của user.
 *
 * Các getter phải khớp (case-sensitive) với quoted alias trong SQL SELECT.
 * Xem ConversationParticipantRepository#findUserConversations.
 */
public interface ConversationRowProjection {
    Long    getId();
    String  getType();
    String  getTitle();
    String  getAvatarUrl();
    Instant getConversationCreatedAt();

    Long    getLastMessageId();
    String  getLastMessageContent();
    String  getLastMessageType();
    Long    getLastMessageSenderId();
    String  getSenderUsername();
    Instant getLastMessageCreatedAt();
    Boolean getLastMessageIsDeleted();

    Long    getUnreadCount();

    Long    getOtherUserId();
    String  getOtherUsername();
    String  getOtherAvatarUrl();
    Boolean getIsOnline();
}
