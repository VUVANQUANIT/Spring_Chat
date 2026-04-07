package com.Spring_chat.Web_chat.dto.friendship;

import com.Spring_chat.Web_chat.enums.FriendshipStatus;

import lombok.Data;

import java.time.Instant;

@Data
public class AcceptFriendResponseDTO {
    private Long id;
    private FriendshipStatus status;
    private Instant updatedAt;
}
