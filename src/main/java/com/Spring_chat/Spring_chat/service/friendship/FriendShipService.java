package com.Spring_chat.Spring_chat.service.friendship;

import com.Spring_chat.Spring_chat.dto.ApiResponse;
import com.Spring_chat.Spring_chat.dto.friendship.FriendRequestCreateRequestDTO;
import com.Spring_chat.Spring_chat.dto.friendship.FriendRequestResponseDTO;
import com.Spring_chat.Spring_chat.dto.friendship.FriendResponseDTO;

import java.util.List;

public interface FriendShipService {
    ApiResponse<FriendRequestResponseDTO> sendRequestFriend(FriendRequestCreateRequestDTO friendRequestCreateRequestDTO);
    ApiResponse<List<FriendResponseDTO>> getFriendRequests();
    ApiResponse<List<FriendResponseDTO>> getSentFriendRequests();
}
