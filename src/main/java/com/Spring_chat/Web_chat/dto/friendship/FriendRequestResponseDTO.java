package com.Spring_chat.Web_chat.dto.friendship;

import com.Spring_chat.Web_chat.ENUM.FriendshipStatus;
import lombok.Data;

import java.time.Instant;

@Data
public class FriendRequestResponseDTO {
    private Long id;
    private Long addresseeId;
    private String addresseeUsername;
    private String addresseeAvatarUrl;
    private FriendshipStatus status;
    private Instant createdAt;
}
