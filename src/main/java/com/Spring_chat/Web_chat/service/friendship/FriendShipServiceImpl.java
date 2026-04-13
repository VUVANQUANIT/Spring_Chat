package com.Spring_chat.Web_chat.service.friendship;

import com.Spring_chat.Web_chat.enums.FriendDirection;
import com.Spring_chat.Web_chat.enums.FriendshipStatus;
import com.Spring_chat.Web_chat.enums.UserStatus;
import com.Spring_chat.Web_chat.dto.ApiResponse;
import com.Spring_chat.Web_chat.dto.PageResponse;
import com.Spring_chat.Web_chat.dto.friendship.*;
import com.Spring_chat.Web_chat.entity.Friendship;
import com.Spring_chat.Web_chat.entity.User;
import com.Spring_chat.Web_chat.exception.AppException;
import com.Spring_chat.Web_chat.exception.ErrorCode;
import com.Spring_chat.Web_chat.mappers.FriendShipMapper;
import com.Spring_chat.Web_chat.repository.FriendshipRepository;
import com.Spring_chat.Web_chat.repository.UserRepository;
import com.Spring_chat.Web_chat.service.common.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

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
    public ApiResponse<PageResponse<FriendResponseDTO>> getFriendRequests(
            FriendshipStatus status,
            FriendDirection direction,
            int page,
            int size) {

        User user = currentUserProvider.findCurrentUserOrThrow();

        // RECEIVED = mình là addressee; SENT = mình là requester
        boolean received = (direction == FriendDirection.RECEIVED);

        Page<FriendResponseDTO> resultPage = friendshipRepository.findRequests(
                user.getId(),
                received,
                status,                     // null → không lọc status
                PageRequest.of(page, size)
        );

        PageResponse<FriendResponseDTO> pageResponse = PageResponse.of(resultPage.getContent(), resultPage);
        return ApiResponse.ok("OK", pageResponse);
    }

    @Override
    @Transactional
    public ApiResponse<AcceptFriendResponseDTO> acceptFriend(Long id) {
        User user = currentUserProvider.findCurrentUserOrThrow();
        Friendship friendship = requirePendingAddressee(
                id,
                user,
                "Người nhận phải là mới có thể chấp nhận lời mời"
        );
        updateStatus(friendship, FriendshipStatus.ACCEPTED);

        AcceptFriendResponseDTO responseDTO = new AcceptFriendResponseDTO();
        responseDTO.setId(friendship.getId());
        responseDTO.setStatus(FriendshipStatus.ACCEPTED);
        responseDTO.setUpdatedAt(friendship.getUpdatedAt());
        log.info("AcceptFriendResponseDTO {}", responseDTO);

        ApiResponse<AcceptFriendResponseDTO> response = new ApiResponse<>();
        return response.ok("Friend request accepted", responseDTO);
    }
    @Override
    @Transactional
    public ApiResponse<RejectFriendResponseDTO> rejectFriendShip(Long id){
        User user = currentUserProvider.findCurrentUserOrThrow();
        Friendship friendship = requirePendingAddressee(
                id,
                user,
                "Người nhận phải là mới có thể từ chối lời mời"
        );
        updateStatus(friendship, FriendshipStatus.REJECTED);
        RejectFriendResponseDTO responseDTO = new RejectFriendResponseDTO();
        responseDTO.setId(friendship.getId());
        responseDTO.setUpdatedAt(friendship.getUpdatedAt());
        log.info("RejectFriendResponseDTO {}", responseDTO);
        ApiResponse<RejectFriendResponseDTO> response = new ApiResponse<>();
        return response.ok("Friend request rejected", responseDTO);
    }

    @Override
    @Transactional
    public ApiResponse<FriendResponseBlockDTO> blockFriendship(Long targetUserId) {
        User blocker = currentUserProvider.findCurrentUserOrThrow();
        User target  = currentUserProvider.findUserOrThrow(targetUserId);

        if (blocker.getId().equals(target.getId())) {
            throw new AppException(ErrorCode.CANNOT_BLOCK_SELF, "Không thể tự block chính mình");
        }


        Friendship friendship = friendshipRepository
                .findBetweenUsers(blocker.getId(), target.getId())
                .map(existing -> {
                    existing.setRequester(blocker);
                    existing.setAddressee(target);
                    existing.setStatus(FriendshipStatus.BLOCKED);
                    return existing;
                })
                .orElseGet(() -> Friendship.builder()
                        .requester(blocker)
                        .addressee(target)
                        .status(FriendshipStatus.BLOCKED)
                        .build()
                );

        friendshipRepository.save(friendship);

        FriendResponseBlockDTO dto = new FriendResponseBlockDTO();
        dto.setBlockedUserId(target.getId());
        dto.setFriendshipStatus(FriendshipStatus.BLOCKED);
        return ApiResponse.ok("User blocked", dto);
    }

    @Override
    @Transactional
    public ApiResponse<String> deleteFriendship(Long id) {
        User currentUser = currentUserProvider.findCurrentUserOrThrow();
        User target = currentUserProvider.findUserOrThrow(id);
        if (target.getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.CANNOT_DELETE_SELF,"Không thể tự xoá chính mình");
        }
        Friendship friendship = friendshipRepository
                .findBetweenUsers(currentUser.getId(),target.getId()).orElseThrow(
                        () -> new   AppException(ErrorCode.RESOURCE_NOT_FOUND,"Bạn chưa phải bạn bẻ")
                );


        friendshipRepository.delete(friendship);
        return ApiResponse.ok("Friendship deleted", null);
    }


    private Friendship requirePendingAddressee(Long id, User user, String notAddresseeMessage) {
        Friendship friendship = friendshipRepository.findById(id).orElseThrow(
                () -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, " Friendship ID không tồn tại")
        );
        log.info("Search ID Friendship: {}", friendship.getId());
        if (!friendship.getStatus().equals(FriendshipStatus.PENDING)) {
            throw new AppException(ErrorCode.BUSINESS_RULE_VIOLATED, "Trạng thái lời mời phải là Pending");
        }
        log.info("Status Friendship id {} : {}", friendship.getId(), friendship.getStatus());
        if (!friendship.getAddressee().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.BUSINESS_RULE_VIOLATED, notAddresseeMessage);
        }
        return friendship;
    }

    private void updateStatus(Friendship friendship, FriendshipStatus status) {
        friendship.setStatus(status);
        friendship.setUpdatedAt(Instant.now());
        friendshipRepository.save(friendship);
        log.info("Status Friendship id {} : {}", friendship.getId(), friendship.getStatus());
    }
}
