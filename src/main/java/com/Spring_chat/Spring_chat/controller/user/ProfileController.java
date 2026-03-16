package com.Spring_chat.Spring_chat.controller.user;

import com.Spring_chat.Spring_chat.dto.ApiResponse;
import com.Spring_chat.Spring_chat.dto.user.MyProfileUserDTO;
import com.Spring_chat.Spring_chat.dto.user.ProfileUserDTO;
import com.Spring_chat.Spring_chat.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("api/users")
public class ProfileController {
    private final UserService userService;
    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<ProfileUserDTO>> getUserProfile(@PathVariable long id) {
        return  ResponseEntity.ok(userService.getUserProfile(id));
    }
    @GetMapping("/user/me")
    public ResponseEntity<ApiResponse<MyProfileUserDTO>> getMyProfile() {
        return  ResponseEntity.ok(userService.getMyProfile());
    }
}
