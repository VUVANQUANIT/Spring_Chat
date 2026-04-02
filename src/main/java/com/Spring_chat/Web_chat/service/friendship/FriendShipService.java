package com.Spring_chat.Web_chat.service.friendship;

import com.Spring_chat.Web_chat.dto.ApiResponse;
import com.Spring_chat.Web_chat.dto.friendship.*;

import java.util.List;

public interface FriendShipService {
    ApiResponse<FriendRequestResponseDTO> sendRequestFriend(FriendRequestCreateRequestDTO friendRequestCreateRequestDTO);
    ApiResponse<List<FriendResponseDTO>> getFriendRequests();
    ApiResponse<List<FriendResponseDTO>> getSentFriendRequests();
    ApiResponse<AcceptFriendResponseDTO> acceptFriend(Long id);
    ApiResponse<RejectFriendResponseDTO> rejectFriendShip(Long id);
    ApiResponse<FriendResponseBlockDTO> blockFriendship(Long id);
    ApiResponse<String>deleteFriendship(Long id);
}
