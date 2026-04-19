package com.Spring_chat.Web_chat.service.friendship;

import com.Spring_chat.Web_chat.enums.FriendshipStatus;
import com.Spring_chat.Web_chat.dto.ApiResponse;
import com.Spring_chat.Web_chat.dto.friendship.RejectFriendResponseDTO;
import com.Spring_chat.Web_chat.entity.Friendship;
import com.Spring_chat.Web_chat.entity.User;
import com.Spring_chat.Web_chat.exception.AppException;
import com.Spring_chat.Web_chat.exception.ErrorCode;
import com.Spring_chat.Web_chat.repository.FriendshipRepository;
import com.Spring_chat.Web_chat.service.common.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendShipServiceImplTest {

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private FriendShipServiceImpl friendShipService;

    private User currentUser;
    private User requester;
    private Friendship friendship;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(1L);

        requester = new User();
        requester.setId(2L);

        friendship = new Friendship();
        friendship.setId(100L);
        friendship.setRequester(requester);
        friendship.setAddressee(currentUser);
        friendship.setStatus(FriendshipStatus.PENDING);
    }

    @Test
    void rejectFriendShip_Success() {
        // Given
        when(currentUserProvider.findCurrentUserOrThrow()).thenReturn(currentUser);
        when(friendshipRepository.findById(100L)).thenReturn(Optional.of(friendship));

        // When
        ApiResponse<RejectFriendResponseDTO> response = friendShipService.rejectFriendShip(100L);

        // Then
        assertEquals("Friend request rejected", response.getMessage());
        assertEquals(FriendshipStatus.REJECTED, friendship.getStatus());
        verify(friendshipRepository, times(1)).save(friendship);
    }

    @Test
    void rejectFriendShip_NotFound_ThrowsException() {
        // Given
        when(currentUserProvider.findCurrentUserOrThrow()).thenReturn(currentUser);
        when(friendshipRepository.findById(100L)).thenReturn(Optional.empty());

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> friendShipService.rejectFriendShip(100L));
        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void rejectFriendShip_NotAddressee_ThrowsException() {
        // Given
        User stranger = new User();
        stranger.setId(3L);
        friendship.setAddressee(stranger); // Current user is NOT the addressee

        when(currentUserProvider.findCurrentUserOrThrow()).thenReturn(currentUser);
        when(friendshipRepository.findById(100L)).thenReturn(Optional.of(friendship));

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> friendShipService.rejectFriendShip(100L));
        assertEquals(ErrorCode.FORBIDDEN, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("Bạn không có quyền từ chối lời mời này"));
    }

    @Test
    void rejectFriendShip_NotPending_ThrowsException() {
        // Given
        friendship.setStatus(FriendshipStatus.ACCEPTED);

        when(currentUserProvider.findCurrentUserOrThrow()).thenReturn(currentUser);
        when(friendshipRepository.findById(100L)).thenReturn(Optional.of(friendship));

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> friendShipService.rejectFriendShip(100L));
        assertEquals(ErrorCode.BUSINESS_RULE_VIOLATED, exception.getErrorCode());
        assertTrue(exception.getMessage().contains("PENDING"));
    }
}
