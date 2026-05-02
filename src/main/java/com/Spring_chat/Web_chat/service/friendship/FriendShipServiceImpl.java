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
import com.Spring_chat.Web_chat.dto.conversations.CreateConversationsDTO;
import com.Spring_chat.Web_chat.enums.ConversationType;
import com.Spring_chat.Web_chat.service.conversation.ConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendShipServiceImpl implements FriendShipService {
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final FriendShipMapper friendShipMapper;
    private final ConversationService conversationService;

    @Override
    @Transactional
    public ApiResponse<FriendRequestResponseDTO> sendRequestFriend(FriendRequestCreateRequestDTO dto) {
        User requester = currentUserProvider.findCurrentUserOrThrow();
        User addressee = currentUserProvider.findUserOrThrow(dto.getAddresseeId());

        if (requester.getId().equals(addressee.getId())) {
            throw new AppException(ErrorCode.BUSINESS_RULE_VIOLATED, "Không thể gửi lời mời cho chính mình");
        }

        // Spec 3.1: Nếu bị BLOCKED -> trả 404 (ẩn thông tin)
        friendshipRepository.findBetweenUsers(requester.getId(), addressee.getId())
                .ifPresent(f -> {
                    if (f.getStatus() == FriendshipStatus.BLOCKED) {
                        // Nếu addressee block requester -> trả 404
                        if (f.getRequester().getId().equals(addressee.getId())) {
                            throw new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Người dùng không tồn tại");
                        }
                        // Nếu mình đã block họ -> trả Forbidden hoặc Conflict tùy design, ở đây báo lỗi rõ ràng
                        throw new AppException(ErrorCode.BUSINESS_RULE_VIOLATED, "Bạn đã chặn người dùng này");
                    }
                    if (f.getStatus() == FriendshipStatus.ACCEPTED) {
                        throw new AppException(ErrorCode.HAS_FRIEND, "Đã là bạn bè");
                    }
                    if (f.getStatus() == FriendshipStatus.PENDING) {
                        throw new AppException(ErrorCode.FRIENDSHIP_REQUEST_EXISTS, "Đã có lời mời đang chờ xử lý");
                    }
                });

        Friendship friendship = Friendship.builder()
                .requester(requester)
                .addressee(addressee)
                .status(FriendshipStatus.PENDING)
                .build();
        
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
        boolean received = (direction == FriendDirection.RECEIVED);

        Page<FriendResponseDTO> resultPage = friendshipRepository.findRequests(
                user.getId(),
                received,
                status,
                PageRequest.of(page, size)
        );

        return ApiResponse.ok("OK", PageResponse.of(resultPage.getContent(), resultPage));
    }

    @Override
    @Transactional
    public ApiResponse<AcceptFriendResponseDTO> acceptFriend(Long id) {
        User currentUser = currentUserProvider.findCurrentUserOrThrow();
        Friendship friendship = requirePendingAddressee(id, currentUser, "Bạn không có quyền chấp nhận lời mời này");
        
        updateStatus(friendship, FriendshipStatus.ACCEPTED);

        // Spec 3.3: Sau khi accept, tự động tạo/lấy Conversation PRIVATE
        createPrivateConversation(friendship.getRequester(), friendship.getAddressee());

        AcceptFriendResponseDTO responseDTO = new AcceptFriendResponseDTO();
        responseDTO.setId(friendship.getId());
        responseDTO.setStatus(FriendshipStatus.ACCEPTED);
        responseDTO.setUpdatedAt(friendship.getUpdatedAt());

        return ApiResponse.ok("Friend request accepted", responseDTO);
    }

    private void createPrivateConversation(User u1, User u2) {
        try {
            CreateConversationsDTO createDTO = new CreateConversationsDTO();
            createDTO.setType(ConversationType.PRIVATE);
            createDTO.setParticipantIds(new Long[]{u1.getId()}); // ConversationService.createConversation adds current user
            conversationService.createConversation(createDTO);
        } catch (Exception e) {
            log.error("Failed to auto-create conversation between {} and {}: {}", u1.getId(), u2.getId(), e.getMessage());
        }
    }

    @Override
    @Transactional
    public ApiResponse<RejectFriendResponseDTO> rejectFriendShip(Long id) {
        User currentUser = currentUserProvider.findCurrentUserOrThrow();
        Friendship friendship = requirePendingAddressee(id, currentUser, "Bạn không có quyền từ chối lời mời này");
        
        updateStatus(friendship, FriendshipStatus.REJECTED);
        
        RejectFriendResponseDTO responseDTO = new RejectFriendResponseDTO();
        responseDTO.setId(friendship.getId());
        responseDTO.setUpdatedAt(friendship.getUpdatedAt());
        
        return ApiResponse.ok("Friend request rejected", responseDTO);
    }

    @Override
    @Transactional
    public ApiResponse<FriendResponseBlockDTO> blockFriendship(Long targetUserId) {
        User blocker = currentUserProvider.findCurrentUserOrThrow();
        User target  = currentUserProvider.findUserOrThrow(targetUserId);

        if (blocker.getId().equals(target.getId())) {
            throw new AppException(ErrorCode.CANNOT_BLOCK_SELF, "Không thể tự block chính mình");
        }

        Friendship friendship = friendshipRepository.findBetweenUsers(blocker.getId(), target.getId())
                .map(existing -> {
                    existing.setRequester(blocker); // Mark current user as the one who initiated block
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
    public ApiResponse<String> deleteFriendship(Long targetUserId) {
        User currentUser = currentUserProvider.findCurrentUserOrThrow();
        
        Friendship friendship = friendshipRepository.findBetweenUsers(currentUser.getId(), targetUserId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy quan hệ bạn bè"));

        if (friendship.getStatus() != FriendshipStatus.ACCEPTED) {
            throw new AppException(ErrorCode.BUSINESS_RULE_VIOLATED, "Hai người chưa phải là bạn bè");
        }

        friendshipRepository.delete(friendship);
        return ApiResponse.ok("Friendship deleted", null);
    }

    private Friendship requirePendingAddressee(Long id, User user, String errorMessage) {
        Friendship friendship = friendshipRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Lời mời kết bạn không tồn tại"));

        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw new AppException(ErrorCode.BUSINESS_RULE_VIOLATED, "Trạng thái lời mời không hợp lệ (phải là PENDING)");
        }

        if (!friendship.getAddressee().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN, errorMessage);
        }
        return friendship;
    }

    private void updateStatus(Friendship friendship, FriendshipStatus status) {
        friendship.setStatus(status);
        friendship.setUpdatedAt(Instant.now());
        friendshipRepository.save(friendship);
    }
}
