package com.Spring_chat.Web_chat.dto.conversations;

import com.Spring_chat.Web_chat.enums.ConversationType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateConversationsDTO {

    @NotNull(message = "Conversation type is required")
    private ConversationType type;

    @NotEmpty(message = "At least one participant is required")
    private Long[] participantIds;

    @Size(max = 100, message = "Title must not exceed 100 characters")
    private String title;

    @Size(max = 500, message = "Avatar URL must not exceed 500 characters")
    private String avatarUrl;
}
