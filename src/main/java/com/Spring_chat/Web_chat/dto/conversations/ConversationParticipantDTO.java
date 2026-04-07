package com.Spring_chat.Web_chat.dto.conversations;

import lombok.Data;

import java.time.Instant;

@Data
public class ConversationParticipantDTO {
    private Long userId;
    private String username;
    private String avatarUrl;
    private Instant joinedAt;
}
