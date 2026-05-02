package com.Spring_chat.Web_chat.dto.message;

import com.Spring_chat.Web_chat.enums.MessageType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SendMessageRequestDTO {
    @Size(max = 4000, message = "Content must be at most 4000 characters")
    private String content;

    private MessageType type;

    private Long replyToId;

    @JsonProperty("clientMessageId")
    @Size(max = 100, message = "clientMessageId must be at most 100 characters")
    private String clientMessageId;
}
