package com.Spring_chat.Web_chat.dto.conversations;

import com.Spring_chat.Web_chat.enums.ConversationType;
import lombok.Data;

@Data
public class CreateConversationsDTO {
    private ConversationType type;
    private Long[] participantIds;
    private String title;
    private String avatarUrl;

}
