package com.Spring_chat.Spring_chat.controller.friendship;

import com.Spring_chat.Spring_chat.dto.ApiResponse;
import com.Spring_chat.Spring_chat.dto.friendship.*;
import com.Spring_chat.Spring_chat.service.friendship.FriendShipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    @PostMapping("/requests/{id}/accept")
    public ResponseEntity<ApiResponse<AcceptFriendResponseDTO>> acceptRequestFriendShip(@PathVariable("id") Long id){
        return ResponseEntity.ok(friendShipService.acceptFriend(id));
    }
    @PostMapping("/requests/{id}/reject")
    public ResponseEntity<ApiResponse<RejectFriendResponseDTO>> rejectRequestFriendShip(@PathVariable("id") Long id){
        return ResponseEntity.ok(friendShipService.rejectFriendShip(id));
    }
    @PostMapping("/friendships/{userId}/block")
    public ResponseEntity<ApiResponse<FriendResponseBlockDTO>> blockFriendship(@PathVariable("userId") Long id){
        return ResponseEntity.ok(friendShipService.blockFriendship(id));
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteFriendship(@PathVariable("id") Long id){
        return ResponseEntity.ok(friendShipService.deleteFriendship(id));
    }
}
