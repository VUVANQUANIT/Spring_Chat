package com.Spring_chat.Web_chat.controller.user;

import com.Spring_chat.Web_chat.dto.ApiResponse;
import com.Spring_chat.Web_chat.dto.PageResponse;
import com.Spring_chat.Web_chat.dto.user.MyProfileUserDTO;
import com.Spring_chat.Web_chat.dto.user.ProfileUserDTO;
import com.Spring_chat.Web_chat.dto.user.UpdateMyProfileRequestDTO;
import com.Spring_chat.Web_chat.dto.user.UserSearchResponseDTO;
import com.Spring_chat.Web_chat.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class ProfileController {

    private final UserService userService;

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

    /**
     * Tìm kiếm user theo username hoặc fullName.
     *
     * GET /api/users/search?q=alice&page=0&size=20
     *
     * Endpoint này phải đặt TRÊN /{id} để tránh Spring match "search" như một path variable.
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<UserSearchResponseDTO>>> searchUsers(
            @RequestParam String q,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(userService.searchUsers(q, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProfileUserDTO>> getUserProfile(@PathVariable long id) {
        return ResponseEntity.ok(userService.getUserProfile(id));
    }
}
