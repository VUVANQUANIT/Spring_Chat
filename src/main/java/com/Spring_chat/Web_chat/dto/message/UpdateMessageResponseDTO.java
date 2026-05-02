package com.Spring_chat.Web_chat.dto.message;

import com.Spring_chat.Web_chat.enums.MessageType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.Instant;

@Data
public class UpdateMessageResponseDTO {
    private Long id;
    private Long conversationId;
    private SenderDTO sender;
    private String content;
    private MessageType type;
    private Long replyTo;

    @JsonProperty("isDeleted")
    private boolean deleted;

    @JsonProperty("isEdited")
    private boolean edited;

    private Instant editedAt;
    private EditedByDTO editedBy;
    private Instant createdAt;
    private String clientMessageId;
}
