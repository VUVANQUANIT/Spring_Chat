package com.Spring_chat.Web_chat.controller.friendship;

import com.Spring_chat.Web_chat.enums.FriendDirection;
import com.Spring_chat.Web_chat.enums.FriendshipStatus;
import com.Spring_chat.Web_chat.dto.ApiResponse;
import com.Spring_chat.Web_chat.dto.PageResponse;
import com.Spring_chat.Web_chat.dto.friendship.*;
import com.Spring_chat.Web_chat.service.friendship.FriendShipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/friendships")
public class FriendShipController {
    private final FriendShipService friendShipService;

    @PostMapping("/requests")
    public ResponseEntity<ApiResponse<FriendRequestResponseDTO>> sendRequestFriendShip(
            @Valid @RequestBody FriendRequestCreateRequestDTO requestCreateRequestDTO) {
        return ResponseEntity.status(201).body(friendShipService.sendRequestFriend(requestCreateRequestDTO));
    }

    /**
     * GET /api/friendships/requests
     *
     * Query params (Spec 3.2):
     *   - status    : PENDING | ACCEPTED | REJECTED | BLOCKED  (optional, default = all)
     *   - direction : RECEIVED | SENT                          (optional, default = RECEIVED)
     *   - page      : 0-based page index                       (optional, default = 0)
     *   - size      : items per page, max 50                   (optional, default = 20)
     */
    @GetMapping("/requests")
    public ResponseEntity<ApiResponse<PageResponse<FriendResponseDTO>>> getFriendRequests(
            @RequestParam(required = false)                         FriendshipStatus status,
            @RequestParam(defaultValue = "RECEIVED")                FriendDirection  direction,
            @RequestParam(defaultValue = "0")                       int page,
            @RequestParam(defaultValue = "20")                      int size) {

        return ResponseEntity.ok(friendShipService.getFriendRequests(status, direction, page, size));
    }

    @PostMapping("/requests/{id}/accept")
    public ResponseEntity<ApiResponse<AcceptFriendResponseDTO>> acceptRequestFriendShip(@PathVariable("id") Long id) {
        return ResponseEntity.ok(friendShipService.acceptFriend(id));
    }

    @PostMapping("/requests/{id}/reject")
    public ResponseEntity<ApiResponse<RejectFriendResponseDTO>> rejectRequestFriendShip(@PathVariable("id") Long id) {
        return ResponseEntity.ok(friendShipService.rejectFriendShip(id));
    }

    @PostMapping("/{userId}/block")
    public ResponseEntity<ApiResponse<FriendResponseBlockDTO>> blockFriendship(@PathVariable("userId") Long id) {
        return ResponseEntity.ok(friendShipService.blockFriendship(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFriendship(@PathVariable("id") Long id) {
        friendShipService.deleteFriendship(id);
        return ResponseEntity.noContent().build();
    }
}
