package com.Spring_chat.Spring_chat.service.user;

import com.Spring_chat.Spring_chat.dto.ApiResponse;
import com.Spring_chat.Spring_chat.dto.user.MyProfileUserDTO;
import com.Spring_chat.Spring_chat.dto.user.ProfileUserDTO;

public interface UserService  {
    ApiResponse<ProfileUserDTO> getUserProfile(Long id);
    ApiResponse<MyProfileUserDTO> getMyProfile();
}
