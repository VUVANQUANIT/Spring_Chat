package com.Spring_chat.Web_chat.dto.message;

import com.Spring_chat.Web_chat.enums.MessageType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;

@Data
public class SendMessageResponseDTO {
    private Long id;
    private Long conversationId;
    private SenderDTO sender;
    private String content;
    private MessageType type;
    private Long replyTo;

    @JsonProperty("isDeleted")
    private boolean isDeleted;

    @JsonProperty("isEdited")
    private boolean isEdited;

    private Instant createdAt;
    private String clientMessageId;
}
