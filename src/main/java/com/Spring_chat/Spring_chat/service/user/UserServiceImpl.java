package com.Spring_chat.Spring_chat.service.user;

import com.Spring_chat.Spring_chat.dto.ApiResponse;
import com.Spring_chat.Spring_chat.dto.user.MyProfileUserDTO;
import com.Spring_chat.Spring_chat.dto.user.ProfileUserDTO;
import com.Spring_chat.Spring_chat.dto.user.UpdateMyProfileRequestDTO;
import com.Spring_chat.Spring_chat.entity.User;
import com.Spring_chat.Spring_chat.exception.AppException;
import com.Spring_chat.Spring_chat.exception.ErrorCode;
import com.Spring_chat.Spring_chat.mappers.UserMapper;
import com.Spring_chat.Spring_chat.repository.UserRepository;
import com.Spring_chat.Spring_chat.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    @Override
    @Transactional(readOnly = true)
    public ApiResponse<ProfileUserDTO> getUserProfile(Long id) {
        User user = findUserOrThrow(id);
        return ApiResponse.ok("OK", userMapper.userToUserDTO(user));
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<MyProfileUserDTO> getMyProfile() {
        User user = findCurrentUserOrThrow();
        return ApiResponse.ok("OK", userMapper.userToMyUserDTO(user));
    }

    @Override
    @Transactional
    public ApiResponse<MyProfileUserDTO> updateMyProfile(UpdateMyProfileRequestDTO request) {
        User user = findCurrentUserOrThrow();

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName().trim());
        }

        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl().trim());
        }

        User savedUser = userRepository.save(user);
        return ApiResponse.ok("Profile updated successfully", userMapper.userToMyUserDTO(savedUser));
    }

    private User findCurrentUserOrThrow() {
        AuthenticatedUser authenticatedUser =
                (AuthenticatedUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return findUserOrThrow(authenticatedUser.id());
    }

    private User findUserOrThrow(Long id) {
        if (id == null) {
            throw new AppException(ErrorCode.MISSING_PARAMETER, "Missing user id");
        }
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));
    }
}
