package com.Spring_chat.Web_chat.dto.conversations;

import lombok.Data;

@Data
public class OtherParticipantDTO {
    private Long userId;
    private String username;
    private String avatarUrl;
    private boolean isOnline;
}
