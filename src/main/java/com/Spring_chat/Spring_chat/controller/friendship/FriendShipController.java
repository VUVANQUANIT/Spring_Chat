package com.Spring_chat.Spring_chat.controller.friendship;

import com.Spring_chat.Spring_chat.dto.ApiResponse;
import com.Spring_chat.Spring_chat.dto.friendship.FriendRequestCreateRequestDTO;
import com.Spring_chat.Spring_chat.dto.friendship.FriendRequestResponseDTO;
import com.Spring_chat.Spring_chat.dto.friendship.FriendResponseDTO;
import com.Spring_chat.Spring_chat.service.friendship.FriendShipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/friendships")
public class FriendShipController {
    private final FriendShipService friendShipService;
    @PostMapping("/request")
    public ResponseEntity<ApiResponse<FriendRequestResponseDTO>> sendRequestFriendShip(@Valid @RequestBody FriendRequestCreateRequestDTO requestCreateRequestDTO){
        return ResponseEntity.ok(friendShipService.sendRequestFriend(requestCreateRequestDTO));
    }
    @GetMapping("/requests")
    public ResponseEntity<ApiResponse<List<FriendResponseDTO>>> getRequestFriendShip(){
        return ResponseEntity.ok(friendShipService.getFriendRequests());
    }

    @GetMapping("/requests/sent")
    public ResponseEntity<ApiResponse<List<FriendResponseDTO>>> getSentRequestFriendShip(){
        return ResponseEntity.ok(friendShipService.getSentFriendRequests());
    }
}
