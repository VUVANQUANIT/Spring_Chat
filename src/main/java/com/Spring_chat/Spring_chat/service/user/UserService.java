package com.Spring_chat.Spring_chat.service.user;

import com.Spring_chat.Spring_chat.dto.ApiResponse;
import com.Spring_chat.Spring_chat.dto.PageResponse;
import com.Spring_chat.Spring_chat.dto.user.MyProfileUserDTO;
import com.Spring_chat.Spring_chat.dto.user.ProfileUserDTO;
import com.Spring_chat.Spring_chat.dto.user.UpdateMyProfileRequestDTO;
import com.Spring_chat.Spring_chat.dto.user.UserSearchResponseDTO;

public interface UserService {
    ApiResponse<ProfileUserDTO>     getUserProfile(Long id);
    ApiResponse<MyProfileUserDTO>   getMyProfile();
    ApiResponse<MyProfileUserDTO>   updateMyProfile(UpdateMyProfileRequestDTO request);
    ApiResponse<PageResponse<UserSearchResponseDTO>> searchUsers(String q, int page, int size);
}
