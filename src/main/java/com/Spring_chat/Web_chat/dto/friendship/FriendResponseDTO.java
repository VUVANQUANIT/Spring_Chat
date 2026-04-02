package com.Spring_chat.Web_chat.dto.friendship;

import com.Spring_chat.Web_chat.ENUM.FriendshipStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FriendResponseDTO {
    private Long friendRequestId;
    private Long requesterId;
    private String requesterUsername;
    private String requestAvatarUrl;
    private Long addresseeId;
    private String addresseeUsername;
    private String addressAvatarUrl;
    private FriendshipStatus status;
    private Instant createdAt;
    private Instant updatedAt;
}
