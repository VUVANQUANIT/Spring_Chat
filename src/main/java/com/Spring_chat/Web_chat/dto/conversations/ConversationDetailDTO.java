package com.Spring_chat.Web_chat.dto.conversations;

import com.Spring_chat.Web_chat.enums.ConversationType;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class ConversationDetailDTO {
    private Long id;
    private ConversationType type;
    private String title;
    private String avatarUrl;
    private Long ownerId;
    private Instant createdAt;
    private List<ConversationParticipantDTO> participants;
}
