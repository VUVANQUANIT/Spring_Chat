package com.Spring_chat.Spring_chat.service.friendship;

import com.Spring_chat.Spring_chat.ENUM.FriendshipStatus;
import com.Spring_chat.Spring_chat.ENUM.UserStatus;
import com.Spring_chat.Spring_chat.dto.ApiResponse;
import com.Spring_chat.Spring_chat.dto.friendship.AcceptFriendResponseDTO;
import com.Spring_chat.Spring_chat.dto.friendship.FriendRequestCreateRequestDTO;
import com.Spring_chat.Spring_chat.dto.friendship.FriendRequestResponseDTO;
import com.Spring_chat.Spring_chat.dto.friendship.FriendResponseDTO;
import com.Spring_chat.Spring_chat.entity.Friendship;
import com.Spring_chat.Spring_chat.entity.User;
import com.Spring_chat.Spring_chat.exception.AppException;
import com.Spring_chat.Spring_chat.exception.ErrorCode;
import com.Spring_chat.Spring_chat.mappers.FriendShipMapper;
import com.Spring_chat.Spring_chat.repository.FriendshipRepository;
import com.Spring_chat.Spring_chat.repository.UserRepository;
import com.Spring_chat.Spring_chat.service.common.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendShipServiceImpl implements FriendShipService {
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final FriendShipMapper friendShipMapper;

    @Override
    @Transactional
    public ApiResponse<FriendRequestResponseDTO> sendRequestFriend(FriendRequestCreateRequestDTO friendRequestCreateRequestDTO) {
        User user = currentUserProvider.findCurrentUserOrThrow();
        User user1 = currentUserProvider.findUserOrThrow(friendRequestCreateRequestDTO.getAddresseeId());

        if (user.getId().equals(user1.getId())) {
            throw new AppException(ErrorCode.BUSINESS_RULE_VIOLATED, "Không thể gửi lời mời cho chính mình");
        }
        if (userRepository.existsByIdAndStatus(user1.getId(), UserStatus.BANNED)
                || userRepository.existsByIdAndStatus(user1.getId(), UserStatus.BLOCKED)) {
            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Người dùng đang bị khoá không thể nhận lời mời kết bạn");
        }

        boolean hasPending = friendshipRepository.existsByRequester_IdAndAddressee_IdAndStatus(
                user.getId(), user1.getId(), FriendshipStatus.PENDING
        ) || friendshipRepository.existsByRequester_IdAndAddressee_IdAndStatus(
                user1.getId(), user.getId(), FriendshipStatus.PENDING
        );
        boolean hasFriend = friendshipRepository.existsByRequester_IdAndAddressee_IdAndStatus(
                user.getId(), user1.getId(), FriendshipStatus.ACCEPTED
        ) || friendshipRepository.existsByRequester_IdAndAddressee_IdAndStatus(
                user1.getId(), user.getId(), FriendshipStatus.ACCEPTED
        );

        if (hasPending) {
            throw new AppException(ErrorCode.FRIENDSHIP_REQUEST_EXISTS, "Đã gửi lời mời kết bạn trước đó rồi");
        }
        if (hasFriend) {
            throw new AppException(ErrorCode.HAS_FRIEND, "Đã là bạn bè");
        }

        Friendship friendship = new Friendship();
        friendship.setRequester(user);
        friendship.setAddressee(user1);
        friendship.setStatus(FriendshipStatus.PENDING);
        friendshipRepository.save(friendship);

        return ApiResponse.ok("Friend request sent", friendShipMapper.toFriendRequestResponseDTO(friendship));
    }

    @Override
    public ApiResponse<List<FriendResponseDTO>> getFriendRequests() {
        User user = currentUserProvider.findCurrentUserOrThrow();
        List<FriendResponseDTO> responseDTOS = friendshipRepository.findAllRequestFriends(
                user.getId(),
                FriendshipStatus.PENDING
        );
        if (responseDTOS.isEmpty()) {
            return ApiResponse.ok("Không có lời mời kết bạn", responseDTOS);
        }
        return ApiResponse.ok("Danh sách lời mời kết bạn", responseDTOS);
    }

    @Override
    public ApiResponse<List<FriendResponseDTO>> getSentFriendRequests() {
        User user = currentUserProvider.findCurrentUserOrThrow();
        List<FriendResponseDTO> responseDTOS = friendshipRepository.findAllSentRequestFriends(
                user.getId(),
                FriendshipStatus.PENDING
        );
        if (responseDTOS.isEmpty()) {
            return ApiResponse.ok("Không có lời mời đã gửi", responseDTOS);
        }
        return ApiResponse.ok("Danh sách lời mời đã gửi", responseDTOS);
    }

    @Override
    @Transactional
    public ApiResponse<AcceptFriendResponseDTO> acceptFriend(Long id) {
        User user = currentUserProvider.findCurrentUserOrThrow();

        Friendship friendship = friendshipRepository.findById(id).orElseThrow(
                () -> new AppException(ErrorCode.RESOURCE_NOT_FOUND," Friendship ID không tồn tại")
        );
        log.info("Search ID Friendship: {}", friendship.getId());
        if (!friendship.getStatus().equals(FriendshipStatus.PENDING)) {
            throw new AppException(ErrorCode.BUSINESS_RULE_VIOLATED,"Trạng thái lời mời phải là Pending");
        }
        log.info("Status Friendship id {} : {}", friendship.getId(), friendship.getStatus());
        if(!friendship.getAddressee().getId().equals(user.getId())){
            throw new AppException(ErrorCode.BUSINESS_RULE_VIOLATED,"Người nhận phải là mới có thể chấp nhận lời mời");
        }

        friendship.setStatus(FriendshipStatus.ACCEPTED);
        friendship.setUpdatedAt(Instant.now());
        friendshipRepository.save(friendship);
        log.info("Status Friendship id {} : {}", friendship.getId(), friendship.getStatus());

        AcceptFriendResponseDTO responseDTO = new AcceptFriendResponseDTO();
        responseDTO.setId(friendship.getId());
        responseDTO.setStatus(FriendshipStatus.ACCEPTED);
        responseDTO.setUpdatedAt(friendship.getUpdatedAt());
        log.info("AcceptFriendResponseDTO {}", responseDTO);

        ApiResponse<AcceptFriendResponseDTO> response = new ApiResponse<>();
        return response.ok("Friend request accepted", responseDTO);
    }
}
