package com.Spring_chat.Web_chat.dto.friendship;

import com.Spring_chat.Web_chat.enums.FriendshipStatus;
import lombok.Data;

@Data
public class FriendResponseBlockDTO {
    private Long blockedUserId;
    private FriendshipStatus friendshipStatus = FriendshipStatus.BLOCKED;
}
