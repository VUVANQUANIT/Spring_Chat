package com.Spring_chat.Spring_chat.controller.user;

import com.Spring_chat.Spring_chat.dto.ApiResponse;
import com.Spring_chat.Spring_chat.dto.user.MyProfileUserDTO;
import com.Spring_chat.Spring_chat.dto.user.ProfileUserDTO;
import com.Spring_chat.Spring_chat.dto.user.UpdateMyProfileRequestDTO;
import com.Spring_chat.Spring_chat.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class ProfileController {
    private final UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProfileUserDTO>> getUserProfile(@PathVariable long id) {
        return ResponseEntity.ok(userService.getUserProfile(id));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MyProfileUserDTO>> getMyProfile() {
        return ResponseEntity.ok(userService.getMyProfile());
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<MyProfileUserDTO>> updateMyProfile(
            @Valid @RequestBody UpdateMyProfileRequestDTO request
    ) {
        return ResponseEntity.ok(userService.updateMyProfile(request));
    }
}
