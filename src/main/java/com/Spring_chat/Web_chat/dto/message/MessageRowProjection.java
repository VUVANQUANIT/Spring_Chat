package com.Spring_chat.Web_chat.dto.message;

import com.Spring_chat.Web_chat.enums.MessageDeliveryStatus;
import com.Spring_chat.Web_chat.enums.MessageType;
import java.time.Instant;

public interface MessageRowProjection {
    Long getId();
    Long getConversationId();
    String getContent();
    MessageType getType();
    Long getReplyToId();
    Boolean getIsDeleted();
    Boolean getIsEdited();
    Instant getEditedAt();
    Instant getCreatedAt();
    
    // Sender Info
    Long getSenderId();
    String getSenderUsername();
    String getSenderAvatar();
    
    // My Status
    MessageDeliveryStatus getMyStatus();
}
