package com.Spring_chat.Web_chat.service.friendship;

import com.Spring_chat.Web_chat.ENUM.FriendDirection;
import com.Spring_chat.Web_chat.ENUM.FriendshipStatus;
import com.Spring_chat.Web_chat.dto.ApiResponse;
import com.Spring_chat.Web_chat.dto.PageResponse;
import com.Spring_chat.Web_chat.dto.friendship.*;

public interface FriendShipService {
    ApiResponse<FriendRequestResponseDTO> sendRequestFriend(FriendRequestCreateRequestDTO friendRequestCreateRequestDTO);

    /**
     * Lấy danh sách friend requests theo spec 3.2.
     *
     * @param status    null = tất cả trạng thái; PENDING/ACCEPTED/REJECTED/BLOCKED = lọc
     * @param direction RECEIVED (mình là addressee) hoặc SENT (mình là requester)
     * @param page      trang (0-based)
     * @param size      số bản ghi/trang
     */
    ApiResponse<PageResponse<FriendResponseDTO>> getFriendRequests(
            FriendshipStatus status,
            FriendDirection direction,
            int page,
            int size
    );

    ApiResponse<AcceptFriendResponseDTO> acceptFriend(Long id);
    ApiResponse<RejectFriendResponseDTO> rejectFriendShip(Long id);
    ApiResponse<FriendResponseBlockDTO> blockFriendship(Long id);
    ApiResponse<String> deleteFriendship(Long id);
}
