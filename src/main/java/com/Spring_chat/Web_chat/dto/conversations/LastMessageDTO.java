package com.Spring_chat.Web_chat.dto.conversations;

import com.Spring_chat.Web_chat.enums.MessageType;
import lombok.Data;

import java.time.Instant;

@Data
public class LastMessageDTO {
    private Long id;
    private String content;
    private MessageType type;
    private Long senderId;
    private String senderUsername;
    private Instant createdAt;
    private boolean isDeleted;
}
